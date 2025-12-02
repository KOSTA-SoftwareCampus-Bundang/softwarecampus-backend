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

    // 내가 쓴 댓글 수 (삭제되지 않은 게시글의 댓글만 카운트)
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.account.id = :accountId AND c.isDeleted = false AND c.board.isDeleted = false")
    Long countByAccountId(@Param("accountId") Long accountId);

    // 권한 체크용: Account만 Fetch Join (삭제되지 않은 댓글과 계정만 조회)
    @Query("SELECT c FROM Comment c JOIN FETCH c.account a WHERE c.id = :id AND c.isDeleted = false AND a.isDeleted = false")
    Optional<Comment> findByIdWithAccount(@Param("id") Long id);

    // 댓글 조회용: Account + 대댓글 Fetch Join
    // 주의: LEFT JOIN FETCH로 조회된 subComments 중 삭제된 것은 서비스 레이어에서 필터링 필요
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.account a " +
            "LEFT JOIN FETCH c.subComments sc " +
            "WHERE c.id = :id AND c.isDeleted = false AND a.isDeleted = false")
    Optional<Comment> findByIdWithAccountAndSubComments(@Param("id") Long id);
}
