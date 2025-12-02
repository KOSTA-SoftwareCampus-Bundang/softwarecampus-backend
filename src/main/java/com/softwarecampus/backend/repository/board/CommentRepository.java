package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

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
