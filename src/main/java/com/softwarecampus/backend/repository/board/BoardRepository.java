package com.softwarecampus.backend.repository.board;


import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.dto.board.BoardListResponseDTO;
import com.softwarecampus.backend.dto.board.BoardResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),count(c),DATE_FORMAT(MAX(b.createdAt), '%Y-%m-%d %H:%i:%s')) from Board b left join b.boardRecommends r left join Comment c join b.account a " +
            "WHERE b.category=:category GROUP BY b", countQuery = "select count(b) from Board b WHERE b.category=:category")
    Page<BoardListResponseDTO> findBoardsByCategory(@Param("category") BoardCategory category, Pageable pageable);

    @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),count(c),DATE_FORMAT(MAX(b.createdAt), '%Y-%m-%d %H:%i:%s')) from Board b left join b.boardRecommends r left join Comment c join b.account a " +
            "WHERE b.category=:category and b.title like %:searchText% GROUP BY b", countQuery = "select count(b) from Board b WHERE b.category=:category and b.title like %:searchText%")
    Page<BoardListResponseDTO> findBoardsByTitle(@Param("category") BoardCategory category, @Param("searchText") String searchText, Pageable pageable);

    @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),count(c),DATE_FORMAT(MAX(b.createdAt), '%Y-%m-%d %H:%i:%s')) from Board b left join b.boardRecommends r left join Comment c join b.account a " +
            "WHERE b.category=:category and b.text like %:searchText% GROUP BY b", countQuery = "select count(b) from Board b WHERE b.category=:category and b.text like %:searchText%")
    Page<BoardListResponseDTO> findBoardsByText(@Param("category") BoardCategory category, @Param("searchText") String searchText, Pageable pageable);

    @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),count(c),DATE_FORMAT(MAX(b.createdAt), '%Y-%m-%d %H:%i:%s')) from Board b left join b.boardRecommends r left join Comment c join b.account a " +
            "WHERE b.category=:category and b.title like %:searchText% OR b.text like %:searchText% GROUP BY b", countQuery = "select count(b) from Board b WHERE b.category=:category and (b.title like %:searchText% OR b.text like %:searchText%)")
    Page<BoardListResponseDTO> findBoardsByTitleAndText(@Param("category") BoardCategory category, @Param("searchText") String searchText, Pageable pageable);
}
