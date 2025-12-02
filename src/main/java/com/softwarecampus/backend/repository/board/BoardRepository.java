package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.dto.board.BoardListResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

        // 게시글 상세 조회용: Account + Recommends + Attaches + Comments 한번에 조회
        @Query("SELECT DISTINCT b FROM Board b " +
                        "LEFT JOIN FETCH b.account " +
                        "LEFT JOIN FETCH b.boardAttaches " +
                        "LEFT JOIN FETCH b.boardRecommends br " +
                        "LEFT JOIN FETCH br.account " +
                        "WHERE b.id = :id")
        Optional<Board> findByIdWithDetails(@Param("id") Long id);

        @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),MAX(a.id),count(distinct case when c.isDeleted=false then c.id end),MAX(b.hits),count(distinct r.id),max(b.createdAt)) from Board b left join b.boardRecommends r left join b.comments c join b.account a "
                        +
                        "WHERE (:category IS NULL OR b.category=:category) and b.isDeleted = false GROUP BY b.id", countQuery = "select count(b) from Board b WHERE (:category IS NULL OR b.category=:category) and b.isDeleted=false")
        Page<BoardListResponseDTO> findBoardsByCategory(@Param("category") BoardCategory category, Pageable pageable);

        @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),MAX(a.id),count(distinct case when c.isDeleted=false then c.id end),MAX(b.hits),count(distinct r.id),max(b.createdAt)) from Board b left join b.boardRecommends r left join b.comments c join b.account a "
                        +
                        "WHERE (:category IS NULL OR b.category=:category) and b.title like :searchText and b.isDeleted=false GROUP BY b.id", countQuery = "select count(b) from Board b WHERE (:category IS NULL OR b.category=:category) and b.title like :searchText and b.isDeleted=false")
        Page<BoardListResponseDTO> findBoardsByTitle(@Param("category") BoardCategory category,
                        @Param("searchText") String searchText, Pageable pageable);

        @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),MAX(a.id),count(distinct case when c.isDeleted=false then c.id end),MAX(b.hits),count(distinct r.id),max(b.createdAt)) from Board b left join b.boardRecommends r left join b.comments c join b.account a "
                        +
                        "WHERE (:category IS NULL OR b.category=:category) and b.text like :searchText and b.isDeleted=false GROUP BY b.id", countQuery = "select count(b) from Board b WHERE (:category IS NULL OR b.category=:category) and b.text like :searchText and b.isDeleted=false")
        Page<BoardListResponseDTO> findBoardsByText(@Param("category") BoardCategory category,
                        @Param("searchText") String searchText, Pageable pageable);

        @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),MAX(a.id),count(distinct case when c.isDeleted=false then c.id end),MAX(b.hits),count(distinct r.id),max(b.createdAt)) from Board b left join b.boardRecommends r left join b.comments c join b.account a "
                        +
                        "WHERE (:category IS NULL OR b.category=:category) and (b.title like :searchText OR b.text like :searchText) and b.isDeleted=false GROUP BY b.id", countQuery = "select count(b) from Board b WHERE (:category IS NULL OR b.category=:category) and (b.title like :searchText OR b.text like :searchText) and b.isDeleted=false")
        Page<BoardListResponseDTO> findBoardsByTitleAndText(@Param("category") BoardCategory category,
                        @Param("searchText") String searchText, Pageable pageable);

        // 글쓴이(작성자) 검색
        @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),MAX(a.id),count(distinct case when c.isDeleted=false then c.id end),MAX(b.hits),count(distinct r.id),max(b.createdAt)) from Board b left join b.boardRecommends r left join b.comments c join b.account a "
                        +
                        "WHERE (:category IS NULL OR b.category=:category) and a.userName like :searchText and b.isDeleted=false GROUP BY b.id", countQuery = "select count(b) from Board b join b.account a WHERE (:category IS NULL OR b.category=:category) and a.userName like :searchText and b.isDeleted=false")
        Page<BoardListResponseDTO> findBoardsByAuthor(@Param("category") BoardCategory category,
                        @Param("searchText") String searchText, Pageable pageable);

        // 댓글 내용 검색
        @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),MAX(a.id),count(distinct case when c.isDeleted=false then c.id end),MAX(b.hits),count(distinct r.id),max(b.createdAt)) from Board b left join b.boardRecommends r left join b.comments c join b.account a "
                        +
                        "WHERE (:category IS NULL OR b.category=:category) and c.text like :searchText and c.isDeleted=false and b.isDeleted=false GROUP BY b.id", countQuery = "select count(distinct b) from Board b left join b.comments c WHERE (:category IS NULL OR b.category=:category) and c.text like :searchText and c.isDeleted=false and b.isDeleted=false")
        Page<BoardListResponseDTO> findBoardsByComment(@Param("category") BoardCategory category,
                        @Param("searchText") String searchText, Pageable pageable);

        // 전체 검색 (제목 + 내용 + 글쓴이)
        @Query(value = "SELECT new com.softwarecampus.backend.dto.board.BoardListResponseDTO(b.id,MAX(b.category),MAX(b.title),MAX(b.secret),MAX(a.userName),MAX(a.id),count(distinct case when c.isDeleted=false then c.id end),MAX(b.hits),count(distinct r.id),max(b.createdAt)) from Board b left join b.boardRecommends r left join b.comments c join b.account a "
                        +
                        "WHERE (:category IS NULL OR b.category=:category) and (b.title like :searchText OR b.text like :searchText OR a.userName like :searchText) and b.isDeleted=false GROUP BY b.id", countQuery = "select count(b) from Board b join b.account a WHERE (:category IS NULL OR b.category=:category) and (b.title like :searchText OR b.text like :searchText OR a.userName like :searchText) and b.isDeleted=false")
        Page<BoardListResponseDTO> findBoardsByAll(@Param("category") BoardCategory category,
                        @Param("searchText") String searchText, Pageable pageable);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "account" })
        Page<Board> findByIsDeletedFalse(Pageable pageable);
}
