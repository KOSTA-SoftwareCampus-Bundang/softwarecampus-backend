# Phase 5: 테스트 작성

## 📋 작업 목표
- 단위 테스트 작성 (Util, Service)
- 통합 테스트 작성 (Controller, Repository)
- Mock 테스트 (이메일 발송)
- 테스트 커버리지 확보

---

## 1️⃣ Util 단위 테스트

### `src/test/java/com/softwarecampus/backend/util/VerificationCodeGeneratorTest.java`
```java
package com.softwarecampus.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VerificationCodeGenerator 단위 테스트
 */
@DisplayName("인증 코드 생성 유틸리티 테스트")
class VerificationCodeGeneratorTest {
    
    @Test
    @DisplayName("생성된 코드는 6자리여야 한다")
    void generateCode_ShouldBe6Digits() {
        // when
        String code = VerificationCodeGenerator.generateCode();
        
        // then
        assertThat(code).hasSize(6);
    }
    
    @Test
    @DisplayName("생성된 코드는 숫자만 포함해야 한다")
    void generateCode_ShouldContainOnlyDigits() {
        // when
        String code = VerificationCodeGenerator.generateCode();
        
        // then
        assertThat(code).matches("^[0-9]{6}$");
    }
    
    @RepeatedTest(1000)
    @DisplayName("생성된 코드는 0 ~ 999999 범위여야 한다")
    void generateCode_ShouldBeInValidRange() {
        // when
        String code = VerificationCodeGenerator.generateCode();
        int numericCode = Integer.parseInt(code);
        
        // then
        assertThat(numericCode).isBetween(0, 999999);
    }
    
    @Test
    @DisplayName("1000번 생성 시 중복이 거의 없어야 한다 (무작위성)")
    void generateCode_ShouldBeRandom() {
        // given
        Set<String> codes = new HashSet<>();
        
        // when
        for (int i = 0; i < 1000; i++) {
            codes.add(VerificationCodeGenerator.generateCode());
        }
        
        // then - 최소 950개 이상은 고유해야 함 (95% 이상)
        assertThat(codes).hasSizeGreaterThan(950);
    }
    
    @Test
    @DisplayName("앞자리 0이 포함된 코드도 6자리여야 한다")
    void generateCode_WithLeadingZeros_ShouldBe6Digits() {
        // given - 앞자리 0인 코드가 나올 때까지 반복
        boolean hasLeadingZero = false;
        
        // when
        for (int i = 0; i < 10000; i++) {
            String code = VerificationCodeGenerator.generateCode();
            if (code.startsWith("0")) {
                hasLeadingZero = true;
                assertThat(code).hasSize(6);
                break;
            }
        }
        
        // then - 10000번 중 최소 1번은 앞자리 0이 나와야 함
        assertThat(hasLeadingZero).isTrue();
    }
    
    @Test
    @DisplayName("유효한 형식의 코드는 검증을 통과해야 한다")
    void isValidFormat_WithValidCode_ShouldReturnTrue() {
        // given
        String validCode = "123456";
        
        // when
        boolean result = VerificationCodeGenerator.isValidFormat(validCode);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("null 코드는 검증 실패해야 한다")
    void isValidFormat_WithNull_ShouldReturnFalse() {
        // when
        boolean result = VerificationCodeGenerator.isValidFormat(null);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("6자리가 아닌 코드는 검증 실패해야 한다")
    void isValidFormat_WithInvalidLength_ShouldReturnFalse() {
        // given
        String shortCode = "12345";
        String longCode = "1234567";
        
        // when & then
        assertThat(VerificationCodeGenerator.isValidFormat(shortCode)).isFalse();
        assertThat(VerificationCodeGenerator.isValidFormat(longCode)).isFalse();
    }
    
    @Test
    @DisplayName("숫자가 아닌 문자가 포함된 코드는 검증 실패해야 한다")
    void isValidFormat_WithNonDigits_ShouldReturnFalse() {
        // given
        String codeWithLetters = "12A456";
        String codeWithSpecialChars = "123@56";
        
        // when & then
        assertThat(VerificationCodeGenerator.isValidFormat(codeWithLetters)).isFalse();
        assertThat(VerificationCodeGenerator.isValidFormat(codeWithSpecialChars)).isFalse();
    }
}
```

**경로:** `src/test/java/com/softwarecampus/backend/util/`

**테스트 커버리지:**
- 코드 길이 검증
- 숫자 형식 검증
- 범위 검증 (0~999999)
- 무작위성 검증 (중복 최소화)
- 앞자리 0 처리
- 유효성 검증 메서드

---

## 2️⃣ Repository 테스트

### `src/test/java/com/softwarecampus/backend/repository/EmailVerificationRepositoryTest.java`
```java
package com.softwarecampus.backend.repository;

import com.softwarecampus.backend.model.entity.EmailVerification;
import com.softwarecampus.backend.model.enums.VerificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EmailVerificationRepository 통합 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("이메일 인증 Repository 테스트")
class EmailVerificationRepositoryTest {
    
    @Autowired
    private EmailVerificationRepository repository;
    
    private String testEmail;
    private String testCode;
    
    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testCode = "123456";
        repository.deleteAll();
    }
    
    @Test
    @DisplayName("이메일 인증 레코드를 저장할 수 있다")
    void save_ShouldPersistEmailVerification() {
        // given
        EmailVerification verification = createVerification(testEmail, testCode);
        
        // when
        EmailVerification saved = repository.save(verification);
        
        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo(testEmail);
        assertThat(saved.getCode()).isEqualTo(testCode);
    }
    
    @Test
    @DisplayName("이메일과 타입으로 최근 레코드를 조회할 수 있다")
    void findTopByEmailAndTypeOrderByCreatedAtDesc_ShouldReturnLatest() {
        // given
        repository.save(createVerification(testEmail, "111111"));
        Thread.sleep(10); // 시간차 보장
        EmailVerification latest = repository.save(createVerification(testEmail, "222222"));
        
        // when
        Optional<EmailVerification> result = repository
                .findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP);
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("222222");
    }
    
    @Test
    @DisplayName("인증 완료된 레코드가 존재하는지 확인할 수 있다")
    void existsByEmailAndTypeAndVerifiedTrue_ShouldReturnTrue() {
        // given
        EmailVerification verification = createVerification(testEmail, testCode);
        verification.markAsVerified();
        repository.save(verification);
        
        // when
        boolean exists = repository.existsByEmailAndTypeAndVerifiedTrue(
                testEmail, 
                VerificationType.SIGNUP
        );
        
        // then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("인증 완료되지 않은 경우 false를 반환한다")
    void existsByEmailAndTypeAndVerifiedTrue_WithUnverified_ShouldReturnFalse() {
        // given
        repository.save(createVerification(testEmail, testCode));
        
        // when
        boolean exists = repository.existsByEmailAndTypeAndVerifiedTrue(
                testEmail, 
                VerificationType.SIGNUP
        );
        
        // then
        assertThat(exists).isFalse();
    }
    
    @Test
    @DisplayName("이메일, 타입, 코드로 레코드를 조회할 수 있다")
    void findByEmailAndTypeAndCode_ShouldReturnVerification() {
        // given
        repository.save(createVerification(testEmail, testCode));
        
        // when
        Optional<EmailVerification> result = repository.findByEmailAndTypeAndCode(
                testEmail, 
                VerificationType.SIGNUP, 
                testCode
        );
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(testEmail);
    }
    
    @Test
    @DisplayName("만료된 인증 완료 데이터를 삭제할 수 있다")
    void deleteByExpiresAtBeforeAndVerifiedTrue_ShouldDeleteExpired() {
        // given
        EmailVerification verified = createVerification(testEmail, testCode);
        verified.markAsVerified();
        repository.save(verified);
        
        // when
        LocalDateTime threshold = LocalDateTime.now().plusHours(1);
        repository.deleteByExpiresAtBeforeAndVerifiedTrue(threshold);
        
        // then
        assertThat(repository.findAll()).isEmpty();
    }
    
    @Test
    @DisplayName("미인증 데이터를 삭제할 수 있다")
    void deleteByCreatedAtBeforeAndVerifiedFalse_ShouldDeleteUnverified() {
        // given
        repository.save(createVerification(testEmail, testCode));
        
        // when
        LocalDateTime threshold = LocalDateTime.now().plusHours(1);
        repository.deleteByCreatedAtBeforeAndVerifiedFalse(threshold);
        
        // then
        assertThat(repository.findAll()).isEmpty();
    }
    
    private EmailVerification createVerification(String email, String code) {
        return EmailVerification.builder()
                .email(email)
                .code(code)
                .type(VerificationType.SIGNUP)
                .verified(false)
                .attempts(0)
                .blocked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();
    }
}
```

**경로:** `src/test/java/com/softwarecampus/backend/repository/`

**테스트 커버리지:**
- 저장 기능
- 최근 레코드 조회
- 인증 완료 여부 확인
- 코드로 레코드 조회
- 배치 삭제 메서드

---

## 3️⃣ Service 단위 테스트 (Mock)

### `src/test/java/com/softwarecampus/backend/service/email/EmailVerificationServiceTest.java`
```java
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
        when(repository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // when
        EmailVerificationResponse response = verificationService.sendVerificationCode(request);
        
        // then
        assertThat(response.getMessage()).contains("발송");
        assertThat(response.getExpiresIn()).isEqualTo(180);
        verify(emailSendService).sendVerificationCode(eq(testEmail), anyString(), eq(VerificationType.SIGNUP));
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
```

**경로:** `src/test/java/com/softwarecampus/backend/service/email/`

**테스트 커버리지:**
- 코드 발송 성공
- 재발송 쿨다운 체크
- 차단 상태 체크
- 코드 검증 성공
- 잘못된 코드 입력
- 5회 실패 시 차단
- 코드 만료
- 인증 완료 여부 확인

---

## 4️⃣ Controller 통합 테스트

### `src/test/java/com/softwarecampus/backend/controller/EmailVerificationControllerTest.java`
```java
package com.softwarecampus.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.model.dto.email.EmailVerificationCodeRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationResponse;
import com.softwarecampus.backend.model.enums.VerificationType;
import com.softwarecampus.backend.service.email.EmailVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EmailVerificationController 통합 테스트
 */
@WebMvcTest(EmailVerificationController.class)
@DisplayName("이메일 인증 Controller 테스트")
class EmailVerificationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private EmailVerificationService verificationService;
    
    @Test
    @DisplayName("POST /api/auth/email/send-verification - 성공")
    void sendSignupVerification_ShouldReturn200() throws Exception {
        // given
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .email("test@example.com")
                .build();
        
        EmailVerificationResponse response = EmailVerificationResponse.withExpiry(
                "인증 코드가 발송되었습니다",
                180
        );
        
        when(verificationService.sendVerificationCode(any())).thenReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/auth/email/send-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다"))
                .andExpect(jsonPath("$.expiresIn").value(180));
    }
    
    @Test
    @DisplayName("POST /api/auth/email/send-verification - 이메일 형식 오류")
    void sendSignupVerification_WithInvalidEmail_ShouldReturn400() throws Exception {
        // given
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .email("invalid-email") // 잘못된 형식
                .build();
        
        // when & then
        mockMvc.perform(post("/api/auth/email/send-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("POST /api/auth/email/verify - 성공")
    void verifySignupCode_ShouldReturn200() throws Exception {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email("test@example.com")
                .code("123456")
                .build();
        
        EmailVerificationResponse response = EmailVerificationResponse.success(
                "이메일 인증이 완료되었습니다"
        );
        
        when(verificationService.verifyCode(any())).thenReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다"));
    }
    
    @Test
    @DisplayName("POST /api/auth/email/verify - 코드 형식 오류")
    void verifySignupCode_WithInvalidCodeFormat_ShouldReturn400() throws Exception {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email("test@example.com")
                .code("12345") // 5자리 (잘못된 형식)
                .build();
        
        // when & then
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("POST /api/auth/email/send-reset-code - 성공")
    void sendPasswordResetCode_ShouldReturn200() throws Exception {
        // given
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .email("test@example.com")
                .build();
        
        EmailVerificationResponse response = EmailVerificationResponse.withExpiry(
                "인증 코드가 발송되었습니다",
                180
        );
        
        when(verificationService.sendVerificationCode(any())).thenReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/auth/email/send-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.expiresIn").value(180));
    }
    
    @Test
    @DisplayName("POST /api/auth/email/verify-reset - 성공")
    void verifyPasswordResetCode_ShouldReturn200() throws Exception {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email("test@example.com")
                .code("654321")
                .build();
        
        EmailVerificationResponse response = EmailVerificationResponse.success(
                "인증이 완료되었습니다. 새 비밀번호를 설정하세요"
        );
        
        when(verificationService.verifyResetCode(any())).thenReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/auth/email/verify-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
```

**경로:** `src/test/java/com/softwarecampus/backend/controller/`

**테스트 커버리지:**
- 4개 API 엔드포인트 성공 케이스
- Validation 실패 케이스
- JSON 응답 검증

---

## 5️⃣ test/resources 설정

### `src/test/resources/application-test.properties`
```properties
# Test Profile Configuration

# H2 In-Memory Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# Logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Mail (Mock)
spring.mail.host=localhost
spring.mail.port=25
spring.mail.username=test@example.com
spring.mail.password=test-password

# Email Verification Settings
email.verification.code-length=6
email.verification.expiry-minutes=3
email.verification.max-attempts=5
email.verification.block-duration-minutes=30
email.verification.resend-cooldown-seconds=60
```

**경로:** `src/test/resources/`

---

## ✅ Phase 5 완료 체크리스트

### Util 테스트
- [ ] `VerificationCodeGeneratorTest` 작성
- [ ] 코드 길이 테스트
- [ ] 숫자 형식 테스트
- [ ] 범위 테스트 (0~999999)
- [ ] 무작위성 테스트
- [ ] 앞자리 0 처리 테스트
- [ ] 유효성 검증 테스트

### Repository 테스트
- [ ] `EmailVerificationRepositoryTest` 작성
- [ ] 저장/조회 테스트
- [ ] 최근 레코드 조회 테스트
- [ ] 인증 완료 여부 확인 테스트
- [ ] 배치 삭제 테스트

### Service 테스트 (Mock)
- [ ] `EmailVerificationServiceTest` 작성
- [ ] 코드 발송 성공 테스트
- [ ] 재발송 쿨다운 테스트
- [ ] 차단 상태 테스트
- [ ] 코드 검증 성공 테스트
- [ ] 잘못된 코드 입력 테스트
- [ ] 5회 실패 차단 테스트
- [ ] 코드 만료 테스트

### Controller 테스트
- [ ] `EmailVerificationControllerTest` 작성
- [ ] 4개 API 성공 케이스 테스트
- [ ] Validation 실패 테스트
- [ ] JSON 응답 검증

### 설정
- [ ] `application-test.properties` 작성
- [ ] H2 In-Memory DB 설정
- [ ] Mock SMTP 설정

### 테스트 실행
- [ ] 전체 테스트 실행 성공
- [ ] 테스트 커버리지 80% 이상 달성

---

## 📌 다음 단계 (Phase 6)

- 기존 코드와 통합 (SignupService, PasswordResetService)
- E2E 테스트 (회원가입 전체 플로우)
- 문서화 (API 명세서, README)

---

## 🔍 테스트 실행 명령어

### 전체 테스트 실행
```bash
./mvnw test
```

### 특정 클래스 테스트
```bash
./mvnw test -Dtest=VerificationCodeGeneratorTest
```

### 테스트 커버리지 확인 (JaCoCo)
```bash
./mvnw clean test jacoco:report
```
- 리포트 위치: `target/site/jacoco/index.html`

---

## 🎯 테스트 원칙

### 1. AAA 패턴
- **Arrange** (준비): 테스트 데이터 설정
- **Act** (실행): 테스트 대상 메서드 실행
- **Assert** (검증): 결과 검증

### 2. 독립성
- 각 테스트는 독립적으로 실행 가능
- `@BeforeEach`로 초기화
- DB 테스트는 `@DataJpaTest`로 격리

### 3. 명확성
- `@DisplayName`으로 테스트 의도 명시
- 테스트 메서드명: `메서드명_조건_예상결과`

### 4. Mock vs Real
- **Mock**: Service 레이어 (의존성 격리)
- **Real**: Repository 레이어 (실제 DB 동작)
