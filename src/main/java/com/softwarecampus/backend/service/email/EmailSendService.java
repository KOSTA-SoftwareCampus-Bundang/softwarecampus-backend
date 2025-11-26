package com.softwarecampus.backend.service.email;

import com.softwarecampus.backend.model.enums.VerificationType;

/**
 * 이메일 발송 서비스
 */
public interface EmailSendService {
    
    /**
     * 인증 코드 이메일 발송
     * 
     * @param to 수신자 이메일
     * @param code 6자리 인증 코드
     * @param type 인증 타입 (SIGNUP, PASSWORD_RESET)
     */
    void sendVerificationCode(String to, String code, VerificationType type);
}
