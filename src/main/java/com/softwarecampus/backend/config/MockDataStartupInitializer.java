package com.softwarecampus.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 기동 시 Mock 데이터를 항상 초기화한다.
 * 주의: 현재 요구사항에 따라 기존 데이터를 정리한 뒤 다시 적재한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MockDataStartupInitializer implements ApplicationRunner {

    private final MockDataResetService mockDataResetService;

    @Override
    public void run(ApplicationArguments args) {
        mockDataResetService.resetMockData("기동 시");
    }
}
