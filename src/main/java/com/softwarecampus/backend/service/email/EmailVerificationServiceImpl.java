package com.softwarecampus.backend.service.email;

import com.softwarecampus.backend.common.constants.EmailConstants;
import com.softwarecampus.backend.exception.email.EmailVerificationException;
import com.softwarecampus.backend.exception.email.TooManyAttemptsException;
import com.softwarecampus.backend.exception.email.VerificationCodeExpiredException;
import com.softwarecampus.backend.model.dto.email.EmailVerificationCodeRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationResponse;
import com.softwarecampus.backend.model.entity.EmailVerification;
import com.softwarecampus.backend.model.enums.VerificationType;
import com.softwarecampus.backend.repository.EmailVerificationRepository;
import com.softwarecampus.backend.util.email.VerificationCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 이메일 인증 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {
    
    private final EmailVerificationRepository verificationRepository;
    private final EmailSendService emailSendService;
    private final VerificationCodeGenerator codeGenerator;
    
    @Override
    @Transactional
    public EmailVerificationResponse sendVerificationCode(EmailVerificationRequest request) {
        String email = request.getEmail();
        VerificationType type = request.getType();
        
        // 1. 재발송 쿨다운 체크 (60초)
        checkResendCooldown(email, type);
        
        // 2. 차단 상태 체크
        checkBlockStatus(email, type);
        
        // 3. 인증 코드 생성
        String code = codeGenerator.generateCode();
        
        // 4. DB 저장
        EmailVerification verification = createVerification(email, code, type);
        verificationRepository.save(verification);
        
        // 5. 이메일 발송
        emailSendService.sendVerificationCode(email, code, type);
        
        log.info("인증 코드 발송 완료 - type: {}", type);
        
        return EmailVerificationResponse.withExpiry(
            "인증 코드가 발송되었습니다",
            EmailConstants.EXPIRY_SECONDS
        );
    }
    
    @Override
    @Transactional
    public EmailVerificationResponse verifyCode(EmailVerificationCodeRequest request) {
        return verifyCodeInternal(request, VerificationType.SIGNUP, "이메일 인증이 완료되었습니다");
    }
    
    @Override
    @Transactional
    public EmailVerificationResponse verifyResetCode(EmailVerificationCodeRequest request) {
        return verifyCodeInternal(request, VerificationType.PASSWORD_RESET, "인증이 완료되었습니다. 새 비밀번호를 설정하세요");
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isEmailVerified(String email, VerificationType type) {
        return verificationRepository.existsByEmailAndTypeAndVerifiedTrue(email, type);
    }
    
    /**
     * 인증 코드 검증 내부 로직 (회원가입/비밀번호 재설정 공통)
     */
    private EmailVerificationResponse verifyCodeInternal(
            EmailVerificationCodeRequest request, 
            VerificationType type, 
            String successMessage
    ) {
        String email = request.getEmail();
        String code = request.getCode();
        
        // 1. 최근 인증 레코드 조회
        EmailVerification verification = verificationRepository
            .findTopByEmailAndTypeOrderByCreatedAtDesc(email, type)
            .orElseThrow(() -> new EmailVerificationException("인증 요청 기록이 없습니다"));
        
        // 2. 차단 상태 체크 (만료 시 자동 해제)
        if (verification.isStillBlocked()) {
            throw new TooManyAttemptsException(
                "인증 시도 횟수를 초과했습니다. " + verification.getBlockedUntil() + "까지 차단됩니다",
                verification.getBlockedUntil()
            );
        }
        
        // 3. 만료 체크
        if (verification.isExpired()) {
            throw new VerificationCodeExpiredException("인증 코드가 만료되었습니다. 새로운 코드를 요청하세요");
        }
        
        // 4. 이미 인증 완료된 경우
        if (verification.getVerified()) {
            return EmailVerificationResponse.success("이미 인증이 완료되었습니다");
        }
        
        // 5. 코드 일치 여부 확인
        if (!verification.getCode().equals(code)) {
            verification.incrementAttempts();
            
            // 5회 실패 시 차단
            if (verification.getAttempts() >= EmailConstants.MAX_ATTEMPTS) {
                verification.block(EmailConstants.BLOCK_DURATION_MINUTES);
                verificationRepository.save(verification);
                
                throw new TooManyAttemptsException(
                    "인증 시도 횟수를 초과했습니다. 30분간 차단됩니다",
                    verification.getBlockedUntil()
                );
            }
            
            verificationRepository.save(verification);
            int remaining = EmailConstants.MAX_ATTEMPTS - verification.getAttempts();
            
            return EmailVerificationResponse.withAttempts(
                "인증 코드가 일치하지 않습니다",
                remaining
            );
        }
        
        // 6. 인증 성공
        verification.markAsVerified();
        verificationRepository.save(verification);
        
        log.info("이메일 인증 성공 - type: {}", type);
        
        return EmailVerificationResponse.success(successMessage);
    }
    
    /**
     * 재발송 쿨다운 체크 (60초)
     */
    private void checkResendCooldown(String email, VerificationType type) {
        Optional<EmailVerification> recent = verificationRepository
            .findTopByEmailAndTypeOrderByCreatedAtDesc(email, type);
        
        if (recent.isPresent()) {
            LocalDateTime lastSent = recent.get().getCreatedAt();
            long secondsSinceLastSent = Duration.between(lastSent, LocalDateTime.now()).getSeconds();
            
            if (secondsSinceLastSent < EmailConstants.RESEND_COOLDOWN_SECONDS) {
                long remainingSeconds = EmailConstants.RESEND_COOLDOWN_SECONDS - secondsSinceLastSent;
                throw new EmailVerificationException(
                    String.format("인증 코드는 %d초 후에 재발송할 수 있습니다", remainingSeconds)
                );
            }
        }
    }
    
    /**
     * 차단 상태 체크
     */
    private void checkBlockStatus(String email, VerificationType type) {
        Optional<EmailVerification> recent = verificationRepository
            .findTopByEmailAndTypeOrderByCreatedAtDesc(email, type);
        
        if (recent.isPresent() && recent.get().isStillBlocked()) {
            throw new TooManyAttemptsException(
                "인증 시도 횟수를 초과하여 차단되었습니다",
                recent.get().getBlockedUntil()
            );
        }
    }
    
    /**
     * EmailVerification 엔티티 생성
     */
    private EmailVerification createVerification(String email, String code, VerificationType type) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(EmailConstants.EXPIRY_MINUTES);
        
        return EmailVerification.builder()
            .email(email)
            .code(code)
            .type(type)
            .verified(false)
            .attempts(0)
            .blocked(false)
            .expiresAt(expiresAt)
            .build();
    }
}
