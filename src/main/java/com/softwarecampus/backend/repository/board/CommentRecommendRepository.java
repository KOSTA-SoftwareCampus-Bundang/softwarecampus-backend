package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.BoardRecommend;
import com.softwarecampus.backend.domain.board.Comment;
import com.softwarecampus.backend.domain.board.CommentRecommend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRecommendRepository extends JpaRepository<CommentRecommend, Long> {

    @Query("SELECT cr from CommentRecommend cr join cr.comment join cr.account where cr.comment.id=:boardId and cr.account.id=:userId")
    CommentRecommend findByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

}
