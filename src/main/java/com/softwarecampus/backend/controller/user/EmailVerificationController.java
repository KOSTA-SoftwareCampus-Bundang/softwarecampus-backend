package com.softwarecampus.backend.controller.user;

import com.softwarecampus.backend.dto.user.EmailVerificationCodeRequest;
import com.softwarecampus.backend.dto.user.EmailVerificationRequest;
import com.softwarecampus.backend.dto.user.EmailVerificationResponse;
import com.softwarecampus.backend.domain.common.VerificationType;
import com.softwarecampus.backend.service.user.email.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
            @Valid @RequestBody EmailVerificationRequest request) {
        log.info("회원가입 인증 코드 발송 요청 - type: {}", VerificationType.SIGNUP);

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
            @Valid @RequestBody EmailVerificationCodeRequest request) {
        log.info("회원가입 인증 코드 검증 요청 - type: {}", VerificationType.SIGNUP);

        EmailVerificationResponse response = verificationService.verifyCode(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. 비밀번호 재설정 인증 코드 발송
     * POST /api/auth/email/send-reset-code
     */
    @PostMapping("/send-reset-code")
    public ResponseEntity<EmailVerificationResponse> sendPasswordResetCode(
            @Valid @RequestBody EmailVerificationRequest request) {
        log.info("비밀번호 재설정 인증 코드 발송 요청 - type: {}", VerificationType.PASSWORD_RESET);

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
            @Valid @RequestBody EmailVerificationCodeRequest request) {
        log.info("비밀번호 재설정 인증 코드 검증 요청 - type: {}", VerificationType.PASSWORD_RESET);

        EmailVerificationResponse response = verificationService.verifyResetCode(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. 비밀번호 변경 인증 코드 발송 (로그인 사용자 전용)
     * POST /api/auth/email/send-change-code
     * 
     * 용도: 마이페이지 비밀번호 변경 Step 2
     * - JWT에서 이메일 추출
     * - PASSWORD_CHANGE 타입으로 인증 코드 발송
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/send-change-code")
    public ResponseEntity<EmailVerificationResponse> sendPasswordChangeCode(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        log.info("비밀번호 변경 인증 코드 발송 요청 - type: {}", VerificationType.PASSWORD_CHANGE);

        EmailVerificationRequest request = new EmailVerificationRequest();
        request.setEmail(email);
        request.setType(VerificationType.PASSWORD_CHANGE);

        EmailVerificationResponse response = verificationService.sendVerificationCode(request);
        return ResponseEntity.ok(response);
    }
}
