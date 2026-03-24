package com.softwarecampus.backend.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Mock 데이터 전체 리셋 서비스
 * - 기존 데이터를 정리한 뒤 최신 시드 데이터를 재적재한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockDataResetService {

    private final DataSource dataSource;
    private final EntityManager entityManager;

    public void resetMockData(String triggerSource) {
        log.warn("========================================");
        log.warn("{} Mock 데이터 초기화를 시작합니다.", triggerSource);
        log.warn("기존 데이터는 모두 정리된 뒤 최신 시드 데이터로 재적재됩니다.");
        log.warn("========================================");

        executeSqlScript("sql/clean-database.sql");
        log.info("✅ 기존 데이터 정리 완료");

        executeSqlScript("sql/mock-data-new.sql");
        log.info("✅ Mock 데이터 적재 완료");

        entityManager.clear();

        log.warn("========================================");
        log.warn("{} Mock 데이터 초기화가 완료되었습니다.", triggerSource);
        log.warn("========================================");
    }

    private void executeSqlScript(String scriptPath) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(scriptPath));
        populator.setSeparator(";");
        populator.execute(dataSource);
    }
}
