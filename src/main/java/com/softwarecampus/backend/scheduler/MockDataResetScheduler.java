package com.softwarecampus.backend.scheduler;

import com.softwarecampus.backend.config.MockDataResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 쇼케이스용 Mock 데이터 자동 리셋 스케줄러
 * - 기본값: 매일 자정(00:00)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataResetScheduler {

    private final MockDataResetService mockDataResetService;

    @Scheduled(cron = "${app.mock-data.reset.cron:0 0 0 * * ?}")
    public void resetMockDataDaily() {
        log.info("매일 자정 Mock 데이터 리셋 스케줄러를 실행합니다.");
        mockDataResetService.resetMockData("스케줄러");
    }
}
