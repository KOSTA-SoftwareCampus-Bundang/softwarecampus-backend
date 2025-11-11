package com.softwarecampus.backend.service.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardAttach;
import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.dto.board.BoardCreateRequestDTO;
import com.softwarecampus.backend.dto.board.BoardUpdateRequestDTO;
import com.softwarecampus.backend.repository.board.BoardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;

    @Override
    public List<Board> getBoards(int pageNo, BoardCategory category, String searchType, String searchText) {
        if (searchType == null || "".equals(searchType) || searchText == null || "".equals(searchText)) {
            return boardRepository.findBoardsByCategory(category);
        } else {
            return null;
        }
    }

    //
    @Override
    public Board getBoardById(Long id) {
        return boardRepository.findById(id).orElseThrow(() -> new IllegalStateException("게시글이 존재하지 않습니다"));
    }

    //게시글 생성
    @Transactional
    @Override
    public Long createBoard(BoardCreateRequestDTO boardCreateRequestDTO, MultipartFile[] files) {
        //파일업로드 코드 작성
        List<BoardAttach> boardAttachList = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                boardAttachList.add(uploadFile(file, boardCreateRequestDTO.getId()));
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException("파일업로드 중 오류가 발생하였습니다");
            }
        }
        //사용자 조회하는 코드 작성
        //
        return null;
    }

    //게시글 수정
    @Transactional
    @Override
    public void updateBoard(BoardUpdateRequestDTO boardUpdateRequestDTO) {
        Board board = boardRepository.findById(boardUpdateRequestDTO.getId()).orElseThrow(() -> new IllegalStateException("게시글을 찾지 못했습니다"));
        boardUpdateRequestDTO.updateEntity(board);

    }

    //게시글 삭제
    @Transactional
    @Override
    public void deleteBoardById(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new IllegalStateException("게시글을 찾지 못했습니다"));
        boardRepository.delete(board);
    }

    @Override
    public BoardAttach uploadFile(MultipartFile file, Long boardId) throws IOException {


    }
}
