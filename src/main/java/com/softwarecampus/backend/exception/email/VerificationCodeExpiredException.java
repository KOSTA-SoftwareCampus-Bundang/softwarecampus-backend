package com.softwarecampus.backend.exception.email;

/**
 * 인증 코드 만료 예외
 */
public class VerificationCodeExpiredException extends RuntimeException {
    
    public VerificationCodeExpiredException(String message) {
        super(message);
    }
}
