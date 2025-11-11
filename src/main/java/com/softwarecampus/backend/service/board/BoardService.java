package com.softwarecampus.backend.service.board;


import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardAttach;
import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.dto.board.BoardCreateRequestDTO;
import com.softwarecampus.backend.dto.board.BoardUpdateRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface BoardService {

    //전체 게시글 조회
    public List<Board> getBoards(int pageNo, BoardCategory category, String searchType, String searchText);

    //게시글 하나 조회
    public Board getBoardById(Long id);

    //게시글 생성
    public Long createBoard(BoardCreateRequestDTO boardCreateRequestDTO, MultipartFile[] files);

    //게시글 수정
    public void updateBoard(BoardUpdateRequestDTO boardUpdateRequestDTO, MultipartFile[] files);

    //게시글 삭제
    public void deleteBoardById(Long id);

    public BoardAttach uploadFile(MultipartFile file) throws IOException;

}
