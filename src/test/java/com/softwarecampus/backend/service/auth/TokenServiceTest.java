package com.softwarecampus.backend.service.auth;

import com.softwarecampus.backend.dto.auth.TokenResponse;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TokenService 테스트
 * - Access Token + Refresh Token 발급 및 관리 검증
 * - Redis 저장소 동작 검증
 * 
 * Phase 13: JWT 보안 시스템 테스트 (Phase 12.5 검증)
 * 
 * @since 2025-01-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 테스트")
class TokenServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenService tokenService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000L; // 15분
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000L; // 7일

    @BeforeEach
    void setUp() {
        // RedisTemplate의 opsForValue() 메서드가 valueOperations를 반환하도록 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("createTokens: Access Token과 Refresh Token 생성 성공")
    void createTokens_Success() {
        // given
        when(jwtTokenProvider.generateToken(TEST_EMAIL)).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtTokenProvider.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

        // when
        TokenResponse response = tokenService.createTokens(TEST_EMAIL);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotEmpty();
        assertThat(response.getExpiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRATION / 1000); // 초 단위
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        // Redis에 Refresh Token 저장 확인
        verify(valueOperations, times(1)).set(
                eq("refresh:" + TEST_EMAIL),
                anyString(),
                eq(REFRESH_TOKEN_VALIDITY),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("createTokens: Refresh Token은 UUID 형식")
    void createTokens_RefreshTokenIsUUID() {
        // given
        when(jwtTokenProvider.generateToken(TEST_EMAIL)).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtTokenProvider.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

        // when
        TokenResponse response = tokenService.createTokens(TEST_EMAIL);

        // then
        String refreshToken = response.getRefreshToken();
        assertThat(refreshToken).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("createTokens: 동일 이메일로 여러 번 호출 시 다른 Refresh Token 생성")
    void createTokens_DifferentRefreshTokens() {
        // given
        when(jwtTokenProvider.generateToken(TEST_EMAIL)).thenReturn(TEST_ACCESS_TOKEN);
        when(jwtTokenProvider.getExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

        // when
        TokenResponse response1 = tokenService.createTokens(TEST_EMAIL);
        TokenResponse response2 = tokenService.createTokens(TEST_EMAIL);

        // then
        assertThat(response1.getRefreshToken()).isNotEqualTo(response2.getRefreshToken());
    }

    @Test
    @DisplayName("refreshAccessToken: 유효한 Refresh Token으로 Access Token 갱신 성공")
    void refreshAccessToken_Success() {
        // given
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        when(valueOperations.get("refresh:" + TEST_EMAIL)).thenReturn(refreshToken);
        when(jwtTokenProvider.generateToken(TEST_EMAIL)).thenReturn(newAccessToken);

        // when
        String result = tokenService.refreshAccessToken(TEST_EMAIL, refreshToken);

        // then
        assertThat(result).isEqualTo(newAccessToken);
        verify(valueOperations, times(1)).get("refresh:" + TEST_EMAIL);
        verify(jwtTokenProvider, times(1)).generateToken(TEST_EMAIL);
    }

    @Test
    @DisplayName("refreshAccessToken: 존재하지 않는 Refresh Token으로 갱신 실패")
    void refreshAccessToken_TokenNotFound() {
        // given
        String refreshToken = "invalid-refresh-token";
        when(valueOperations.get("refresh:" + TEST_EMAIL)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> tokenService.refreshAccessToken(TEST_EMAIL, refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(valueOperations, times(1)).get("refresh:" + TEST_EMAIL);
        verifyNoInteractions(jwtTokenProvider); // JwtTokenProvider는 호출하지 않음
    }

    @Test
    @DisplayName("refreshAccessToken: 불일치하는 Refresh Token으로 갱신 실패")
    void refreshAccessToken_TokenMismatch() {
        // given
        String storedToken = "stored-refresh-token";
        String providedToken = "different-refresh-token";
        when(valueOperations.get("refresh:" + TEST_EMAIL)).thenReturn(storedToken);

        // when & then
        assertThatThrownBy(() -> tokenService.refreshAccessToken(TEST_EMAIL, providedToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(valueOperations, times(1)).get("refresh:" + TEST_EMAIL);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("revokeRefreshToken: Refresh Token 삭제 성공")
    void revokeRefreshToken_Success() {
        // given
        when(redisTemplate.delete("refresh:" + TEST_EMAIL)).thenReturn(true);

        // when
        tokenService.revokeRefreshToken(TEST_EMAIL);

        // then
        verify(redisTemplate, times(1)).delete("refresh:" + TEST_EMAIL);
    }

    @Test
    @DisplayName("revokeRefreshToken: 존재하지 않는 Token 삭제 시도")
    void revokeRefreshToken_TokenNotFound() {
        // given
        when(redisTemplate.delete("refresh:" + TEST_EMAIL)).thenReturn(false);

        // when
        tokenService.revokeRefreshToken(TEST_EMAIL);

        // then - 예외 없이 정상 종료
        verify(redisTemplate, times(1)).delete("refresh:" + TEST_EMAIL);
    }

    @Test
    @DisplayName("hasRefreshToken: Refresh Token 존재 확인 - 있음")
    void hasRefreshToken_Exists() {
        // given
        when(redisTemplate.hasKey("refresh:" + TEST_EMAIL)).thenReturn(true);

        // when
        boolean result = tokenService.hasRefreshToken(TEST_EMAIL);

        // then
        assertThat(result).isTrue();
        verify(redisTemplate, times(1)).hasKey("refresh:" + TEST_EMAIL);
    }

    @Test
    @DisplayName("hasRefreshToken: Refresh Token 존재 확인 - 없음")
    void hasRefreshToken_NotExists() {
        // given
        when(redisTemplate.hasKey("refresh:" + TEST_EMAIL)).thenReturn(false);

        // when
        boolean result = tokenService.hasRefreshToken(TEST_EMAIL);

        // then
        assertThat(result).isFalse();
        verify(redisTemplate, times(1)).hasKey("refresh:" + TEST_EMAIL);
    }

    @Test
    @DisplayName("hasRefreshToken: Redis에서 null 반환 시 false")
    void hasRefreshToken_NullFromRedis() {
        // given
        when(redisTemplate.hasKey("refresh:" + TEST_EMAIL)).thenReturn(null);

        // when
        boolean result = tokenService.hasRefreshToken(TEST_EMAIL);

        // then
        assertThat(result).isFalse();
        verify(redisTemplate, times(1)).hasKey("refresh:" + TEST_EMAIL);
    }
}
