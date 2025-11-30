package com.softwarecampus.backend.exception.user;

/**
 * 비밀번호 불일치 예외
 * - 현재 비밀번호 검증 실패 시 발생
 */
public class InvalidPasswordException extends RuntimeException {
    
    public InvalidPasswordException(String message) {
        super(message);
    }
}
