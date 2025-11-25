package com.softwarecampus.backend.service.board;


import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardAttach;
import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.dto.board.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface BoardService {

    public BoardAttach uploadFile(MultipartFile file);

    public void deleteFile(BoardAttach boardAttach);

    //전체 게시글 조회
    public Page<BoardListResponseDTO> getBoards(int pageNo, BoardCategory category, String searchType, String searchText);

    //게시글 하나 조회
    public BoardResponseDTO getBoardById(Long id, Long userId);

    //게시글 생성
    public Long createBoard(BoardCreateRequestDTO boardCreateRequestDTO, MultipartFile[] files, Long userId);

    //게시글 수정
    public void updateBoard(BoardUpdateRequestDTO boardUpdateRequestDTO, MultipartFile[] files);

    //게시글 삭제
    public void deleteBoardById(Long id);

    //게시글 추천
    public void recommendBoard(Long BoardId, Long userId);

    //게시글 추천취소
    public void unRecommendBoard(Long boardId, Long userId);
    
    //댓글 생성
    public Long createComment(CommentCreateRequestDTO commentCreateRequestDTO, Long userId);

    //댓글 수정
    public void updateComment(CommentUpdateRequestDTO commentUpdateRequestDTO);

    //댓글 삭제
    public void deleteComment(Long id);

    //댓글 추천
    public void recommendComment(Long commentId, Long userId);

    //댓글 추천 취소
    public void unRecommendComment(Long commentId, Long userId);

}
