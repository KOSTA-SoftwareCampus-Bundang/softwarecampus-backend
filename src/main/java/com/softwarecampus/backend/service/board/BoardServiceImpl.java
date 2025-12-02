package com.softwarecampus.backend.service.board;

import com.softwarecampus.backend.domain.board.*;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.board.*;
import com.softwarecampus.backend.exception.board.BoardErrorCode;
import com.softwarecampus.backend.exception.board.BoardException;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.repository.board.*;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final S3Service s3Service;
    private final FileType fileType;
    private final BoardRepository boardRepository;
    private final BoardAttachRepository boardAttachRepository;
    private final BoardViewRepository boardViewRepository;
    private final AccountRepository accountRepository;
    private final CommentRepository commentRepository;
    private final BoardRecommendRepository boardRecommendRepository;
    private final CommentRecommendRepository commentRecommendRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<BoardListResponseDTO> getBoards(int pageNo, BoardCategory category, String searchType,
            String searchText, String sortType) {
        int pageIndex = Math.max(pageNo - 1, 0);
        PageRequest pageRequest = PageRequest.of(pageIndex, 10);

        // 정렬 타입 기본값 설정
        String effectiveSortType = (sortType != null && !sortType.isEmpty()) ? sortType : "latest";

        // 검색어 없으면 정렬만 적용
        if (searchType == null || "".equals(searchType) || searchText == null || "".equals(searchText)) {
            switch (effectiveSortType) {
                case "popular":
                    return boardRepository.findBoardsByCategoryOrderByPopular(category, pageRequest);
                case "views":
                    return boardRepository.findBoardsByCategoryOrderByViews(category, pageRequest);
                case "comments":
                    return boardRepository.findBoardsByCategoryOrderByComments(category, pageRequest);
                case "latest":
                default:
                    return boardRepository.findBoardsByCategoryOrderByLatest(category, pageRequest);
            }
        } else {
            // 검색 + 정렬 (검색 시에는 최신순 고정 - 추후 확장 가능)
            String searchPattern = "%" + searchText + "%";
            switch (searchType) {
                case "title":
                    return boardRepository.findBoardsByTitle(category, searchPattern, pageRequest);
                case "content":
                    return boardRepository.findBoardsByText(category, searchPattern, pageRequest);
                case "title_content":
                    return boardRepository.findBoardsByTitleAndText(category, searchPattern, pageRequest);
                case "author":
                    return boardRepository.findBoardsByAuthor(category, searchPattern, pageRequest);
                case "comment":
                    return boardRepository.findBoardsByComment(category, searchPattern, pageRequest);
                case "all":
                    return boardRepository.findBoardsByAll(category, searchPattern, pageRequest);
                default:
                    throw new BoardException(BoardErrorCode.SEARCHTYPE_MISSMATCH);
            }
        }
    } //

    @Transactional
    @Override
    public BoardResponseDTO getBoardById(Long id, Long userId, String clientIp) {
        // Fetch Join으로 연관 엔티티 한번에 조회 (N+1 방지)
        Board board = boardRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        // 삭제된 글 조회 불가 처리
        if (!board.isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        // 비밀글 접근 권한 체크: 작성자 본인 또는 관리자만 접근 가능
        if (board.isSecret()) {
            if (userId == null) {
                throw new BoardException(BoardErrorCode.CANNOT_READ_BOARD);
            }
            Account viewer = accountRepository.findById(userId).orElse(null);
            boolean isOwner = userId.equals(board.getAccount().getId());
            boolean isAdmin = viewer != null && viewer.getAccountType() == AccountType.ADMIN;
            if (!isOwner && !isAdmin) {
                throw new BoardException(BoardErrorCode.CANNOT_READ_BOARD);
            }
        }

        // 조회수 증가 로직 (중복 방지)
        incrementHitsIfAllowed(board, userId, clientIp);

        BoardResponseDTO boardResponseDTO = BoardResponseDTO.from(board, userId);
        if (userId != null) {
            boardResponseDTO.setLike(
                    board.getBoardRecommends().stream().anyMatch(br -> br.getAccount().getId().equals(userId)));
            boardResponseDTO.setOwner(userId.equals(board.getAccount().getId()));
        }
        return boardResponseDTO;
    }

    /**
     * 조회수 증가 (중복 방지)
     * - 작성자 본인: 증가 안함
     * - 로그인 사용자: 오늘 첫 조회일 때만 +1 (account_id 기준)
     * - 비로그인 사용자: 오늘 첫 조회일 때만 +1 (IP 기준)
     */
    private void incrementHitsIfAllowed(Board board, Long userId, String clientIp) {
        // 1. 작성자 본인이면 증가 안함
        if (userId != null && userId.equals(board.getAccount().getId())) {
            return;
        }

        boolean alreadyViewed;

        if (userId != null) {
            // 2. 로그인 사용자: account_id 기준
            alreadyViewed = boardViewRepository.existsTodayViewByAccount(board.getId(), userId);
        } else {
            // 3. 비로그인 사용자: IP 기준
            alreadyViewed = boardViewRepository.existsTodayViewByIp(board.getId(), clientIp);
        }

        if (!alreadyViewed) {
            // 조회수 증가
            board.setHits(board.getHits() + 1);

            // 조회 기록 저장
            BoardView boardView = BoardView.builder()
                    .board(board)
                    .account(userId != null ? accountRepository.getReferenceById(userId) : null)
                    .ipAddress(userId == null ? clientIp : null)
                    .build();
            boardViewRepository.save(boardView);
        }
    }

    // 게시글 생성
    @Transactional
    @Override
    public Long createBoard(BoardCreateRequestDTO boardCreateRequestDTO, MultipartFile[] files, Long userId) {

        Board board = boardCreateRequestDTO.toEntity();

        // 파일 개수 검증 (새로 업로드할 파일 + 이미 업로드된 파일)
        int totalFileCount = (files != null ? files.length : 0) +
                (boardCreateRequestDTO.getUploadedFileUrls() != null
                        ? boardCreateRequestDTO.getUploadedFileUrls().size()
                        : 0);

        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.BOARD_ATTACH);
        if (totalFileCount > 0) {
            if (!config.isFileCountValid(totalFileCount)) {
                throw new BoardException(BoardErrorCode.FILE_COUNT_EXCEEDED);
            }
        }

        List<BoardAttach> boardAttachList = board.getBoardAttaches();

        // 1. 새로 업로드할 파일 처리
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                BoardAttach boardAttach = uploadFile(file);
                boardAttachList.add(boardAttach);
                boardAttach.setBoard(board);
            }
        }

        // 2. 이미 S3에 업로드된 파일 처리 (에디터에서 업로드한 이미지)
        if (boardCreateRequestDTO.getUploadedFileUrls() != null
                && !boardCreateRequestDTO.getUploadedFileUrls().isEmpty()) {
            for (String uploadedUrl : boardCreateRequestDTO.getUploadedFileUrls()) {
                // URL에서 파일명 추출
                String fileName = extractFileNameFromUrl(uploadedUrl);
                
                // S3에서 파일 크기 조회 (실패 시 0 반환)
                long fileSize = s3Service.getFileSize(uploadedUrl);

                BoardAttach boardAttach = BoardAttach.builder()
                        .originalFilename(fileName)
                        .realFilename(uploadedUrl)
                        .fileSize(fileSize)
                        .board(board)
                        .build();

                boardAttachList.add(boardAttach);
            }
        }
        // 로그인한 사용자 조회
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다. ID: " + userId));
        // 조회된 사용자 boardEntity에 세팅 후 save로 저장
        board.setAccount(account);
        boardRepository.save(board);

        return board.getId();
    }

    /**
     * S3 URL에서 파일명 추출
     * 예: https://swcampus-s3.s3.ap-northeast-2.amazonaws.com/board/abc123.png ->
     * abc123.png
     */
    private String extractFileNameFromUrl(String url) {
        try {
            return url.substring(url.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return "uploaded-file.dat";
        }
    }

    // 게시글 수정
    @Transactional
    @Override
    public void updateBoard(BoardUpdateRequestDTO boardUpdateRequestDTO, MultipartFile[] files) {
        Board board = boardRepository.findById(boardUpdateRequestDTO.getId())
                .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        // 삭제된 글 수정 불가 처리
        if (!board.isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }

        List<BoardAttach> boardAttachList = board.getBoardAttaches();
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.BOARD_ATTACH);

        // 1. 선택적 파일 삭제 (deleteAttachIds에 있는 파일만 삭제)
        List<Long> deleteAttachIds = boardUpdateRequestDTO.getDeleteAttachIds();
        if (deleteAttachIds != null && !deleteAttachIds.isEmpty()) {
            // Set으로 변환하여 조회 성능 O(1)으로 개선
            Set<Long> deleteAttachIdSet = new HashSet<>(deleteAttachIds);
            for (BoardAttach boardAttach : boardAttachList) {
                if (boardAttach.isActive() && deleteAttachIdSet.contains(boardAttach.getId())) {
                    deleteFile(boardAttach);
                    boardAttach.markDeleted();
                    // @Transactional 내에서 managed 엔티티는 dirty checking으로 자동 반영되므로 save 호출 불필요
                }
            }
        }

        // 2. 현재 활성 파일 개수 계산
        long activeFileCount = boardAttachList.stream()
                .filter(BoardAttach::isActive)
                .count();

        // 3. 새 파일 추가 (개수 검증 포함)
        if (files != null && files.length > 0) {
            // 총 파일 개수 검증
            if (!config.isFileCountValid((int) (activeFileCount + files.length))) {
                throw new BoardException(BoardErrorCode.FILE_COUNT_EXCEEDED);
            }

            for (MultipartFile file : files) {
                BoardAttach boardAttach = uploadFile(file);
                boardAttach.setBoard(board);
                boardAttachList.add(boardAttach);
            }
        }

        boardUpdateRequestDTO.updateEntity(board);
    }

    // 게시글 삭제
    @Transactional
    @Override
    public void deleteBoardById(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        // 삭제된 글 다시삭제 불가 처리
        if (!board.isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        board.markDeleted();
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, byte[]> downloadBoardAttach(Long boardId, Long boardAttachId, Long userId) {
        BoardAttach boardAttach = boardAttachRepository.findById(boardAttachId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.FILE_NOT_FOUND));
        if (!boardId.equals(boardAttach.getBoard().getId()) || !boardAttach.isActive()) {
            throw new BoardException(BoardErrorCode.FILE_NOT_FOUND);
        }
        // 비밀글 첨부파일 접근 권한 체크: 작성자 본인 또는 관리자만 접근 가능
        if (boardAttach.getBoard().isSecret()) {
            if (userId == null) {
                throw new BoardException(BoardErrorCode.FILE_ACCESS_FORBIDDEN);
            }
            Account viewer = accountRepository.findById(userId).orElse(null);
            boolean isOwner = userId.equals(boardAttach.getBoard().getAccount().getId());
            boolean isAdmin = viewer != null && viewer.getAccountType() == AccountType.ADMIN;
            if (!isOwner && !isAdmin) {
                throw new BoardException(BoardErrorCode.FILE_ACCESS_FORBIDDEN);
            }
        }

        byte[] fileBytes = s3Service.downloadFile(boardAttach.getRealFilename());
        return Map.of(boardAttach.getOriginalFilename(), fileBytes);
    }

    @Transactional
    @Override
    public void recommendBoard(Long boardId, Long userId) {

        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다. ID: " + userId));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        // 삭제된 글 조회 불가 처리
        if (!board.isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        if (boardRecommendRepository.findByBoardIdAndUserId(boardId, userId) != null) {
            throw new BoardException(BoardErrorCode.ALREADY_RECOMMEND_BOARD);
        }
        BoardRecommend boardRecommend = new BoardRecommend();
        boardRecommend.setBoard(board);
        boardRecommend.setAccount(account);

        boardRecommendRepository.save(boardRecommend);
    }

    @Transactional
    @Override
    public void unRecommendBoard(Long boardId, Long userId) {

        BoardRecommend boardRecommend = boardRecommendRepository.findByBoardIdAndUserId(boardId, userId);
        if (boardRecommend != null && !boardRecommend.getBoard().isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        if (boardRecommend == null) {
            throw new BoardException(BoardErrorCode.NOT_RECOMMEND_BOARD);
        }
        boardRecommendRepository.delete(boardRecommend);
    }

    @Transactional
    @Override
    public Long createComment(CommentCreateRequestDTO commentCreateRequestDTO, Long userId) {
        Board board = boardRepository.findById(commentCreateRequestDTO.getBoardId())
                .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        // 삭제된 글 조회 불가(댓글달기 불가)
        if (!board.isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        // 로그인한 사용자 조회
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다. ID: " + userId));
        Comment comment = commentCreateRequestDTO.toEntity(board, account);

        if (commentCreateRequestDTO.getTopCommentId() != null) {
            Comment topComment = commentRepository.findById(commentCreateRequestDTO.getTopCommentId())
                    .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
            if (!topComment.isActive()) {
                throw new BoardException(BoardErrorCode.COMMENT_NOT_FOUND);
            }
            comment.setTopComment(topComment);
        }

        commentRepository.save(comment);

        return comment.getId();
    }

    @Transactional
    @Override
    public void updateComment(CommentUpdateRequestDTO commentUpdateRequestDTO) {
        Comment comment = commentRepository.findById(commentUpdateRequestDTO.getId())
                .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        // 삭제된 댓글 수정 불가 처리
        if (!comment.isActive()) {
            throw new BoardException(BoardErrorCode.COMMENT_NOT_FOUND);
        }
        commentUpdateRequestDTO.updateEntity(comment);
    }

    @Transactional
    @Override
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        // 삭제된 댓글 재삭제 불가 처리
        if (!comment.isActive()) {
            throw new BoardException(BoardErrorCode.COMMENT_NOT_FOUND);
        }
        comment.markDeleted();
    }

    @Transactional
    @Override
    public void recommendComment(Long commentId, Long userId) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다. ID: " + userId));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        // 삭제된 댓글 추천 불가 처리
        if (!comment.isActive()) {
            throw new BoardException(BoardErrorCode.COMMENT_NOT_FOUND);
        }
        if (commentRecommendRepository.findByCommentIdAndUserId(commentId, userId) != null) {
            throw new BoardException(BoardErrorCode.ALREADY_RECOMMEND_COMMENT);
        }
        CommentRecommend commentRecommend = new CommentRecommend();
        commentRecommend.setAccount(account);
        commentRecommend.setComment(comment);

        commentRecommendRepository.save(commentRecommend);
    }

    @Transactional
    @Override
    public void unRecommendComment(Long commentId, Long userId) {
        CommentRecommend commentRecommend = commentRecommendRepository.findByCommentIdAndUserId(commentId, userId);
        if (commentRecommend != null && !commentRecommend.getComment().isActive()) {
            throw new BoardException(BoardErrorCode.COMMENT_NOT_FOUND);
        }
        if (commentRecommend == null) {
            throw new BoardException(BoardErrorCode.NOT_RECOMMEND_COMMENT);
        }

        commentRecommendRepository.delete(commentRecommend);
    }

    @Override
    public BoardAttach uploadFile(MultipartFile file) {
        // s3 bucket upload code
        String fileURL = s3Service.uploadFile(file, "board", FileType.FileTypeEnum.BOARD_ATTACH);
        return BoardAttach.builder()
                .originalFilename(file.getOriginalFilename())
                .realFilename(fileURL)
                .fileSize(file.getSize())
                .build();
    }

    @Override
    public void deleteFile(BoardAttach boardAttach) {
        s3Service.deleteFile(boardAttach.getRealFilename());
    }

}
