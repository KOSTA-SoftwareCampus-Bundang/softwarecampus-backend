package com.softwarecampus.backend;

import com.softwarecampus.backend.util.MockDataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mock 데이터 초기화 테스트
 * <p>
 * 주의: 이 테스트는 개발/테스트 환경에서만 실행해야 합니다.
 * 프로덕션 환경에서는 절대 실행하지 마세요!
 */
@SpringBootTest
@Transactional
@Rollback(false) // 데이터를 실제로 DB에 반영
public class MockDataInitializerTest {

    @Autowired
    private MockDataInitializer mockDataInitializer;

    @Test
    void initializeMockData() {
        mockDataInitializer.initialize();
    }
}
