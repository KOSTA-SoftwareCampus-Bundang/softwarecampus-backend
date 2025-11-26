package com.softwarecampus.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.dto.board.*;
import com.softwarecampus.backend.service.board.BoardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;


@SpringBootTest
public class SoftwarecampusBackendApplicationBoardTests {

    @Autowired
    private  BoardService boardService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("게시글 하나 조회 테스트")
    void getBoard() throws JsonProcessingException {

        BoardResponseDTO dto = boardService.getBoardById(2L,1L);
        String json = objectMapper.writeValueAsString(dto);
        System.out.println(json);

    }

    @Test
    @DisplayName("게시글 여러개 조회 테스트")
    void getBoards() throws JsonProcessingException {


        Page<BoardListResponseDTO> dto = boardService.getBoards(1, BoardCategory.NOTICE,"title",null);
//        Page<BoardListResponseDTO> dto = boardService.getBoards(1, BoardCategory.COURSE_STORY,null,"안녕");
//        Page<BoardListResponseDTO> dto = boardService.getBoards(1, BoardCategory.COURSE_STORY,"title+text","제목3");
        String json = objectMapper.writeValueAsString(dto);
        System.out.println(json);

    }

    @Test
    @DisplayName("게시글 생성 테스트")
    void createBoard() {
        BoardCreateRequestDTO dto = BoardCreateRequestDTO.builder().title("제목5").text("제목4").category(BoardCategory.NOTICE).build();
        boardService.createBoard(dto,null,1L);

    }

    @Test
    @DisplayName("게시글 수정 테스트")
    void updateBoard() {
        BoardUpdateRequestDTO dto = BoardUpdateRequestDTO.builder().id(1L).secret(true).text("글5번입니다").build();
        boardService.updateBoard(dto,null);

    }

    @Test
    @DisplayName("게시글 삭제 테스트")
    void deleteBoard() {

        boardService.deleteBoardById(1L);

    }

    @Test
    @DisplayName("게시글 추천 테스트")
    void recommendBoard() {

        boardService.recommendBoard(2L,1L);

    }

    @Test
    @DisplayName("게시글 비추 테스트")
    void unRecommendBoard() {

        boardService.unRecommendBoard(1L,1L);

    }

    @Test
    @DisplayName("댓글 생성 테스트")
    void createComment() {

        boardService.createComment(CommentCreateRequestDTO.builder().boardId(2L).topCommentId(4L).text("댓글8").build(),1L);

    }

    @Test
    @DisplayName("댓글 수정 테스트")
    void updateComment() {

        boardService.updateComment(CommentUpdateRequestDTO.builder().id(2L).text("HI").build());

    }

    @Test
    @DisplayName("댓글 삭제 테스트")
    void deleteComment() {

        boardService.deleteComment(1L);

    }

    @Test
    @DisplayName("댓글 추천 테스트")
    void recommendComment() {

        boardService.recommendComment(2L,1L);

    }

    @Test
    @DisplayName("댓글 비추 테스트")
    void unRecommendComment() {

        boardService.unRecommendComment(2L,1L);

    }
}
