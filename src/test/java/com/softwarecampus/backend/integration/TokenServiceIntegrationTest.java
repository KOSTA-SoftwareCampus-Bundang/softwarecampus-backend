package com.softwarecampus.backend.integration;

import com.softwarecampus.backend.dto.auth.TokenResponse;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import com.softwarecampus.backend.service.auth.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 13: TokenService + Redis 통합 테스트
 * 
 * 목표:
 * - TokenService와 Redis 저장소 통합 검증
 * - Refresh Token 저장/조회/삭제 E2E 테스트
 * - Access Token 갱신 플로우 검증
 * - Redis TTL 동작 확인
 * 
 * 테스트 시나리오:
 * 1. Access Token + Refresh Token 생성 및 Redis 저장
 * 2. Refresh Token으로 Access Token 갱신
 * 3. Refresh Token 삭제 (로그아웃)
 * 4. Refresh Token 존재 여부 확인
 * 5. Redis TTL 설정 확인
 * 6. 잘못된 Refresh Token으로 갱신 실패
 * 7. 동일 사용자의 토큰 덮어쓰기
 * 
 * @since 2025-11-19
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
@DisplayName("TokenService + Redis 통합 테스트")
class TokenServiceIntegrationTest {
    
    @Autowired(required = false)
    private TokenService tokenService;
    
    @Autowired(required = false)
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String TEST_EMAIL = "test@example.com";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    
    @BeforeEach
    void setUp() {
        // Redis 연결 확인
        if (redisTemplate != null) {
            // 테스트 전 기존 데이터 삭제
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + TEST_EMAIL);
        }
    }
    
    @Test
    @DisplayName("createTokens: Access Token + Refresh Token 생성 및 Redis 저장")
    void createTokens_SavesRefreshTokenToRedis() {
        // Redis가 없으면 테스트 스킵
        if (tokenService == null || redisTemplate == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // when
        TokenResponse response = tokenService.createTokens(TEST_EMAIL);
        
        // then - TokenResponse 검증
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getExpiresIn()).isPositive();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        
        // then - Redis 저장 확인
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + TEST_EMAIL);
        assertThat(storedToken).isNotNull();
        assertThat(storedToken).isEqualTo(response.getRefreshToken());
    }
    
    @Test
    @DisplayName("createTokens: Refresh Token TTL이 7일로 설정됨")
    void createTokens_SetsCorrectTTL() {
        if (tokenService == null || redisTemplate == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // when
        tokenService.createTokens(TEST_EMAIL);
        
        // then - TTL 확인 (7일 = 604800초)
        Long ttl = redisTemplate.getExpire(REFRESH_TOKEN_PREFIX + TEST_EMAIL, TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(604700L); // 약간의 오차 허용
        assertThat(ttl).isLessThanOrEqualTo(604800L);
    }
    
    @Test
    @DisplayName("refreshAccessToken: 유효한 Refresh Token으로 Access Token 갱신 성공")
    void refreshAccessToken_Success() {
        if (tokenService == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // given
        TokenResponse tokens = tokenService.createTokens(TEST_EMAIL);
        String originalAccessToken = tokens.getAccessToken();
        
        // when
        String newAccessToken = tokenService.refreshAccessToken(TEST_EMAIL, tokens.getRefreshToken());
        
        // then
        assertThat(newAccessToken).isNotNull();
        assertThat(newAccessToken).isNotEqualTo(originalAccessToken); // 새 토큰 생성됨
        
        // 새 토큰으로 이메일 추출 가능
        String email = jwtTokenProvider.getEmailFromToken(newAccessToken);
        assertThat(email).isEqualTo(TEST_EMAIL);
    }
    
    @Test
    @DisplayName("refreshAccessToken: 잘못된 Refresh Token으로 갱신 실패")
    void refreshAccessToken_InvalidToken_ThrowsException() {
        if (tokenService == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // given
        tokenService.createTokens(TEST_EMAIL);
        String wrongRefreshToken = "wrong-refresh-token";
        
        // when & then
        assertThatThrownBy(() -> tokenService.refreshAccessToken(TEST_EMAIL, wrongRefreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }
    
    @Test
    @DisplayName("refreshAccessToken: Redis에 없는 Refresh Token으로 갱신 실패")
    void refreshAccessToken_TokenNotInRedis_ThrowsException() {
        if (tokenService == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // given - Redis에 토큰 저장하지 않음
        String nonExistentToken = "non-existent-token";
        
        // when & then
        assertThatThrownBy(() -> tokenService.refreshAccessToken(TEST_EMAIL, nonExistentToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }
    
    @Test
    @DisplayName("revokeRefreshToken: Refresh Token 삭제 성공")
    void revokeRefreshToken_DeletesFromRedis() {
        if (tokenService == null || redisTemplate == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // given
        tokenService.createTokens(TEST_EMAIL);
        assertThat(redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + TEST_EMAIL)).isTrue();
        
        // when
        tokenService.revokeRefreshToken(TEST_EMAIL);
        
        // then
        assertThat(redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + TEST_EMAIL)).isFalse();
    }
    
    @Test
    @DisplayName("revokeRefreshToken: 존재하지 않는 토큰 삭제 시 예외 없이 종료")
    void revokeRefreshToken_NonExistentToken_NoException() {
        if (tokenService == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // when & then - 예외 발생하지 않음
        tokenService.revokeRefreshToken("nonexistent@example.com");
    }
    
    @Test
    @DisplayName("hasRefreshToken: Refresh Token 존재 여부 확인")
    void hasRefreshToken_ChecksRedis() {
        if (tokenService == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // given
        assertThat(tokenService.hasRefreshToken(TEST_EMAIL)).isFalse();
        
        // when
        tokenService.createTokens(TEST_EMAIL);
        
        // then
        assertThat(tokenService.hasRefreshToken(TEST_EMAIL)).isTrue();
    }
    
    @Test
    @DisplayName("동일 사용자의 토큰 재발급 시 이전 Refresh Token 덮어쓰기")
    void createTokens_OverwritesPreviousToken() {
        if (tokenService == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // given
        TokenResponse firstTokens = tokenService.createTokens(TEST_EMAIL);
        
        // when
        TokenResponse secondTokens = tokenService.createTokens(TEST_EMAIL);
        
        // then
        assertThat(secondTokens.getRefreshToken()).isNotEqualTo(firstTokens.getRefreshToken());
        
        // 이전 Refresh Token으로는 갱신 불가
        assertThatThrownBy(() -> 
            tokenService.refreshAccessToken(TEST_EMAIL, firstTokens.getRefreshToken()))
                .isInstanceOf(IllegalArgumentException.class);
        
        // 새 Refresh Token으로는 갱신 가능
        String newAccessToken = tokenService.refreshAccessToken(TEST_EMAIL, secondTokens.getRefreshToken());
        assertThat(newAccessToken).isNotNull();
    }
    
    @Test
    @DisplayName("전체 플로우: 토큰 생성 → 갱신 → 삭제")
    void fullTokenFlow_CreateRefreshRevoke() {
        if (tokenService == null) {
            System.out.println("⚠️ Redis not available, skipping test");
            return;
        }
        
        // Step 1: 토큰 생성
        TokenResponse tokens = tokenService.createTokens(TEST_EMAIL);
        assertThat(tokenService.hasRefreshToken(TEST_EMAIL)).isTrue();
        
        // Step 2: Access Token 갱신
        String newAccessToken = tokenService.refreshAccessToken(TEST_EMAIL, tokens.getRefreshToken());
        assertThat(newAccessToken).isNotNull();
        
        // Step 3: Refresh Token 삭제 (로그아웃)
        tokenService.revokeRefreshToken(TEST_EMAIL);
        assertThat(tokenService.hasRefreshToken(TEST_EMAIL)).isFalse();
        
        // Step 4: 삭제 후 갱신 불가
        assertThatThrownBy(() -> 
            tokenService.refreshAccessToken(TEST_EMAIL, tokens.getRefreshToken()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
