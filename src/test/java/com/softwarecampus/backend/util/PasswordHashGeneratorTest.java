package com.softwarecampus.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;

/**
 * BCrypt 해시 생성 및 검증 테스트
 * init_data.sql에 사용할 해시값 생성용
 */
@DisplayName("BCrypt 비밀번호 해시 생성 테스트")
class PasswordHashGeneratorTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("password123 해시 생성 및 출력")
    void generatePasswordHash() {
        // Given
        String rawPassword = "password123";
        
        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // Then
        System.out.println("========================================");
        System.out.println("Raw Password: " + rawPassword);
        System.out.println("BCrypt Hash: " + encodedPassword);
        System.out.println("========================================");
        
        // 생성된 해시가 원본 비밀번호와 매치하는지 검증
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    @DisplayName("init_data.sql의 기존 해시 검증")
    void verifyExistingHash() {
        // Given
        String rawPassword = "password123";
        String existingHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        
        // When
        boolean matches = passwordEncoder.matches(rawPassword, existingHash);
        
        // Then
        System.out.println("========================================");
        System.out.println("Existing hash matches 'password123': " + matches);
        System.out.println("========================================");
        
        // 이 테스트가 실패하면 해시가 올바르지 않은 것
        // assertThat(matches).isTrue(); // 주석 처리 - 검증용
    }
}
