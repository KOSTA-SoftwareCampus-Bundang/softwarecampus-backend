package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.BoardAttach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 게시글 첨부파일 레포지토리
 */
@Repository
public interface BoardAttachRepository extends JpaRepository<BoardAttach, Long> {
}
