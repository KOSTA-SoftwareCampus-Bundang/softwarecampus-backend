package com.softwarecampus.backend.dto.user;

/**
 * 로그인 응답 DTO
 * 
 * @param accessToken JWT Access Token (3분 유효)
 * @param refreshToken JWT Refresh Token (7일 유효)
 * @param tokenType 토큰 타입 (항상 "Bearer")
 * @param expiresIn Access Token 만료 시간 (초 단위, 180 = 3분)
 * @param account 사용자 계정 정보
 * 
 * @author 태윤
 */
public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    AccountResponse account
) {
    /**
     * 정적 팩토리 메서드: 로그인 성공 응답 생성
     * 
     * @param accessToken JWT Access Token
     * @param refreshToken JWT Refresh Token
     * @param expiresIn Access Token 만료 시간 (초)
     * @param account 사용자 계정 정보
     * @return LoginResponse
     */
    public static LoginResponse of(
        String accessToken, 
        String refreshToken, 
        Long expiresIn, 
        AccountResponse account
    ) {
        return new LoginResponse(
            accessToken, 
            refreshToken, 
            "Bearer",  // 고정값
            expiresIn, 
            account
        );
    }
}
