package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.BoardRecommend;
import com.softwarecampus.backend.domain.board.Comment;
import com.softwarecampus.backend.domain.board.CommentRecommend;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRecommendRepository extends JpaRepository<CommentRecommend,Long> {
}
