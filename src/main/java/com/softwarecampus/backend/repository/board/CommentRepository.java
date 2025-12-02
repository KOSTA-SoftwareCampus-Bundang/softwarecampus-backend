package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.Comment;
import com.softwarecampus.backend.dto.mypage.MyCommentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ===== 마이페이지용 쿼리 =====

    // 내가 쓴 댓글 목록 조회
    @Query(value = "SELECT new com.softwarecampus.backend.dto.mypage.MyCommentResponseDTO(" +
            "c.id, c.text, c.board.id, c.board.title, c.createdAt) " +
            "FROM Comment c " +
            "WHERE c.account.id = :accountId AND c.isDeleted = false AND c.board.isDeleted = false", countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.account.id = :accountId AND c.isDeleted = false AND c.board.isDeleted = false")
    Page<MyCommentResponseDTO> findMyComments(@Param("accountId") Long accountId, Pageable pageable);

    // 내가 쓴 댓글 수
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.account.id = :accountId AND c.isDeleted = false")
    Long countByAccountId(@Param("accountId") Long accountId);

    // 권한 체크용: Account만 Fetch Join
    @Query("SELECT c FROM Comment c JOIN FETCH c.account WHERE c.id = :id")
    Optional<Comment> findByIdWithAccount(@Param("id") Long id);

    // 댓글 조회용: Account + 대댓글 Fetch Join
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.account " +
            "LEFT JOIN FETCH c.subComments sc " +
            "WHERE c.id = :id")
    Optional<Comment> findByIdWithAccountAndSubComments(@Param("id") Long id);
}
