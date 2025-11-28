package com.softwarecampus.backend.service.user.email;

import com.softwarecampus.backend.domain.common.VerificationType;

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
    
    /**
     * 기관 승인 완료 이메일 발송
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     * 
     * @param toEmail 수신자 이메일 (기관 등록 시 입력한 이메일)
     * @param academyName 기관명
     */
    void sendAcademyApprovalEmail(String toEmail, String academyName);
    
    /**
     * 기관 거절 이메일 발송
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     * 
     * @param toEmail 수신자 이메일
     * @param academyName 기관명
     * @param reason 거절 사유
     */
    void sendAcademyRejectionEmail(String toEmail, String academyName, String reason);
    
    /**
     * 회원 승인 완료 이메일 발송
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     * 
     * @param toEmail 수신자 이메일 (회원가입 시 입력한 이메일)
     * @param userName 사용자명
     */
    void sendAccountApprovalEmail(String toEmail, String userName);
}
