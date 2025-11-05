package com.softwarecampus.backend.dto.user;

/**
 * 간단한 메시지 응답 DTO
 * 
 * @param status 응답 상태 (SUCCESS 또는 ERROR)
 * @param message 응답 메시지
 */
public record MessageResponse(
    Status status,
    String message
) {
    /**
     * 응답 상태 열거형
     */
    public enum Status {
        SUCCESS,
        ERROR
    }
    
    /**
     * 정적 팩토리 메서드 - 성공 메시지
     */
    public static MessageResponse success(String message) {
        return new MessageResponse(Status.SUCCESS, message);
    }
    
    /**
     * 정적 팩토리 메서드 - 에러 메시지
     */
    public static MessageResponse error(String message) {
        return new MessageResponse(Status.ERROR, message);
    }
}
