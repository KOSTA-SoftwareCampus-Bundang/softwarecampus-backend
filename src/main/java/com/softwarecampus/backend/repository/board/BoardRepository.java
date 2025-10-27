package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.common.BoardCategory;
import com.softwarecampus.backend.domain.user.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 게시글 레포지토리
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    
    // 카테고리별 게시글 목록 조회 (삭제되지 않은 것만)
    Page<Board> findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(
        BoardCategory category,
        Pageable pageable
    );
    
    // 내가 작성한 글 목록
    List<Board> findByAccountAndIsDeletedFalseOrderByCreatedAtDesc(Account account);
    
    // 제목 또는 내용 검색
    @Query("SELECT b FROM Board b WHERE " +
           "b.isDeleted = false AND " +
           "(:category IS NULL OR b.category = :category) AND " +
           "(b.title LIKE %:keyword% OR b.text LIKE %:keyword%)")
    Page<Board> searchBoards(
        @Param("category") BoardCategory category,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
