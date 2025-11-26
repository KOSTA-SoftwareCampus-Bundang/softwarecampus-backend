package com.softwarecampus.backend.util.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VerificationCodeGenerator 단위 테스트
 */
@DisplayName("인증 코드 생성 유틸리티 테스트")
class VerificationCodeGeneratorTest {
    
    private final VerificationCodeGenerator generator = new VerificationCodeGenerator();
    
    @Test
    @DisplayName("생성된 코드는 6자리여야 한다")
    void generateCode_ShouldBe6Digits() {
        // when
        String code = generator.generateCode();
        
        // then
        assertThat(code).hasSize(6);
    }
    
    @Test
    @DisplayName("생성된 코드는 숫자만 포함해야 한다")
    void generateCode_ShouldContainOnlyDigits() {
        // when
        String code = generator.generateCode();
        
        // then
        assertThat(code).matches("^[0-9]{6}$");
    }
    
    @RepeatedTest(1000)
    @DisplayName("생성된 코드는 0 ~ 999999 범위여야 한다")
    void generateCode_ShouldBeInValidRange() {
        // when
        String code = generator.generateCode();
        int numericCode = Integer.parseInt(code);
        
        // then
        assertThat(numericCode).isBetween(0, 999999);
    }
    
    @Test
    @DisplayName("1000번 생성 시 중복이 거의 없어야 한다 (무작위성)")
    void generateCode_ShouldBeRandom() {
        // given
        Set<String> codes = new HashSet<>();
        
        // when
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generateCode());
        }
        
        // then - 최소 950개 이상은 고유해야 함 (95% 이상)
        assertThat(codes).hasSizeGreaterThan(950);
    }
    
    @Test
    @DisplayName("앞자리 0이 포함된 코드도 6자리여야 한다")
    void generateCode_WithLeadingZeros_ShouldBe6Digits() {
        // given - 앞자리 0인 코드가 나올 때까지 반복
        boolean hasLeadingZero = false;
        
        // when
        for (int i = 0; i < 10000; i++) {
            String code = generator.generateCode();
            if (code.startsWith("0")) {
                hasLeadingZero = true;
                assertThat(code).hasSize(6);
                break;
            }
        }
        
        // then - 10000번 중 최소 1번은 앞자리 0이 나와야 함
        assertThat(hasLeadingZero).isTrue();
    }
    
    @Test
    @DisplayName("유효한 형식의 코드는 검증을 통과해야 한다")
    void isValidFormat_WithValidCode_ShouldReturnTrue() {
        // given
        String validCode = "123456";
        
        // when
        boolean result = generator.isValidFormat(validCode);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("null 코드는 검증 실패해야 한다")
    void isValidFormat_WithNull_ShouldReturnFalse() {
        // when
        boolean result = generator.isValidFormat(null);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("6자리가 아닌 코드는 검증 실패해야 한다")
    void isValidFormat_WithInvalidLength_ShouldReturnFalse() {
        // given
        String shortCode = "12345";
        String longCode = "1234567";
        
        // when & then
        assertThat(generator.isValidFormat(shortCode)).isFalse();
        assertThat(generator.isValidFormat(longCode)).isFalse();
    }
    
    @Test
    @DisplayName("숫자가 아닌 문자가 포함된 코드는 검증 실패해야 한다")
    void isValidFormat_WithNonDigits_ShouldReturnFalse() {
        // given
        String codeWithLetters = "12A456";
        String codeWithSpecialChars = "123@56";
        
        // when & then
        assertThat(generator.isValidFormat(codeWithLetters)).isFalse();
        assertThat(generator.isValidFormat(codeWithSpecialChars)).isFalse();
    }
}
