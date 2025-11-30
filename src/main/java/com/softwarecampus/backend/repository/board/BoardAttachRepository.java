package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.BoardAttach;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardAttachRepository extends JpaRepository<BoardAttach, Long> {

    // 스케줄러용: 삭제된 지 일정 기간이 지난 파일 조회
    java.util.List<BoardAttach> findByIsDeletedTrueAndDeletedAtBefore(java.time.LocalDateTime threshold);
}
