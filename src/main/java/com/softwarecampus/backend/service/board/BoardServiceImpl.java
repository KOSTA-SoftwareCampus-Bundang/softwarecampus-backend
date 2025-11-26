package com.softwarecampus.backend.service.board;

import com.softwarecampus.backend.domain.board.*;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.board.*;
import com.softwarecampus.backend.exception.board.BoardErrorCode;
import com.softwarecampus.backend.exception.board.BoardException;
import com.softwarecampus.backend.repository.board.*;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final BoardAttachRepository boardAttachRepository;
    private final AccountRepository accountRepository;
    private final CommentRepository commentRepository;
    private final BoardRecommendRepository boardRecommendRepository;
    private final CommentRecommendRepository commentRecommendRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<BoardListResponseDTO> getBoards(int pageNo, BoardCategory category, String searchType, String searchText) {
        PageRequest pageRequest = PageRequest.of(pageNo - 1, 10, Sort.by("id").descending());

        if (searchType == null || "".equals(searchType) || searchText == null || "".equals(searchText)) {
            return boardRepository.findBoardsByCategory(category, pageRequest);
        } else {
            if ("title".equals(searchType)) {
                return boardRepository.findBoardsByTitle(category, "%" + searchText + "%", pageRequest);
            } else if ("text".equals(searchType)) {
                return boardRepository.findBoardsByText(category, "%" + searchText + "%", pageRequest);
            } else if ("title+text".equals(searchType)) {
                return boardRepository.findBoardsByTitleAndText(category, "%" + searchText + "%", pageRequest);
            } else {
                throw new BoardException(BoardErrorCode.SEARCHTYPE_MISSMATCH);
            }
        }
    }

    //
    @Transactional(readOnly = true)
    @Override
    public BoardResponseDTO getBoardById(Long id, Long userId) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        //삭제된 글 조회 불가 처리
        if (!board.isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        board.setHits(board.getHits() + 1);
        BoardResponseDTO boardResponseDTO = BoardResponseDTO.from(board);
        if (userId != null) {
            boardResponseDTO.setLike(board.getBoardRecommends().stream().anyMatch(br -> br.getAccount().getId().equals(userId)));
        }
        return boardResponseDTO;
    }

    //게시글 생성
    @Transactional
    @Override
    public Long createBoard(BoardCreateRequestDTO boardCreateRequestDTO, MultipartFile[] files, Long userId) {

        Board board = boardCreateRequestDTO.toEntity();

        //파일업로드 코드 작성
        List<BoardAttach> boardAttachList = board.getBoardAttaches();
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                BoardAttach boardAttach = uploadFile(file);
                boardAttachList.add(boardAttach);
                boardAttach.setBoard(board);
            }
        }
        //사용자 조회하는 코드 작성(실제론 1l 대신 로그인한 사용자의 id값 인자로 전달)
        Account account = accountRepository.findById(1L).get();
        //조회된 사용자 boardEntity에 세팅 후 save로 저장
        board.setAccount(account);
        boardRepository.save(board);

        return board.getId();
    }

    //게시글 수정
    @Transactional
    @Override
    public void updateBoard(BoardUpdateRequestDTO boardUpdateRequestDTO, MultipartFile[] files) {
        Board board = boardRepository.findById(boardUpdateRequestDTO.getId()).orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        //삭제된 글 수정 불가 처리
        if (!board.isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        List<BoardAttach> boardAttachList = board.getBoardAttaches();
        if (files != null && files.length > 0) {
            if (boardAttachList.size() > 0) {
                for (BoardAttach boardAttach : board.getBoardAttaches()) {
                    deleteFile(boardAttach);
                    boardAttach.markDeleted();
                    boardAttachRepository.save(boardAttach);
                }
            }
            for (MultipartFile file : files) {
                BoardAttach boardAttach = uploadFile(file);
                boardAttach.setBoard(board);
                boardAttachList.add(boardAttach);
            }
        }
        boardUpdateRequestDTO.updateEntity(board);
    }

    //게시글 삭제
    @Transactional
    @Override
    public void deleteBoardById(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        //삭제된 글 다시삭제 불가 처리
        if (!board.isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        board.markDeleted();
    }

    @Transactional
    @Override
    public void recommendBoard(Long boardId, Long userId) {

        Account account = accountRepository.findById(userId).get();
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        //삭제된 글 조회 불가 처리
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
        Board board = boardRepository.findById(commentCreateRequestDTO.getBoardId()).orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        //삭제된 글 조회 불가(댓글달기 불가)
        if (!board.isActive()) {
            throw new BoardException(BoardErrorCode.BOARD_NOT_FOUND);
        }
        //실제론 1L 대신 userId가 인자로 전달
        Account account = accountRepository.findById(1L).get();
        Comment comment = commentCreateRequestDTO.toEntity(board, account);
        Comment topComment = null;
        if (commentCreateRequestDTO.getTopCommentId() != null) {
            topComment = commentRepository.findById(commentCreateRequestDTO.getTopCommentId()).orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        }
        if (!topComment.isActive()) {
            throw new BoardException(BoardErrorCode.COMMENT_NOT_FOUND);
        }
        comment.setTopComment(topComment);
        commentRepository.save(comment);

        return comment.getId();
    }

    @Transactional
    @Override
    public void updateComment(CommentUpdateRequestDTO commentUpdateRequestDTO) {
        Comment comment = commentRepository.findById(commentUpdateRequestDTO.getId()).orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        //삭제된 댓글 수정 불가 처리
        if (!comment.isActive()) {
            throw new BoardException(BoardErrorCode.COMMENT_NOT_FOUND);
        }
        commentUpdateRequestDTO.updateEntity(comment);
    }

    @Transactional
    @Override
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        //삭제된 댓글 재삭제 불가 처리
        if (!comment.isActive()) {
            throw new BoardException(BoardErrorCode.COMMENT_NOT_FOUND);
        }
        comment.markDeleted();
    }

    @Transactional
    @Override
    public void recommendComment(Long commentId, Long userId) {
        Account account = accountRepository.findById(userId).get();
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        //삭제된 댓글 추천 불가 처리
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
        //s3 bucket upload code

        return BoardAttach.builder().originalFilename(file.getOriginalFilename()).realFilename("xxxxxxxx").build();
    }

    @Override
    public void deleteFile(BoardAttach boardAttach) {
        //s3 버킷에서 삭제하는 코드

    }

}
