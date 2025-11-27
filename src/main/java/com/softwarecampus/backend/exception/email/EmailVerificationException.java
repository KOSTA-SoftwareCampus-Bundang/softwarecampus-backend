package com.softwarecampus.backend.exception.email;

/**
 * 이메일 인증 관련 예외
 */
public class EmailVerificationException extends RuntimeException {
    
    public EmailVerificationException(String message) {
        super(message);
    }
}
