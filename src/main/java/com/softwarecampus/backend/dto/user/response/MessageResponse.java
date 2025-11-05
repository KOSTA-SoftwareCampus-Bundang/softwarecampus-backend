package com.softwarecampus.backend.dto.user.response;

/**
 * 간단한 메시지 응답 DTO
 * 
 * @param message 응답 메시지
 */
public record MessageResponse(
    String message
) {
    /**
     * 정적 팩토리 메서드 - 성공 메시지
     */
    public static MessageResponse success(String message) {
        return new MessageResponse(message);
    }
    
    /**
     * 정적 팩토리 메서드 - 에러 메시지
     */
    public static MessageResponse error(String message) {
        return new MessageResponse(message);
    }
}
