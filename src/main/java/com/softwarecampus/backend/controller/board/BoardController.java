package com.softwarecampus.backend.controller.board;


import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.dto.board.BoardCreateRequestDTO;
import com.softwarecampus.backend.dto.board.BoardUpdateRequestDTO;
import com.softwarecampus.backend.dto.board.CommentCreateRequestDTO;
import com.softwarecampus.backend.dto.board.CommentUpdateRequestDTO;
import com.softwarecampus.backend.service.board.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
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
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<?> getBoard(Long boardId) {

        //게시글 하나 조회 service 메서드 호출
        Board board = boardService.getBoardById(boardId);
        return ResponseEntity.ok(board);
    }

    //게시글 생성 with 첨부파일
    @PostMapping
    public ResponseEntity<?> createBoard(@Valid @RequestBody BoardCreateRequestDTO boardCreateRequestDTO, MultipartFile[] files) {

        //게시글 생성 service 메서드 호출
        boardService.createBoard(boardCreateRequestDTO, files);
        return ResponseEntity.created(URI.create("/boards")).build();

    }

    //게시글 수정 with 첨부파일
    @PatchMapping
    public ResponseEntity<?> updateBoard(@Valid @RequestBody BoardUpdateRequestDTO boardUpdateRequestDTO) {
        //게시글 수정 service 메서드 호출
        boardService.updateBoard(boardUpdateRequestDTO);
        return ResponseEntity.noContent().build();
    }

    //게시글 하나 삭제
    @DeleteMapping
    public ResponseEntity<?> deleteBoard(Long boardId) {

        //게시글 삭제 service 메서드 호출
        boardService.deleteBoardById(boardId);
        return ResponseEntity.noContent().build();
    }

    //댓글 하나 생성
    @PostMapping("/boards/{id:\\d+}/comments")
    public ResponseEntity<?> createComment(@PathVariable Long id, @Valid @RequestBody CommentCreateRequestDTO commentCreateRequestDTO) {
        return ResponseEntity.created(URI.create("/boards/" + id)).build();
    }

    //댓글 수정
    @PatchMapping("/boards/{boardId:\\d+}/comments/{commentId:\\d+}")
    public ResponseEntity<?> updateComment(@PathVariable Long boardId, @PathVariable Long commentId, @Valid @RequestBody CommentUpdateRequestDTO commentUpdateRequestDTO) {


        return ResponseEntity.noContent().build();
    }

    //댓글 삭제
    @DeleteMapping("/boards/{boardId:\\d+}/comments/{commentId:\\d+}")
    public ResponseEntity<?> deleteComment(@PathVariable Long boardId, @PathVariable Long commentId) {


        return ResponseEntity.noContent().build();
    }

    //게시글 추천/비추천
    @PostMapping("/boards/{boardId:\\d+}/recommends")
    public ResponseEntity<?> recommendBoard(@PathVariable Long boardId) {
        return ResponseEntity.created(URI.create("/boards/" + boardId)).build();
    }

    //댓글 추천/비추천
    @PostMapping("/boards/{boardId:\\d+}/comments/{commentId:\\d+}/recommends")
    public ResponseEntity<?> recommendComment(@PathVariable Long boardId, @PathVariable Long commentId) {
        return ResponseEntity.created(URI.create("/boards/" + boardId + "/comments/" + commentId)).build();
    }


}
