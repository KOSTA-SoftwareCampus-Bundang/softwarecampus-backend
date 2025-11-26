package com.softwarecampus.backend.controller;

import com.softwarecampus.backend.model.dto.email.EmailVerificationCodeRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationResponse;
import com.softwarecampus.backend.model.enums.VerificationType;
import com.softwarecampus.backend.service.email.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 이메일 인증 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {
    
    private final EmailVerificationService verificationService;
    
    /**
     * 1. 회원가입 인증 코드 발송
     * POST /api/auth/email/send-verification
     */
    @PostMapping("/send-verification")
    public ResponseEntity<EmailVerificationResponse> sendSignupVerification(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        log.info("회원가입 인증 코드 발송 요청");
        
        // 강제로 SIGNUP 타입 설정
        request.setType(VerificationType.SIGNUP);
        
        EmailVerificationResponse response = verificationService.sendVerificationCode(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 2. 회원가입 인증 코드 검증
     * POST /api/auth/email/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<EmailVerificationResponse> verifySignupCode(
            @Valid @RequestBody EmailVerificationCodeRequest request
    ) {
        log.info("회원가입 인증 코드 검증 요청");
        
        EmailVerificationResponse response = verificationService.verifyCode(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 3. 비밀번호 재설정 인증 코드 발송
     * POST /api/auth/email/send-reset-code
     */
    @PostMapping("/send-reset-code")
    public ResponseEntity<EmailVerificationResponse> sendPasswordResetCode(
        @Valid @RequestBody EmailVerificationRequest request
    ) {
        log.info("비밀번호 재설정 인증 코드 발송 요청");
        
        // 강제로 PASSWORD_RESET 타입 설정
        request.setType(VerificationType.PASSWORD_RESET);
        
        EmailVerificationResponse response = verificationService.sendVerificationCode(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 4. 비밀번호 재설정 인증 코드 검증
     * POST /api/auth/email/verify-reset
     */
    @PostMapping("/verify-reset")
    public ResponseEntity<EmailVerificationResponse> verifyPasswordResetCode(
            @Valid @RequestBody EmailVerificationCodeRequest request
    ) {
        log.info("비밀번호 재설정 인증 코드 검증 요청");
        
        EmailVerificationResponse response = verificationService.verifyResetCode(request);
        return ResponseEntity.ok(response);
    }
}
