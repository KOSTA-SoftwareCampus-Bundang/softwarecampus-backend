package com.softwarecampus.backend.exception.email;

import java.time.LocalDateTime;

/**
 * 인증 시도 횟수 초과 예외
 */
public class TooManyAttemptsException extends RuntimeException {
    
    private final LocalDateTime blockedUntil;
    
    public TooManyAttemptsException(String message, LocalDateTime blockedUntil) {
        super(message);
        this.blockedUntil = blockedUntil;
    }
    
    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }
}
