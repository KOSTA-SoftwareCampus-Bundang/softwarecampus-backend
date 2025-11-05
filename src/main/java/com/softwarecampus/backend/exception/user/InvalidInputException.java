package com.softwarecampus.backend.exception.user;

/**
 * 잘못된 입력값 예외
 * - 필수 입력값 누락
 * - 형식이 올바르지 않은 입력값
 */
public class InvalidInputException extends RuntimeException {
    
    public InvalidInputException(String message) {
        super(message);
    }
    
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
