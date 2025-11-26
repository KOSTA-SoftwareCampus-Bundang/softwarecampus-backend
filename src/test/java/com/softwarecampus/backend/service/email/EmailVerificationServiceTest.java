package com.softwarecampus.backend.service.email;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * EmailVerificationService 단위 테스트 (Mock)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("이메일 인증 Service 테스트")
class EmailVerificationServiceTest {
    
    @Mock
    private EmailVerificationRepository repository;
    
    @Mock
    private EmailSendService emailSendService;
    
    @Mock
    private VerificationCodeGenerator codeGenerator;
    
    @InjectMocks
    private EmailVerificationServiceImpl verificationService;
    
    private String testEmail;
    private String testCode;
    
    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testCode = "123456";
    }
    
    @Test
    @DisplayName("인증 코드 발송 성공")
    void sendVerificationCode_ShouldSendEmailAndSaveRecord() {
        // given
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .email(testEmail)
                .type(VerificationType.SIGNUP)
                .build();
        
        when(repository.findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP))
                .thenReturn(Optional.empty());
        when(codeGenerator.generateCode()).thenReturn(testCode);
        when(repository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // when
        EmailVerificationResponse response = verificationService.sendVerificationCode(request);
        
        // then
        assertThat(response.getMessage()).contains("발송");
        assertThat(response.getExpiresIn()).isEqualTo(180);
        verify(emailSendService).sendVerificationCode(eq(testEmail), eq(testCode), eq(VerificationType.SIGNUP));
        verify(repository).save(any(EmailVerification.class));
    }
    
    @Test
    @DisplayName("재발송 쿨다운 60초 이내 시도 시 예외 발생")
    void sendVerificationCode_WithinCooldown_ShouldThrowException() {
        // given
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .email(testEmail)
                .type(VerificationType.SIGNUP)
                .build();
        
        EmailVerification recent = EmailVerification.builder()
                .email(testEmail)
                .code(testCode)
                .type(VerificationType.SIGNUP)
                .createdAt(LocalDateTime.now().minusSeconds(30)) // 30초 전
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .verified(false)
                .attempts(0)
                .blocked(false)
                .build();
        
        when(repository.findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP))
                .thenReturn(Optional.of(recent));
        
        // when & then
        assertThatThrownBy(() -> verificationService.sendVerificationCode(request))
                .isInstanceOf(EmailVerificationException.class)
                .hasMessageContaining("초 후에 재발송");
    }
    
    @Test
    @DisplayName("차단된 상태에서 발송 시도 시 예외 발생")
    void sendVerificationCode_WhenBlocked_ShouldThrowException() {
        // given
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .email(testEmail)
                .type(VerificationType.SIGNUP)
                .build();
        
        EmailVerification blocked = EmailVerification.builder()
                .email(testEmail)
                .code(testCode)
                .type(VerificationType.SIGNUP)
                .blocked(true)
                .blockedUntil(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .verified(false)
                .attempts(5)
                .build();
        
        when(repository.findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP))
                .thenReturn(Optional.of(blocked));
        
        // when & then
        assertThatThrownBy(() -> verificationService.sendVerificationCode(request))
                .isInstanceOf(TooManyAttemptsException.class)
                .hasMessageContaining("차단");
    }
    
    @Test
    @DisplayName("코드 검증 성공")
    void verifyCode_WithCorrectCode_ShouldSucceed() {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email(testEmail)
                .code(testCode)
                .build();
        
        EmailVerification verification = EmailVerification.builder()
                .email(testEmail)
                .code(testCode)
                .type(VerificationType.SIGNUP)
                .verified(false)
                .attempts(0)
                .blocked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .createdAt(LocalDateTime.now())
                .build();
        
        when(repository.findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP))
                .thenReturn(Optional.of(verification));
        
        // when
        EmailVerificationResponse response = verificationService.verifyCode(request);
        
        // then
        assertThat(response.getMessage()).contains("완료");
        verify(repository).save(argThat(v -> v.getVerified() && v.getVerifiedAt() != null));
    }
    
    @Test
    @DisplayName("잘못된 코드 입력 시 시도 횟수 증가")
    void verifyCode_WithWrongCode_ShouldIncrementAttempts() {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email(testEmail)
                .code("999999") // 잘못된 코드
                .build();
        
        EmailVerification verification = EmailVerification.builder()
                .email(testEmail)
                .code(testCode)
                .type(VerificationType.SIGNUP)
                .verified(false)
                .attempts(0)
                .blocked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .createdAt(LocalDateTime.now())
                .build();
        
        when(repository.findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP))
                .thenReturn(Optional.of(verification));
        
        // when
        EmailVerificationResponse response = verificationService.verifyCode(request);
        
        // then
        assertThat(response.getMessage()).contains("일치하지 않습니다");
        assertThat(response.getRemainingAttempts()).isEqualTo(4);
        verify(repository).save(argThat(v -> v.getAttempts() == 1));
    }
    
    @Test
    @DisplayName("5회 실패 시 차단")
    void verifyCode_After5Failures_ShouldBlock() {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email(testEmail)
                .code("999999")
                .build();
        
        EmailVerification verification = EmailVerification.builder()
                .email(testEmail)
                .code(testCode)
                .type(VerificationType.SIGNUP)
                .verified(false)
                .attempts(4) // 이미 4회 실패
                .blocked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .createdAt(LocalDateTime.now())
                .build();
        
        when(repository.findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP))
                .thenReturn(Optional.of(verification));
        
        // when & then
        assertThatThrownBy(() -> verificationService.verifyCode(request))
                .isInstanceOf(TooManyAttemptsException.class)
                .hasMessageContaining("30분간 차단");
        
        verify(repository).save(argThat(v -> v.getBlocked() && v.getBlockedUntil() != null));
    }
    
    @Test
    @DisplayName("만료된 코드 검증 시 예외 발생")
    void verifyCode_WithExpiredCode_ShouldThrowException() {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email(testEmail)
                .code(testCode)
                .build();
        
        EmailVerification verification = EmailVerification.builder()
                .email(testEmail)
                .code(testCode)
                .type(VerificationType.SIGNUP)
                .verified(false)
                .attempts(0)
                .blocked(false)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // 1분 전 만료
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
        
        when(repository.findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP))
                .thenReturn(Optional.of(verification));
        
        // when & then
        assertThatThrownBy(() -> verificationService.verifyCode(request))
                .isInstanceOf(VerificationCodeExpiredException.class)
                .hasMessageContaining("만료");
    }
    
    @Test
    @DisplayName("이메일 인증 완료 여부 확인")
    void isEmailVerified_ShouldReturnTrue() {
        // given
        when(repository.existsByEmailAndTypeAndVerifiedTrue(testEmail, VerificationType.SIGNUP))
                .thenReturn(true);
        
        // when
        boolean result = verificationService.isEmailVerified(testEmail, VerificationType.SIGNUP);
        
        // then
        assertThat(result).isTrue();
    }
}
