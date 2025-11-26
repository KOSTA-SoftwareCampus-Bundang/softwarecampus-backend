package com.softwarecampus.backend.model.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이메일 인증 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationResponse {
    
    private String message;
    private Integer expiresIn; // 만료까지 남은 시간(초)
    private Integer remainingAttempts; // 남은 시도 횟수
    
    public static EmailVerificationResponse success(String message) {
        return EmailVerificationResponse.builder()
                .message(message)
                .build();
    }
    
    public static EmailVerificationResponse withExpiry(String message, int expiresIn) {
        return EmailVerificationResponse.builder()
                .message(message)
                .expiresIn(expiresIn)
                .build();
    }
    
    public static EmailVerificationResponse withAttempts(String message, int remainingAttempts) {
        return EmailVerificationResponse.builder()
                .message(message)
                .remainingAttempts(remainingAttempts)
                .build();
    }
}
