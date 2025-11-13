package com.softwarecampus.backend.exception.user;

/**
 * 계정 미존재 예외
 * - ID 또는 이메일로 계정 조회 시 존재하지 않을 때 발생
 */
public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException(String message) {
        super(message);
    }
    
    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
