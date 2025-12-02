package com.softwarecampus.backend.scheduler;

import com.softwarecampus.backend.repository.board.BoardViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 조회 기록 정리 스케줄러
 * 30일 이상 된 조회 기록을 자동 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardViewCleanupScheduler {

    private final BoardViewRepository boardViewRepository;

    /**
     * 매일 새벽 3시에 30일 이상 된 조회 기록 삭제
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldViewRecords() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        boardViewRepository.deleteByViewedAtBefore(threshold);
        log.info("Cleaned up board view records older than {}", threshold);
    }
}
