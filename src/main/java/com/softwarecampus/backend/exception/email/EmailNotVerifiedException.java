package com.softwarecampus.backend.exception.email;

/**
 * 이메일 미인증 예외
 */
public class EmailNotVerifiedException extends RuntimeException {
    
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
