package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.BoardView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BoardViewRepository extends JpaRepository<BoardView, Long> {

    /**
     * 로그인 사용자: 오늘 해당 게시글 조회 기록 존재 여부
     */
    @Query("SELECT COUNT(bv) > 0 FROM BoardView bv " +
            "WHERE bv.board.id = :boardId " +
            "AND bv.account.id = :accountId " +
            "AND DATE(bv.viewedAt) = CURRENT_DATE")
    boolean existsTodayViewByAccount(
            @Param("boardId") Long boardId,
            @Param("accountId") Long accountId);

    /**
     * 비로그인 사용자: 오늘 해당 게시글 IP 기준 조회 기록 존재 여부
     */
    @Query("SELECT COUNT(bv) > 0 FROM BoardView bv " +
            "WHERE bv.board.id = :boardId " +
            "AND bv.ipAddress = :ipAddress " +
            "AND DATE(bv.viewedAt) = CURRENT_DATE")
    boolean existsTodayViewByIp(
            @Param("boardId") Long boardId,
            @Param("ipAddress") String ipAddress);

    /**
     * 오래된 조회 기록 삭제 (스케줄러용)
     */
    @Modifying
    @Query("DELETE FROM BoardView bv WHERE bv.viewedAt < :threshold")
    void deleteByViewedAtBefore(@Param("threshold") LocalDateTime threshold);
}
