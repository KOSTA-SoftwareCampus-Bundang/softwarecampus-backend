package com.softwarecampus.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * EmailUtils 단위 테스트
 * 
 * 테스트 대상:
 * - isValidFormat(String email): 이메일 형식 검증 (RFC 5322 준수)
 * - maskEmail(String email): 이메일 마스킹 (PII 보호)
 */
@DisplayName("EmailUtils 단위 테스트")
class EmailUtilsTest {
    
    // ========================================
    // 1. 이메일 형식 검증 - 성공 케이스
    // ========================================
    
    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "user.name@example.com",
        "user+tag@example.com",
        "user_name@example.com",
        "user123@example.com",
        "a@example.com",  // 로컬 파트 1글자
        "user@sub.example.com",  // 서브도메인
        "user@example.co.kr"  // 2단계 TLD
    })
    @DisplayName("정상 이메일 형식 - RFC 5322 준수")
    void isValidFormat_정상이메일(String email) {
        // When
        boolean result = EmailUtils.isValidFormat(email);
        
        // Then
        assertThat(result).isTrue();
    }
    
    // ========================================
    // 2. 이메일 형식 검증 - 실패 케이스
    // ========================================
    
    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email",          // @ 없음
        "@example.com",           // 로컬 파트 없음
        "user@",                  // 도메인 없음
        "user @example.com",      // 공백 포함
        "user@exam ple.com",      // 도메인 공백
        "user@@example.com",      // @ 중복
        "user@.example.com",      // 도메인 점 시작
        "user@example.com.",      // 도메인 점 끝
        "user@example..com",      // 도메인 연속 점
        "user@-example.com",      // 도메인 하이픈 시작 (RFC 1035 위반)
        "user@example-.com",      // 도메인 레이블 하이픈 끝
        "",                       // 빈 문자열
        "   "                     // 공백만
    })
    @DisplayName("잘못된 이메일 형식 - RFC 위반")
    void isValidFormat_잘못된형식(String email) {
        // When
        boolean result = EmailUtils.isValidFormat(email);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("null 이메일 - 형식 검증 실패")
    void isValidFormat_null() {
        // When
        boolean result = EmailUtils.isValidFormat(null);
        
        // Then
        assertThat(result).isFalse();
    }
    
    // ========================================
    // 3. 이메일 마스킹 - PII 보호
    // ========================================
    
    @ParameterizedTest
    @CsvSource({
        "user@example.com, u***@example.com",
        "a@example.com, a***@example.com",
        "ab@example.com, a***@example.com",
        "abc@example.com, a***@example.com",
        "abcd@example.com, a***@example.com",
        "longuser@example.com, lo***@example.com"
    })
    @DisplayName("이메일 마스킹 - 로컬 파트 길이별")
    void maskEmail_정상케이스(String original, String expected) {
        // When
        String masked = EmailUtils.maskEmail(original);
        
        // Then
        assertThat(masked).isEqualTo(expected);
    }
    
    @Test
    @DisplayName("null 이메일 - 마스킹 안전 처리")
    void maskEmail_null() {
        // When
        String masked = EmailUtils.maskEmail(null);
        
        // Then
        assertThat(masked).isEqualTo("***");
    }
    
    @Test
    @DisplayName("잘못된 형식 이메일 - 마스킹 안전 처리")
    void maskEmail_잘못된형식() {
        // When
        String masked = EmailUtils.maskEmail("invalid-email");
        
        // Then
        assertThat(masked).isEqualTo("***");
    }
    
    @Test
    @DisplayName("빈 문자열 이메일 - 마스킹 안전 처리")
    void maskEmail_빈문자열() {
        // When
        String masked = EmailUtils.maskEmail("");
        
        // Then
        assertThat(masked).isEqualTo("***");
    }
    
    // ========================================
    // 4. RFC 5322 경계 케이스 테스트
    // ========================================
    
    @Test
    @DisplayName("로컬 파트 최소 길이 - 1글자")
    void isValidFormat_로컬파트최소() {
        // When
        boolean result = EmailUtils.isValidFormat("a@example.com");
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("로컬 파트 최대 길이 - 64글자")
    void isValidFormat_로컬파트최대() {
        // Given
        String longLocal = "a".repeat(64) + "@example.com";
        
        // When
        boolean result = EmailUtils.isValidFormat(longLocal);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("로컬 파트 초과 - 65글자 (RFC 5321 위반)")
    void isValidFormat_로컬파트초과() {
        // Given
        String tooLongLocal = "a".repeat(65) + "@example.com";
        
        // When
        boolean result = EmailUtils.isValidFormat(tooLongLocal);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("도메인 레이블 하이픈 중간 - 허용")
    void isValidFormat_도메인하이픈중간() {
        // When
        boolean result = EmailUtils.isValidFormat("user@ex-ample.com");
        
        // Then
        assertThat(result).isTrue();
    }
}
