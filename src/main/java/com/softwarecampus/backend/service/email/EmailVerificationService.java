package com.softwarecampus.backend.service.email;

import com.softwarecampus.backend.model.dto.email.EmailVerificationCodeRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationResponse;
import com.softwarecampus.backend.model.enums.VerificationType;

/**
 * 이메일 인증 서비스
 */
public interface EmailVerificationService {
    
    /**
     * 인증 코드 발송 (회원가입/비밀번호 재설정)
     * 
     * @param request 이메일 및 인증 타입
     * @return 발송 결과 (만료 시간 포함)
     */
    EmailVerificationResponse sendVerificationCode(EmailVerificationRequest request);
    
    /**
     * 인증 코드 검증 (회원가입)
     * 
     * @param request 이메일 및 인증 코드
     * @return 검증 결과
     */
    EmailVerificationResponse verifyCode(EmailVerificationCodeRequest request);
    
    /**
     * 비밀번호 재설정 인증 코드 검증
     * 
     * @param request 이메일 및 인증 코드
     * @return 검증 결과
     */
    EmailVerificationResponse verifyResetCode(EmailVerificationCodeRequest request);
    
    /**
     * 이메일 인증 완료 여부 확인
     * 
     * @param email 확인할 이메일
     * @param type 인증 타입
     * @return 인증 완료 여부
     */
    boolean isEmailVerified(String email, VerificationType type);
}
