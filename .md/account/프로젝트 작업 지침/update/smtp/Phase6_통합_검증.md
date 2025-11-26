# Phase 6: 통합 및 검증

## 📋 작업 목표
- 기존 회원가입/비밀번호 재설정 로직과 통합
- E2E 통합 테스트 작성
- 전체 플로우 검증
- 최종 검토 및 배포 준비

---

## 1️⃣ 회원가입 Service 통합

### `src/main/java/com/softwarecampus/backend/service/user/SignupServiceImpl.java` 수정

```java
package com.softwarecampus.backend.service.user;

import com.softwarecampus.backend.exception.email.EmailNotVerifiedException;
import com.softwarecampus.backend.model.dto.user.SignupRequest;
import com.softwarecampus.backend.model.dto.user.AccountResponse;
import com.softwarecampus.backend.model.entity.User;
import com.softwarecampus.backend.model.enums.VerificationType;
import com.softwarecampus.backend.repository.UserRepository;
import com.softwarecampus.backend.service.email.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService verificationService; // 추가
    // ... 기타 의존성 (JwtService 등)
    
    @Override
    @Transactional
    public AccountResponse signup(SignupRequest request) {
        String email = request.getEmail();
        
        // ========================================
        // 1. 이메일 인증 완료 여부 확인 (새로 추가)
        // ========================================
        boolean verified = verificationService.isEmailVerified(
                email, 
                VerificationType.SIGNUP
        );
        
        if (!verified) {
            log.warn("이메일 미인증 회원가입 시도 발생");
            throw new EmailNotVerifiedException("이메일 인증이 완료되지 않았습니다");
        }
        
        // ========================================
        // 2. 기존 회원가입 로직
        // ========================================
        
        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다");
        }
        
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        
        // 사용자 생성
        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .username(request.getUsername())
                .role(UserRole.USER)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // 토큰 발급
        String accessToken = jwtService.generateAccessToken(savedUser.getEmail());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getEmail());
        
        log.info("회원가입 완료 - username: {}", request.getUsername());
        
        return AccountResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
```

**변경 사항:**
- `EmailVerificationService` 의존성 주입
- 기존 로직 실행 전 `isEmailVerified()` 체크 추가
- 미인증 시 `EmailNotVerifiedException` 발생

---

## 2️⃣ 비밀번호 재설정 통합

### `src/main/java/com/softwarecampus/backend/controller/PasswordResetController.java`
```java
package com.softwarecampus.backend.controller;

import com.softwarecampus.backend.model.dto.user.PasswordResetRequest;
import com.softwarecampus.backend.service.user.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 비밀번호 재설정 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {
    
    private final PasswordResetService passwordResetService;
    
    /**
     * 비밀번호 재설정
     * POST /api/auth/password/reset
     * 
     * 사전 조건: 이메일 인증 완료 (verify-reset)
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        log.info("비밀번호 재설정 요청");
        
        passwordResetService.resetPassword(request);
        
        return ResponseEntity.ok(Map.of(
                "message", "비밀번호가 성공적으로 변경되었습니다"
        ));
    }
}
```

### `src/main/java/com/softwarecampus/backend/service/user/PasswordResetService.java`
```java
package com.softwarecampus.backend.service.user;

import com.softwarecampus.backend.exception.email.EmailNotVerifiedException;
import com.softwarecampus.backend.exception.user.UserNotFoundException;
import com.softwarecampus.backend.model.dto.user.PasswordResetRequest;
import com.softwarecampus.backend.model.entity.User;
import com.softwarecampus.backend.model.enums.VerificationType;
import com.softwarecampus.backend.repository.UserRepository;
import com.softwarecampus.backend.service.email.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비밀번호 재설정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    
    private final UserRepository userRepository;
    private final EmailVerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String email = request.getEmail();
        
        // 1. 이메일 인증 완료 여부 확인
        boolean verified = verificationService.isEmailVerified(
                email, 
                VerificationType.PASSWORD_RESET
        );
        
        if (!verified) {
            log.warn("이메일 미인증 비밀번호 재설정 시도 발생");
            throw new EmailNotVerifiedException("이메일 인증이 완료되지 않았습니다");
        }
        
        // 2. 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));
        
        // 3. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(encodedPassword);
        userRepository.save(user);
        
        log.info("비밀번호 재설정 완료");
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/service/user/`

---

## 3️⃣ E2E 통합 테스트

### `src/test/java/com/softwarecampus/backend/integration/SignupWithEmailVerificationIntegrationTest.java`
```java
package com.softwarecampus.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.model.dto.email.EmailVerificationCodeRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationRequest;
import com.softwarecampus.backend.model.dto.user.SignupRequest;
import com.softwarecampus.backend.model.entity.EmailVerification;
import com.softwarecampus.backend.model.enums.VerificationType;
import com.softwarecampus.backend.repository.EmailVerificationRepository;
import com.softwarecampus.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 회원가입 + 이메일 인증 E2E 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("회원가입 이메일 인증 E2E 테스트")
class SignupWithEmailVerificationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private EmailVerificationRepository verificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private String testEmail;
    private String testCode;
    
    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testCode = "123456";
        
        verificationRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    @DisplayName("전체 플로우: 이메일 인증 → 회원가입 성공")
    void completeSignupFlow_WithEmailVerification_ShouldSucceed() throws Exception {
        // ========================================
        // 1. 이메일 인증 코드 발송
        // ========================================
        EmailVerificationRequest verifyRequest = EmailVerificationRequest.builder()
                .email(testEmail)
                .build();
        
        mockMvc.perform(post("/api/auth/email/send-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다"));
        
        // ========================================
        // 2. DB에서 발송된 코드 조회 (실제로는 이메일에서 확인)
        // ========================================
        EmailVerification verification = verificationRepository
                .findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP)
                .orElseThrow();
        
        String actualCode = verification.getCode();
        
        // ========================================
        // 3. 인증 코드 검증
        // ========================================
        EmailVerificationCodeRequest codeRequest = EmailVerificationCodeRequest.builder()
                .email(testEmail)
                .code(actualCode)
                .build();
        
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(codeRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다"));
        
        // ========================================
        // 4. 회원가입 (이메일 인증 완료 후)
        // ========================================
        SignupRequest signupRequest = SignupRequest.builder()
                .email(testEmail)
                .password("Test1234!@")
                .username("테스트유저")
                .build();
        
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.username").value("테스트유저"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }
    
    @Test
    @DisplayName("이메일 미인증 상태에서 회원가입 시도 → 403 Forbidden")
    void signup_WithoutEmailVerification_ShouldReturn403() throws Exception {
        // given - 이메일 인증 없이 바로 회원가입 시도
        SignupRequest signupRequest = SignupRequest.builder()
                .email(testEmail)
                .password("Test1234!@")
                .username("테스트유저")
                .build();
        
        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되지 않았습니다"));
    }
    
    @Test
    @DisplayName("잘못된 코드 5회 입력 → 차단 → 회원가입 불가")
    void signup_After5FailedAttempts_ShouldBeBlocked() throws Exception {
        // 1. 인증 코드 발송
        mockMvc.perform(post("/api/auth/email/send-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                EmailVerificationRequest.builder().email(testEmail).build()
                        )))
                .andExpect(status().isOk());
        
        // 2. 잘못된 코드 5회 입력
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    EmailVerificationCodeRequest.builder()
                                            .email(testEmail)
                                            .code("999999")
                                            .build()
                            )))
                    .andDo(print());
        }
        
        // 3. 차단 확인 (429 Too Many Requests)
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                EmailVerificationCodeRequest.builder()
                                        .email(testEmail)
                                        .code("123456")
                                        .build()
                        )))
                .andDo(print())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("인증 시도 횟수를 초과했습니다. 30분간 차단됩니다"));
        
        // 4. 회원가입 불가 (인증 미완료)
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                SignupRequest.builder()
                                        .email(testEmail)
                                        .password("Test1234!@")
                                        .username("테스트유저")
                                        .build()
                        )))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @DisplayName("코드 만료 후 검증 시도 → 400 Bad Request")
    void verify_WithExpiredCode_ShouldReturn400() throws Exception {
        // given - 만료된 코드 직접 생성
        EmailVerification expiredVerification = EmailVerification.builder()
                .email(testEmail)
                .code(testCode)
                .type(VerificationType.SIGNUP)
                .verified(false)
                .attempts(0)
                .blocked(false)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // 5분 전 만료
                .build();
        
        verificationRepository.save(expiredVerification);
        
        // when & then
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                EmailVerificationCodeRequest.builder()
                                        .email(testEmail)
                                        .code(testCode)
                                        .build()
                        )))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증 코드가 만료되었습니다. 새로운 코드를 요청하세요"));
    }
}
```

**경로:** `src/test/java/com/softwarecampus/backend/integration/`

**테스트 시나리오:**
1. ✅ 정상 플로우: 코드 발송 → 검증 → 회원가입 성공
2. ✅ 미인증 회원가입 차단 (403)
3. ✅ 5회 실패 시 차단 (429)
4. ✅ 만료 코드 검증 실패 (400)

---

## 4️⃣ 비밀번호 재설정 E2E 테스트

### `src/test/java/com/softwarecampus/backend/integration/PasswordResetWithEmailVerificationIntegrationTest.java`
```java
package com.softwarecampus.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.model.dto.email.EmailVerificationCodeRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationRequest;
import com.softwarecampus.backend.model.dto.user.PasswordResetRequest;
import com.softwarecampus.backend.model.entity.EmailVerification;
import com.softwarecampus.backend.model.entity.User;
import com.softwarecampus.backend.model.enums.UserRole;
import com.softwarecampus.backend.model.enums.VerificationType;
import com.softwarecampus.backend.repository.EmailVerificationRepository;
import com.softwarecampus.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 비밀번호 재설정 + 이메일 인증 E2E 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("비밀번호 재설정 이메일 인증 E2E 테스트")
class PasswordResetWithEmailVerificationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private EmailVerificationRepository verificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private String testEmail;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        
        verificationRepository.deleteAll();
        userRepository.deleteAll();
        
        // 기존 사용자 생성
        testUser = User.builder()
                .email(testEmail)
                .password(passwordEncoder.encode("OldPassword1234!"))
                .username("테스트유저")
                .role(UserRole.USER)
                .build();
        
        userRepository.save(testUser);
    }
    
    @Test
    @DisplayName("전체 플로우: 이메일 인증 → 비밀번호 재설정 성공")
    void completePasswordResetFlow_WithEmailVerification_ShouldSucceed() throws Exception {
        // 1. 비밀번호 재설정 코드 발송
        mockMvc.perform(post("/api/auth/email/send-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                EmailVerificationRequest.builder().email(testEmail).build()
                        )))
                .andDo(print())
                .andExpect(status().isOk());
        
        // 2. DB에서 발송된 코드 조회
        EmailVerification verification = verificationRepository
                .findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.PASSWORD_RESET)
                .orElseThrow();
        
        String actualCode = verification.getCode();
        
        // 3. 인증 코드 검증
        mockMvc.perform(post("/api/auth/email/verify-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                EmailVerificationCodeRequest.builder()
                                        .email(testEmail)
                                        .code(actualCode)
                                        .build()
                        )))
                .andDo(print())
                .andExpect(status().isOk());
        
        // 4. 비밀번호 재설정
        String newPassword = "NewPassword1234!";
        
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PasswordResetRequest.builder()
                                        .email(testEmail)
                                        .newPassword(newPassword)
                                        .build()
                        )))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다"));
        
        // 5. 비밀번호 변경 확인
        User updatedUser = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();
    }
    
    @Test
    @DisplayName("이메일 미인증 상태에서 비밀번호 재설정 시도 → 403 Forbidden")
    void resetPassword_WithoutEmailVerification_ShouldReturn403() throws Exception {
        // when & then
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                PasswordResetRequest.builder()
                                        .email(testEmail)
                                        .newPassword("NewPassword1234!")
                                        .build()
                        )))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되지 않았습니다"));
    }
}
```

**경로:** `src/test/java/com/softwarecampus/backend/integration/`

---

## 5️⃣ 최종 검증 체크리스트

### 기능 검증
- [ ] 회원가입 인증 코드 발송 성공
- [ ] 회원가입 인증 코드 검증 성공
- [ ] 비밀번호 재설정 코드 발송 성공
- [ ] 비밀번호 재설정 코드 검증 성공
- [ ] 실제 Gmail SMTP 발송 테스트
- [ ] HTML 이메일 템플릿 정상 렌더링

### 보안 검증
- [ ] 재발송 60초 쿨다운 동작
- [ ] 5회 실패 시 30분 차단 동작
- [ ] 코드 만료 3분 동작
- [ ] 차단 자동 해제 동작
- [ ] 이메일 미인증 시 회원가입 차단
- [ ] 이메일 미인증 시 비밀번호 재설정 차단

### 통합 검증
- [ ] 회원가입 플로우 E2E 테스트 통과
- [ ] 비밀번호 재설정 플로우 E2E 테스트 통과
- [ ] 기존 회원가입 로직과 충돌 없음
- [ ] 배치 작업 (스케줄러) 정상 동작

### 코드 품질
- [ ] 전체 테스트 통과 (단위 + 통합)
- [ ] 테스트 커버리지 80% 이상
- [ ] Checkstyle/PMD 검사 통과
- [ ] 코드 리뷰 완료

---

## 6️⃣ 배포 전 설정 확인

### 환경 변수 설정
```bash
# .env 파일 (프로덕션)
MAIL_USERNAME=your-production-email@gmail.com
MAIL_APP_PASSWORD=your-16-digit-app-password
```

**⚠️ 보안 경고:**
- **절대로 실제 비밀번호를 문서나 Git에 포함하지 마세요**
- `.env` 파일은 반드시 `.gitignore`에 추가되어야 합니다
- 비밀번호가 노출되었다면 즉시 폐기하고 재생성하세요
- 프로덕션 환경에서는 환경 변수 또는 비밀 관리 시스템을 사용하세요

### Gmail SMTP 설정 확인
1. Google 계정 → 보안 → 2단계 인증 활성화
2. 앱 비밀번호 생성 ([Google 앱 비밀번호](https://myaccount.google.com/apppasswords))
3. `.env` 파일에 비밀번호 입력
4. `.gitignore`에 `.env` 추가 확인

### DB 마이그레이션
```sql
-- email_verification 테이블 생성 확인
CREATE TABLE IF NOT EXISTS email_verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    code VARCHAR(6) NOT NULL,
    type VARCHAR(20) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_until TIMESTAMP NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL,
    INDEX idx_email_type (email, type),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 스케줄러 활성화 확인
```java
// Application.java
@EnableScheduling // 이 어노테이션 확인
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 7️⃣ 문서화

### API 명세서 업데이트

#### Swagger/OpenAPI 설정 (선택)
```java
// pom.xml에 추가
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

#### Controller에 API 문서 추가
```java
@RestController
@RequestMapping("/api/auth/email")
@Tag(name = "Email Verification", description = "이메일 인증 API")
public class EmailVerificationController {
    
    @Operation(summary = "회원가입 인증 코드 발송", description = "회원가입을 위한 6자리 인증 코드를 이메일로 발송합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "발송 성공"),
        @ApiResponse(responseCode = "400", description = "재발송 쿨다운 또는 차단 상태"),
        @ApiResponse(responseCode = "500", description = "이메일 발송 실패")
    })
    @PostMapping("/send-verification")
    public ResponseEntity<EmailVerificationResponse> sendSignupVerification(...) {
        // ...
    }
}
```

### README.md 업데이트
```markdown
## 이메일 인증 기능

### 개요
회원가입 및 비밀번호 재설정 시 이메일 인증을 통한 보안 강화

### 주요 기능
- 6자리 인증 코드 발송 (Gmail SMTP)
- 코드 유효 시간: 3분
- 재발송 쿨다운: 60초
- 5회 실패 시 30분 차단

### API 엔드포인트
- `POST /api/auth/email/send-verification` - 회원가입 코드 발송
- `POST /api/auth/email/verify` - 회원가입 코드 검증
- `POST /api/auth/email/send-reset-code` - 비밀번호 재설정 코드 발송
- `POST /api/auth/email/verify-reset` - 비밀번호 재설정 코드 검증

### 환경 설정
1. Gmail 앱 비밀번호 생성
2. `.env` 파일에 설정 추가
3. 배치 작업 활성화 (`@EnableScheduling`)
```

---

## ✅ Phase 6 완료 체크리스트

### 통합 작업
- [ ] `SignupServiceImpl`에 이메일 인증 체크 통합
- [ ] `PasswordResetService` 구현
- [ ] `PasswordResetController` 구현

### E2E 테스트
- [ ] 회원가입 전체 플로우 테스트
- [ ] 비밀번호 재설정 전체 플로우 테스트
- [ ] 미인증 차단 테스트
- [ ] 5회 실패 차단 테스트
- [ ] 코드 만료 테스트

### 배포 준비
- [ ] `.env` 파일 설정
- [ ] Gmail SMTP 설정 완료
- [ ] DB 마이그레이션 확인
- [ ] 스케줄러 활성화 확인
- [ ] 전체 테스트 통과

### 문서화
- [ ] API 명세서 작성/업데이트
- [ ] README.md 업데이트
- [ ] 배포 가이드 작성

---

## 🎉 프로젝트 완료

모든 Phase가 완료되면:
1. ✅ Phase 1: 기본 설정 (의존성, 엔티티, DTO)
2. ✅ Phase 2: Repository, Util, 템플릿
3. ✅ Phase 3: Service 구현
4. ✅ Phase 4: Controller, 예외 처리
5. ✅ Phase 5: 테스트 작성
6. ✅ Phase 6: 통합 및 검증

**최종 산출물:**
- 완전한 이메일 인증 시스템
- 80% 이상 테스트 커버리지
- 프로덕션 배포 준비 완료
- 상세 문서화

---

## 📌 향후 개선 사항

### 1. 이메일 템플릿 엔진
- Thymeleaf로 동적 HTML 생성
- 다국어 지원 (i18n)

### 2. 알림 설정
- 사용자별 이메일 알림 수신 설정
- 마케팅 이메일 구분

### 3. OAuth 통합
- Google/Kakao 로그인 시 이메일 인증 생략
- 소셜 로그인 연동

### 4. 모니터링
- 이메일 발송 성공률 대시보드
- 차단 계정 모니터링
- 인증 완료율 분석

### 5. 성능 최적화
- Redis 캐싱 (선택적 도입)
- 이메일 발송 큐 (비동기 처리)
