package com.softwarecampus.backend.controller.board;


import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.dto.board.BoardCreateRequestDTO;
import com.softwarecampus.backend.dto.board.BoardUpdateRequestDTO;
import com.softwarecampus.backend.dto.board.CommentCreateRequestDTO;
import com.softwarecampus.backend.dto.board.CommentUpdateRequestDTO;
import com.softwarecampus.backend.exception.board.BoardException;
import com.softwarecampus.backend.service.board.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    //게시글 목록 조회, 목록 검색기능
    @GetMapping
    public ResponseEntity<?> getBoards(@RequestParam(defaultValue = "1") int pageNo, String category, @RequestParam(required = false) String searchType, @RequestParam(required = false) String searchText) {

        List<Board> boards = boardService.getBoards(pageNo, BoardCategory.from(category), searchType, searchText);
        return ResponseEntity.ok(boards);
    }

    //게시글 하나 조회 with 댓글
    @GetMapping("/{boardId:\\d+}")
    public ResponseEntity<?> getBoard(@PathVariable Long boardId) {

        //게시글 하나 조회 service 메서드 호출
        Board board = boardService.getBoardById(boardId);
        return ResponseEntity.ok(board);
    }

    //게시글 생성 with 첨부파일
    @PostMapping
    public ResponseEntity<?> createBoard(@Valid BoardCreateRequestDTO boardCreateRequestDTO,@RequestParam(required = false) MultipartFile[] files) {

        //게시글 생성 service 메서드 호출
        boardService.createBoard(boardCreateRequestDTO, files);
        return ResponseEntity.created(URI.create("/boards")).build();

    }

    //게시글 수정 with 첨부파일
    @PatchMapping("/{boardId:\\d+}")
    public ResponseEntity<?> updateBoard(@PathVariable Long boardId, @Valid BoardUpdateRequestDTO boardUpdateRequestDTO,@RequestParam(required = false) MultipartFile[] files) {
        //게시글 수정 service 메서드 호출
        boardService.updateBoard(boardUpdateRequestDTO, files);
        return ResponseEntity.noContent().build();
    }

    //게시글 하나 삭제
    @DeleteMapping("/{boardId:\\d+}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long boardId) {

        //게시글 삭제 service 메서드 호출
        boardService.deleteBoardById(boardId);
        return ResponseEntity.noContent().build();
    }

    //댓글 하나 생성
    @PostMapping("/{id:\\d+}/comments")
    public ResponseEntity<?> createComment(@PathVariable Long id, @Valid @RequestBody CommentCreateRequestDTO commentCreateRequestDTO) {
        return ResponseEntity.created(URI.create("/boards/" + id)).build();
    }

    //댓글 수정
    @PatchMapping("/{boardId:\\d+}/comments/{commentId:\\d+}")
    public ResponseEntity<?> updateComment(@PathVariable Long boardId, @PathVariable Long commentId, @Valid @RequestBody CommentUpdateRequestDTO commentUpdateRequestDTO) {


        return ResponseEntity.noContent().build();
    }

    //댓글 삭제
    @DeleteMapping("/{boardId:\\d+}/comments/{commentId:\\d+}")
    public ResponseEntity<?> deleteComment(@PathVariable Long boardId, @PathVariable Long commentId) {


        return ResponseEntity.noContent().build();
    }

    //게시글 추천
    @PostMapping("/{boardId:\\d+}/recommends")
    public ResponseEntity<?> recommendBoard(@PathVariable Long boardId) {
        return ResponseEntity.created(URI.create("/boards/" + boardId)).build();
    }

    //댓글 추천
    @PostMapping("/{boardId:\\d+}/comments/{commentId:\\d+}/recommends")
    public ResponseEntity<?> recommendComment(@PathVariable Long boardId, @PathVariable Long commentId) {
        return ResponseEntity.created(URI.create("/boards/" + boardId + "/comments/" + commentId)).build();
    }

    //게시글 추천취소
    @DeleteMapping("/{boardId:\\d+}/recommends")
    public ResponseEntity<?> recommendBoardCancel(@PathVariable Long boardId) {
        return ResponseEntity.noContent().build();
    }

    //댓글 추천취소
    @DeleteMapping("/{boardId:\\d+}/comments/{commentId:\\d+}/recommends")
    public ResponseEntity<?> recommendCommentCancel(@PathVariable Long boardId, @PathVariable Long commentId) {
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(BoardException.class)
    public ProblemDetail handleBoardException(BoardException e, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(e.getErrorCode().getHttpStatus());

        problemDetail.setTitle(e.getErrorCode().toString());
        problemDetail.setDetail(e.getErrorCode().getDetails());
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }


}
