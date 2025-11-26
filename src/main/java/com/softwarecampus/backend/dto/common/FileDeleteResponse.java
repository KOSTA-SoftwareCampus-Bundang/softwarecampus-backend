package com.softwarecampus.backend.dto.common;

/**
 * 파일 삭제 응답 DTO
 * 
 * @param message 응답 메시지
 */
public record FileDeleteResponse(String message) {
    
    /**
     * 정적 팩토리 메서드
     */
    public static FileDeleteResponse of(String message) {
        return new FileDeleteResponse(message);
    }
    
    /**
     * 성공 응답 생성
     */
    public static FileDeleteResponse success() {
        return new FileDeleteResponse("파일이 성공적으로 삭제되었습니다.");
    }
}
