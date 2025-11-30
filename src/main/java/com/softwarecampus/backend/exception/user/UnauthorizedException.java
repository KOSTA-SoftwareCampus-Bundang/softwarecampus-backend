package com.softwarecampus.backend.exception.user;

/**
 * 인증되지 않은 사용자의 접근 예외
 * 
 * 발생 시나리오:
 * - 로그인하지 않은 사용자가 인증 필요 API 호출
 * - JWT 토큰 누락 또는 만료
 * - 인증 정보가 null인 경우
 * 
 * HTTP 상태 코드: 401 Unauthorized
 * 
 * @author Jake Park
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
}
