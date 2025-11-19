package com.softwarecampus.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트
 * Phase 13: JWT 토큰 생성/검증 로직 테스트
 * 
 * 테스트 항목:
 * 1. 토큰 생성 (email만)
 * 2. 토큰 생성 (email + role)
 * 3. 토큰에서 email 추출
 * 4. 토큰에서 role 추출
 * 5. 토큰 유효성 검증
 * 6. 만료된 토큰 검증 실패
 * 7. 잘못된 서명 토큰 검증 실패
 * 8. 만료 시간 설정 확인
 * 
 * @author Phase 13
 * @since 2025-11-19
 */
@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {
    
    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    
    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-must-be-at-least-256-bits-long";
    private static final long TEST_EXPIRATION = 1000 * 60 * 30; // 30분
    
    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        ReflectionTestUtils.setField(jwtProperties, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtProperties, "expiration", TEST_EXPIRATION);
        
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }
    
    @Test
    @DisplayName("토큰 생성 - email만 포함")
    void generateToken_emailOnly() {
        // given
        String email = "test@example.com";
        
        // when
        String token = jwtTokenProvider.generateToken(email);
        
        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 3개 파트로 구성 (header.payload.signature)
    }
    
    @Test
    @DisplayName("토큰 생성 - email + role 포함")
    void generateToken_withRole() {
        // given
        String email = "user@example.com";
        String role = "USER";
        
        // when
        String token = jwtTokenProvider.generateToken(email, role);
        
        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // 토큰에서 role 추출 값도 확인
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);
        assertThat(extractedRole).isEqualTo(role);
    }
    
    @Test
    @DisplayName("토큰에서 email 추출")
    void getEmailFromToken() {
        // given
        String email = "test@example.com";
        String token = jwtTokenProvider.generateToken(email);
        
        // when
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
        
        // then
        assertThat(extractedEmail).isEqualTo(email);
    }
    
    @Test
    @DisplayName("토큰에서 role 추출")
    void getRoleFromToken() {
        // given
        String email = "admin@example.com";
        String role = "ADMIN";
        String token = jwtTokenProvider.generateToken(email, role);
        
        // when
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);
        
        // then
        assertThat(extractedRole).isEqualTo(role);
    }
    
    @Test
    @DisplayName("토큰에서 role 추출 - role 없는 토큰")
    void getRoleFromToken_noRole() {
        // given
        String email = "user@example.com";
        String token = jwtTokenProvider.generateToken(email); // role 없이 생성
        
        // when
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);
        
        // then
        assertThat(extractedRole).isNull(); // role이 없으면 null 반환
    }
    
    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_valid() {
        // given
        String email = "test@example.com";
        String token = jwtTokenProvider.generateToken(email);
        
        // when
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // then
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void validateToken_expired() throws InterruptedException {
        // given - 만료 시간 1ms로 설정
        JwtProperties shortExpirationProperties = new JwtProperties();
        ReflectionTestUtils.setField(shortExpirationProperties, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(shortExpirationProperties, "expiration", 1L); // 1ms
        
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortExpirationProperties);
        String token = shortProvider.generateToken("test@example.com");
        
        // when - 토큰 만료 대기
        Thread.sleep(10);
        boolean isValid = shortProvider.validateToken(token);
        
        // then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("잘못된 서명 토큰 검증 실패")
    void validateToken_invalidSignature() {
        // given - 다른 secret으로 생성한 토큰
        JwtProperties differentProperties = new JwtProperties();
        ReflectionTestUtils.setField(differentProperties, "secret", 
            "different-secret-key-for-testing-invalid-signature-must-be-long-enough");
        ReflectionTestUtils.setField(differentProperties, "expiration", TEST_EXPIRATION);
        
        JwtTokenProvider differentProvider = new JwtTokenProvider(differentProperties);
        String token = differentProvider.generateToken("test@example.com");
        
        // when - 다른 provider로 검증
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("잘못된 형식 토큰 검증 실패")
    void validateToken_malformed() {
        // given
        String malformedToken = "this.is.not.a.valid.jwt.token";
        
        // when
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);
        
        // then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("빈 토큰 검증 실패")
    void validateToken_empty() {
        // given
        String emptyToken = "";
        
        // when
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);
        
        // then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("만료 시간 설정 확인")
    void getExpiration() {
        // when
        long expiration = jwtTokenProvider.getExpiration();
        
        // then
        assertThat(expiration).isEqualTo(TEST_EXPIRATION);
    }
    
    @Test
    @DisplayName("생성된 토큰의 Claims 검증")
    void verifyTokenClaims() {
        // given
        String email = "user@example.com";
        String role = "USER";
        String token = jwtTokenProvider.generateToken(email, role);
        
        // when - 직접 토큰 파싱하여 Claims 확인
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        // then
        assertThat(claims.getSubject()).isEqualTo(email);
        assertThat(claims.get("role", String.class)).isEqualTo(role);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().getTime())
            .isGreaterThan(claims.getIssuedAt().getTime());
    }
    
    @Test
    @DisplayName("토큰 만료 시간 정확성 검증")
    void verifyTokenExpirationTime() {
        // given
        String token = jwtTokenProvider.generateToken("test@example.com");
        
        // when
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        long expirationTime = claims.getExpiration().getTime();
        long issuedTime = claims.getIssuedAt().getTime();
        
        // then - JWT는 초 단위로 저장하므로 밀리초는 잘림
        // 정확히 TEST_EXPIRATION이 아닌 TEST_EXPIRATION ± 1000ms 범위 내
        long actualDuration = expirationTime - issuedTime;
        assertThat(actualDuration).isBetween(TEST_EXPIRATION - 1000, TEST_EXPIRATION + 1000);
        assertThat(issuedTime).isLessThanOrEqualTo(System.currentTimeMillis());
    }
}
