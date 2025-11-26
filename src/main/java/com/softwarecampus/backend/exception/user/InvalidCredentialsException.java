package com.softwarecampus.backend.exception.user;

/**
 * 로그인 인증 실패 예외
 * 
 * 발생 시나리오:
 * - 존재하지 않는 이메일로 로그인 시도
 * - 비밀번호 불일치
 * - 비활성화된 계정 (isActive = false)
 * - 미승인 ACADEMY 계정 (accountApproved = PENDING/REJECTED)
 * 
 * HTTP 상태 코드: 401 Unauthorized
 * 
 * @author 태윤
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
