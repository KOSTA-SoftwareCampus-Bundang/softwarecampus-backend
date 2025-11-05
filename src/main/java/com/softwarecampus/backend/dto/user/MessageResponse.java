package com.softwarecampus.backend.dto.user;

/**
 * 간단한 메시지 응답 DTO
 * HTTP 상태 코드로 성공/실패를 판단하므로 별도 status 필드 불필요
 * 
 * @param message 응답 메시지
 */
public record MessageResponse(String message) {
    
    /**
     * 정적 팩토리 메서드
     */
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
