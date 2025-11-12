package com.softwarecampus.backend.repository.board;


import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findBoardsByCategoryAndIsDeletedFalse(BoardCategory category);

//    @Query
//    List<Board> findBoardsBySearchText(String searchType, String searchText);
}
