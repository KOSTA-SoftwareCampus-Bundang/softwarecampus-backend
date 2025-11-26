package com.softwarecampus.backend.exception.user;

/**
 * 전화번호 중복 예외
 * 
 * HTTP Status: 409 Conflict
 */
public class PhoneNumberAlreadyExistsException extends RuntimeException {
    
    public PhoneNumberAlreadyExistsException(String phoneNumber) {
        super(String.format("이미 사용 중인 전화번호입니다: %s", phoneNumber));
    }
}
