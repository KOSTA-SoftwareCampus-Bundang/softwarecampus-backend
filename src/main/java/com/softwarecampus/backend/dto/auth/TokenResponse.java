package com.softwarecampus.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 응답 DTO
 * Access Token + Refresh Token 패턴
 * 
 * @since 2025-11-19 (Phase 12.5)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    
    /**
     * Access Token (15분 유효)
     * 모든 API 요청에 사용
     */
    private String accessToken;
    
    /**
     * Refresh Token (7일 유효)
     * Access Token 갱신에만 사용
     */
    private String refreshToken;
    
    /**
     * Access Token 만료 시간 (초)
     */
    private Long expiresIn;
    
    /**
     * 토큰 타입 (항상 "Bearer")
     */
    private String tokenType;
}
