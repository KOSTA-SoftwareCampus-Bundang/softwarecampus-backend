package com.softwarecampus.backend.dto.common;

/**
 * 파일 업로드 응답 DTO
 * 
 * @param fileUrl 업로드된 파일의 S3 URL
 * @param message 응답 메시지
 */
public record FileUploadResponse(
    String fileUrl,
    String message
) {
    
    /**
     * 정적 팩토리 메서드
     */
    public static FileUploadResponse of(String fileUrl, String message) {
        return new FileUploadResponse(fileUrl, message);
    }
    
    /**
     * 성공 응답 생성
     */
    public static FileUploadResponse success(String fileUrl) {
        return new FileUploadResponse(fileUrl, "파일이 성공적으로 업로드되었습니다.");
    }
}
