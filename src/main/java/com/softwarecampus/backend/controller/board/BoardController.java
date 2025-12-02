package com.softwarecampus.backend.controller.board;

import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.dto.board.*;
import com.softwarecampus.backend.exception.board.BoardException;
import com.softwarecampus.backend.security.CustomUserDetails;
import com.softwarecampus.backend.service.board.BoardService;
import com.softwarecampus.backend.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

    private final BoardService boardService;
    private final ClientIpUtils clientIpUtils;

    // 게시글 목록 조회, 목록 검색기능
    @GetMapping
    public ResponseEntity<?> getBoards(@RequestParam(defaultValue = "1") int pageNo, BoardCategory category,
            @RequestParam(required = false) String searchType, @RequestParam(required = false) String searchText,
            @RequestParam(required = false, defaultValue = "latest") String sortType) {

        Page<BoardListResponseDTO> boards = boardService.getBoards(pageNo, category, searchType, searchText, sortType);
        return ResponseEntity.ok(boards);
    }

    // 게시글 하나 조회 with 댓글
    @GetMapping("/{boardId:\\d+}")
    public ResponseEntity<?> getBoard(@PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {

        Long userId = userDetails != null ? userDetails.getId() : null;
        String clientIp = clientIpUtils.getClientIp(request);

        // 게시글 하나 조회 service 메서드 호출
        BoardResponseDTO board = boardService.getBoardById(boardId, userId, clientIp);
        return ResponseEntity.ok(board);
    }

    // 게시글 생성 with 첨부파일

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<?> createBoard(@Valid BoardCreateRequestDTO boardCreateRequestDTO,
            @RequestParam(required = false) MultipartFile[] files,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 게시글 생성 service 메서드 호출
        Long boardId = boardService.createBoard(boardCreateRequestDTO, files, userDetails.getId());
        return ResponseEntity.created(URI.create("/api/boards/" + boardId)).build();
    }

    // 게시글 수정 with 첨부파일
    @PreAuthorize("hasRole('ROLE_ADMIN') or @boardAuthorizeService.canManipulateBoard(#boardId,principal.id)")
    @PatchMapping("/{boardId:\\d+}")
    public ResponseEntity<?> updateBoard(@PathVariable Long boardId, @Valid BoardUpdateRequestDTO boardUpdateRequestDTO,
            @RequestParam(required = false) MultipartFile[] files) {
        // 경로 변수 boardId를 DTO에 명시적으로 설정하여 권한 검증 우회 방지
        boardUpdateRequestDTO.setId(boardId);
        // 게시글 수정 service 메서드 호출
        boardService.updateBoard(boardUpdateRequestDTO, files);
        return ResponseEntity.noContent().build();
    }

    // 게시글 하나 삭제
    @PreAuthorize("hasRole('ROLE_ADMIN') or @boardAuthorizeService.canManipulateBoard(#boardId,principal.id)")
    @DeleteMapping("/{boardId:\\d+}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long boardId) {

        // 게시글 삭제 service 메서드 호출
        boardService.deleteBoardById(boardId);
        return ResponseEntity.noContent().build();
    }

    // 게시글 첨부파일 다운로드
    @GetMapping("/{boardId:\\d+}/boardAttachs/{boardAttachId:\\d+}/download")
    public ResponseEntity<?> downloadBoardAttach(@PathVariable Long boardId, @PathVariable Long boardAttachId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, byte[]> file = boardService.downloadBoardAttach(boardId, boardAttachId,
                userDetails != null ? userDetails.getId() : null);

        String fileName = file.keySet().iterator().next();

        String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("Content-Disposition",
                "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);

        return new ResponseEntity<>(file.get(fileName), headers, HttpStatus.OK);
    }

    // 댓글 하나 생성
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{boardId:\\d+}/comments")
    public ResponseEntity<?> createComment(@PathVariable Long boardId,
            @Valid @RequestBody CommentCreateRequestDTO commentCreateRequestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        commentCreateRequestDTO.setBoardId(boardId);
        // 1L자리에 사용자 id 전달
        Long commentId = boardService.createComment(commentCreateRequestDTO, userDetails.getId());
        return ResponseEntity.created(URI.create("/api/boards/" + boardId + "/comments/" + commentId)).build();
    }

    // 댓글 수정
    @PatchMapping("/{boardId:\\d+}/comments/{commentId:\\d+}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or  @boardAuthorizeService.canManipulateComment(#commentId,principal.id)")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequestDTO commentUpdateRequestDTO) {

        commentUpdateRequestDTO.setId(commentId);
        boardService.updateComment(commentUpdateRequestDTO);
        return ResponseEntity.noContent().build();
    }

    // 댓글 삭제
    @DeleteMapping("/{boardId:\\d+}/comments/{commentId:\\d+}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or  @boardAuthorizeService.canManipulateComment(#commentId,principal.id)")
    public ResponseEntity<?> deleteComment(@PathVariable Long boardId, @PathVariable Long commentId) {

        boardService.deleteComment(commentId);

        return ResponseEntity.noContent().build();
    }

    // 게시글 추천

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{boardId:\\d+}/recommends")
    public ResponseEntity<?> recommendBoard(@PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 실제로 사용자 ID를 인자로 넘겨야 함
        boardService.recommendBoard(boardId, userDetails.getId());
        return ResponseEntity.created(URI.create("/api/boards/" + boardId)).build();
    }

    // 게시글 추천취소

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{boardId:\\d+}/recommends")
    public ResponseEntity<?> recommendBoardCancel(@PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boardService.unRecommendBoard(boardId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // 댓글 추천

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{boardId:\\d+}/comments/{commentId:\\d+}/recommends")
    public ResponseEntity<?> recommendComment(@PathVariable Long boardId, @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 실제론 사용자 ID를 인자로 넘겨야 함
        boardService.recommendComment(commentId, userDetails.getId());
        return ResponseEntity.created(URI.create("/api/boards/" + boardId + "/comments/" + commentId)).build();
    }

    // 댓글 추천취소

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{boardId:\\d+}/comments/{commentId:\\d+}/recommends")
    public ResponseEntity<?> recommendCommentCancel(@PathVariable Long boardId, @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 실제론 사용자 ID를 인자로 넘겨야 함
        boardService.unRecommendComment(commentId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(BoardException.class)
    public ProblemDetail handleBoardException(BoardException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(e.getErrorCode().getHttpStatus());

        problemDetail.setTitle(e.getErrorCode().toString());
        problemDetail.setDetail(e.getErrorCode().getDetails());
        problemDetail.setProperty("timestamp",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

}
