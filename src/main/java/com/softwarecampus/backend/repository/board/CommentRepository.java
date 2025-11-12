package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

}
