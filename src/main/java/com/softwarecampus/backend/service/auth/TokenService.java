package com.softwarecampus.backend.service.auth;

import com.softwarecampus.backend.dto.auth.TokenResponse;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 토큰 관리 서비스
 * Access Token + Refresh Token 발급 및 관리
 * 
 * @since 2025-11-19 (Phase 12.5)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = false)
public class TokenService {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000L; // 7일
    
    /**
     * Access Token + Refresh Token 생성
     * 
     * @param email 사용자 이메일
     * @return TokenResponse (accessToken, refreshToken, expiresIn, tokenType)
     */
    public TokenResponse createTokens(String email) {
        // 1. Access Token 생성 (JWT)
        String accessToken = jwtTokenProvider.generateToken(email);
        
        // 2. Refresh Token 생성 (UUID)
        String refreshToken = UUID.randomUUID().toString();
        
        // 3. Refresh Token을 Redis에 저장
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.opsForValue().set(
            key, 
            refreshToken, 
            REFRESH_TOKEN_VALIDITY, 
            TimeUnit.MILLISECONDS
        );
        
        log.debug("Tokens created for user: {}", email);
        
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getExpiration() / 1000) // 초 단위
                .tokenType("Bearer")
                .build();
    }
    
    /**
     * Refresh Token으로 Access Token 갱신
     * 
     * @param email 사용자 이메일
     * @param refreshToken Refresh Token
     * @return 새로운 Access Token
     * @throws IllegalArgumentException Refresh Token이 유효하지 않은 경우
     */
    public String refreshAccessToken(String email, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + email;
        String storedToken = redisTemplate.opsForValue().get(key);
        
        // Refresh Token 검증
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            log.warn("Invalid refresh token for user: {}", email);
            throw new IllegalArgumentException("Invalid refresh token");
        }
        
        // 새로운 Access Token 발급
        String newAccessToken = jwtTokenProvider.generateToken(email);
        log.debug("Access token refreshed for user: {}", email);
        
        return newAccessToken;
    }
    
    /**
     * Refresh Token 삭제 (로그아웃)
     * 
     * @param email 사용자 이메일
     */
    public void revokeRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Refresh token revoked for user: {}", email);
        }
    }
    
    /**
     * Refresh Token 존재 여부 확인
     * 
     * @param email 사용자 이메일
     * @return true: 존재, false: 없음
     */
    public boolean hasRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
