package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 댓글 레포지토리
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 게시글별 댓글 목록 (최상위 댓글만)
    List<Comment> findByBoardAndParentCommentIsNullAndIsDeletedFalseOrderByCreatedAtAsc(Board board);
    
    // 대댓글 목록
    List<Comment> findByParentCommentAndIsDeletedFalseOrderByCreatedAtAsc(Comment parentComment);
}
