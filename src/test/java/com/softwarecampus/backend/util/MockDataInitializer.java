package com.softwarecampus.backend.util;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * Mock 데이터 초기화 유틸리티
 * <p>
 * 주의: 이 클래스는 개발/테스트 환경에서만 사용해야 합니다.
 * 프로덕션 환경에서는 절대 실행하지 마세요!
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataInitializer {

    private final DataSource dataSource;
    private final EntityManager entityManager;

    /**
     * Mock 데이터 초기화 실행
     * <p>
     * 실행 순서:
     * 1. clean-database.sql: 모든 테이블 데이터 삭제
     * 2. mock-data.sql: Mock 데이터 삽입
     */
    @Transactional
    public void initialize() {
        log.info("========================================");
        log.info("Mock 데이터 초기화 시작");
        log.info("========================================");

        try {
            // 1. 기존 데이터 정리
            executeSqlScript("sql/clean-database.sql");
            log.info("✅ 기존 데이터 정리 완료");

            // 2. Mock 데이터 삽입
            executeSqlScript("sql/mock-data.sql");
            log.info("✅ Mock 데이터 삽입 완료");

            // EntityManager 초기화 (캐시 제거)
            entityManager.flush();
            entityManager.clear();

            printSummary();

            log.info("========================================");
            log.info("Mock 데이터 초기화 성공!");
            log.info("========================================");

        } catch (Exception e) {
            log.error("Mock 데이터 초기화 실패", e);
            throw new RuntimeException("Mock 데이터 초기화 중 오류 발생", e);
        }
    }

    /**
     * SQL 스크립트 실행
     */
    private void executeSqlScript(String scriptPath) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(scriptPath));
        populator.setSeparator(";");
        populator.execute(dataSource);
    }

    /**
     * 생성된 데이터 요약 출력
     */
    private void printSummary() {
        log.info("📊 생성된 데이터 요약:");
        log.info("  - Academy: 3개");
        log.info("  - CourseCategory: 24개");
        log.info("  - Account: 6개");
        log.info("  - Course: 12개");
        log.info("  - Board: 10개");
        log.info("  - Comment: 8개");
        log.info("  - CourseReview: 3개");
        log.info("  - ReviewSection: 12개");
        log.info("  - CourseFavorite: 8개");
        log.info("  - CourseCurriculum: 52개");
        log.info("  - CourseQna: 12개");
        log.info("  - ReviewLike: 10개");
    }
}

