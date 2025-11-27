# Phase 4: Controller 및 예외 처리

## 📋 작업 목표
- REST API Controller 구현 (4개 엔드포인트)
- 글로벌 예외 핸들러 구현
- API 응답 형식 통일

---

## 1️⃣ Controller 구현

### `src/main/java/com/softwarecampus/backend/controller/EmailVerificationController.java`
```java
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
        log.info("회원가입 인증 코드 발송 요청 - email: {}", request.getEmail());
        
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
        log.info("회원가입 인증 코드 검증 요청 - email: {}", request.getEmail());
        
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
        log.info("비밀번호 재설정 인증 코드 발송 요청 - email: {}", request.getEmail());
        
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
        log.info("비밀번호 재설정 인증 코드 검증 요청 - email: {}", request.getEmail());
        
        EmailVerificationResponse response = verificationService.verifyResetCode(request);
        return ResponseEntity.ok(response);
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/controller/`

**API 엔드포인트:**
1. `POST /api/auth/email/send-verification` - 회원가입 코드 발송
2. `POST /api/auth/email/verify` - 회원가입 코드 검증
3. `POST /api/auth/email/send-reset-code` - 비밀번호 재설정 코드 발송
4. `POST /api/auth/email/verify-reset` - 비밀번호 재설정 코드 검증

**보안 특징:**
- `@Valid`로 입력 검증 (DTO의 `@Email`, `@Pattern` 등)
- Controller에서 `VerificationType` 강제 설정 (클라이언트 조작 방지)

---

## 2️⃣ 글로벌 예외 핸들러

### `src/main/java/com/softwarecampus/backend/exception/GlobalExceptionHandler.java` 확장

기존 `GlobalExceptionHandler`에 이메일 관련 예외 처리 추가:

```java
package com.softwarecampus.backend.exception;

import com.softwarecampus.backend.exception.email.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 글로벌 예외 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("입력값 검증 실패", errors));
    }
    
    /**
     * 이메일 발송 실패 예외
     */
    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<Map<String, Object>> handleEmailSendException(EmailSendException ex) {
        log.error("이메일 발송 실패", ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(ex.getMessage(), null));
    }
    
    /**
     * 이메일 인증 예외 (일반)
     */
    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<Map<String, Object>> handleEmailVerificationException(
            EmailVerificationException ex
    ) {
        log.warn("이메일 인증 예외: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(ex.getMessage(), null));
    }
    
    /**
     * 이메일 미인증 예외
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerifiedException(
            EmailNotVerifiedException ex
    ) {
        log.warn("이메일 미인증: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(ex.getMessage(), null));
    }
    
    /**
     * 인증 코드 만료 예외
     */
    @ExceptionHandler(VerificationCodeExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationCodeExpiredException(
            VerificationCodeExpiredException ex
    ) {
        log.warn("인증 코드 만료: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(ex.getMessage(), null));
    }
    
    /**
     * 인증 시도 횟수 초과 예외
     */
    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyAttemptsException(
            TooManyAttemptsException ex
    ) {
        log.warn("인증 시도 횟수 초과: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        // ISO-8601 형식으로 통일 (다른 timestamp와 일관성 유지)
        errorDetails.put("blockedUntil", ex.getBlockedUntil().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        ));
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(createErrorResponse(ex.getMessage(), errorDetails));
    }
    
    /**
     * 공통 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message, Object details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        
        if (details != null) {
            response.put("details", details);
        }
        
        return response;
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/exception/`

**HTTP 상태 코드 매핑:**
- `400 BAD_REQUEST`: 일반 검증 실패, 코드 만료
- `403 FORBIDDEN`: 이메일 미인증 (회원가입 시도)
- `429 TOO_MANY_REQUESTS`: 시도 횟수 초과 (차단)
- `500 INTERNAL_SERVER_ERROR`: 이메일 발송 실패

---

## 3️⃣ API 응답 형식 통일

### 성공 응답 예시

#### 1. 인증 코드 발송 성공 (200 OK)
```json
{
  "message": "인증 코드가 발송되었습니다",
  "expiresIn": 180,
  "remainingAttempts": null
}
```

#### 2. 인증 코드 검증 성공 (200 OK)
```json
{
  "message": "이메일 인증이 완료되었습니다",
  "expiresIn": null,
  "remainingAttempts": null
}
```

**참고:** 성공 응답은 `EmailVerificationResponse` DTO를 그대로 반환합니다. 프로젝트에서 RFC 9457 ProblemDetail을 사용하므로, 성공 응답에는 별도의 `success` 필드를 추가하지 않습니다.

### 부분 실패 응답 (검증 실패 - 재시도 가능)

#### 3. 코드 불일치 - 남은 시도 횟수 포함 (200 OK)
```json
{
  "message": "인증 코드가 일치하지 않습니다",
  "expiresIn": null,
  "remainingAttempts": 3
}
```

**참고:** 코드 불일치는 Service 레벨에서 처리되어 정상 응답(`200 OK`)으로 반환되며, `remainingAttempts` 필드로 재시도 가능 여부를 알립니다. 5회 초과 시에만 예외(`TooManyAttemptsException`)가 발생합니다.

### 실패 응답 예시 (RFC 9457 ProblemDetail)

프로젝트는 RFC 9457 표준을 따르는 ProblemDetail 형식을 사용합니다.

#### 1. Validation 실패 (400)
```json
{
  "type": "https://api.example.com/problems/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "입력값 검증 실패",
  "instance": "/api/auth/email/send-verification",
  "errors": {
    "email": "유효한 이메일 형식이 아닙니다",
    "code": "인증 코드는 6자리 숫자여야 합니다"
  }
}
```

#### 2. 재발송 쿨다운 (400)
```json
{
  "type": "https://api.example.com/problems/email-verification-failed",
  "title": "Email Verification Failed",
  "status": 400,
  "detail": "인증 코드는 45초 후에 재발송할 수 있습니다",
  "instance": "/api/auth/email/send-verification"
}
```

#### 3. 코드 만료 (400)
```json
{
  "type": "https://api.example.com/problems/verification-code-expired",
  "title": "Verification Code Expired",
  "status": 400,
  "detail": "인증 코드가 만료되었습니다. 새로운 코드를 요청하세요",
  "instance": "/api/auth/email/verify"
}
```

#### 4. 시도 횟수 초과 - 차단 (429)
```json
{
  "type": "https://api.example.com/problems/too-many-attempts",
  "title": "Too Many Attempts",
  "status": 429,
  "detail": "인증 시도 횟수를 초과했습니다. 30분간 차단됩니다",
  "instance": "/api/auth/email/verify",
  "blockedUntil": "2025-11-26T15:00:00"
}
```

**타임스탬프 형식:** ISO-8601 형식(`yyyy-MM-ddTHH:mm:ss`)으로 통일됩니다.

#### 5. 이메일 미인증 (403)
```json
{
  "type": "https://api.example.com/problems/email-not-verified",
  "title": "Email Not Verified",
  "status": 403,
  "detail": "이메일 인증이 필요합니다",
  "instance": "/api/auth/signup"
}
```

#### 6. 이메일 발송 실패 (500)
```json
{
  "type": "https://api.example.com/problems/email-send-failed",
  "title": "Email Send Failed",
  "status": 500,
  "detail": "이메일 발송에 실패했습니다",
  "instance": "/api/auth/email/send-verification"
}
```

---

## 4️⃣ 회원가입 Service 통합

### `src/main/java/com/softwarecampus/backend/service/user/SignupServiceImpl.java` 수정

기존 `SignupService`에 이메일 인증 체크 로직 추가:

```java
package com.softwarecampus.backend.service.user;

import com.softwarecampus.backend.exception.email.EmailNotVerifiedException;
import com.softwarecampus.backend.model.dto.user.SignupRequest;
import com.softwarecampus.backend.model.dto.user.AccountResponse;
import com.softwarecampus.backend.model.enums.VerificationType;
import com.softwarecampus.backend.service.email.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {
    
    private final EmailVerificationService verificationService;
    // ... 기타 의존성
    
    @Override
    @Transactional
    public AccountResponse signup(SignupRequest request) {
        String email = request.getEmail();
        
        // 1. 이메일 인증 완료 여부 확인 (새로 추가)
        boolean verified = verificationService.isEmailVerified(email, VerificationType.SIGNUP);
        if (!verified) {
            throw new EmailNotVerifiedException("이메일 인증이 완료되지 않았습니다");
        }
        
        // 2. 기존 회원가입 로직 실행
        // - 이메일 중복 체크
        // - 비밀번호 암호화
        // - 사용자 생성
        // - 토큰 발급
        // ...
        
        log.info("회원가입 완료 - email: {}", email);
        return accountResponse;
    }
}
```

**변경 사항:**
- `EmailVerificationService` 의존성 주입
- 회원가입 로직 실행 전 `isEmailVerified()` 체크
- 미인증 시 `EmailNotVerifiedException` 발생 → 403 응답

---

## 5️⃣ 비밀번호 재설정 Service 통합 (선택)

### `src/main/java/com/softwarecampus/backend/service/user/PasswordResetService.java`
```java
package com.softwarecampus.backend.service.user;

import com.softwarecampus.backend.exception.email.EmailNotVerifiedException;
import com.softwarecampus.backend.model.dto.user.PasswordResetRequest;
import com.softwarecampus.backend.model.enums.VerificationType;
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
    
    private final EmailVerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    // ... 기타 의존성 (UserRepository 등)
    
    /**
     * 비밀번호 재설정
     * 
     * @param request 이메일 및 새 비밀번호
     */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String email = request.getEmail();
        
        // 1. 이메일 인증 완료 여부 확인
        boolean verified = verificationService.isEmailVerified(
                email, 
                VerificationType.PASSWORD_RESET
        );
        
        if (!verified) {
            throw new EmailNotVerifiedException("이메일 인증이 완료되지 않았습니다");
        }
        
        // 2. 사용자 조회
        // User user = userRepository.findByEmail(email)
        //     .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));
        
        // 3. 비밀번호 변경
        // String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        // user.changePassword(encodedPassword);
        // userRepository.save(user);
        
        log.info("비밀번호 재설정 완료 - email: {}", email);
    }
}
```

### `src/main/java/com/softwarecampus/backend/model/dto/user/PasswordResetRequest.java`
```java
package com.softwarecampus.backend.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 요청
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String email;
    
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    private String newPassword;
}
```

**경로:** `src/main/java/com/softwarecampus/backend/service/user/`

---

## ✅ Phase 4 완료 체크리스트

### Controller
- [ ] `EmailVerificationController` 생성
- [ ] 회원가입 코드 발송 API (`/send-verification`)
- [ ] 회원가입 코드 검증 API (`/verify`)
- [ ] 비밀번호 재설정 코드 발송 API (`/send-reset-code`)
- [ ] 비밀번호 재설정 코드 검증 API (`/verify-reset`)
- [ ] `@Valid` 입력 검증 적용
- [ ] `VerificationType` 강제 설정 (보안)

### 예외 처리
- [ ] `GlobalExceptionHandler`에 이메일 예외 핸들러 추가
- [ ] `EmailSendException` 핸들러 (500)
- [ ] `EmailVerificationException` 핸들러 (400)
- [ ] `EmailNotVerifiedException` 핸들러 (403)
- [ ] `VerificationCodeExpiredException` 핸들러 (400)
- [ ] `TooManyAttemptsException` 핸들러 (429)
- [ ] `MethodArgumentNotValidException` 핸들러 (400)

### 기존 Service 통합
- [ ] `SignupServiceImpl`에 이메일 인증 체크 추가
- [ ] `PasswordResetService` 생성 (선택)
- [ ] `PasswordResetRequest` DTO 생성 (선택)

### API 테스트
- [ ] Postman/Insomnia로 4개 API 테스트
- [ ] 성공 케이스 테스트
- [ ] Validation 실패 테스트
- [ ] 재발송 쿨다운 테스트
- [ ] 코드 만료 테스트
- [ ] 5회 실패 차단 테스트

---

## 📌 다음 단계 (Phase 5)

- 단위 테스트 작성 (Service, Util)
- 통합 테스트 작성 (Controller, Repository)
- Mock 테스트 (이메일 발송)

---

## 🔍 API 테스트 예시 (Postman)

### 1. 회원가입 인증 코드 발송
```http
POST http://localhost:8080/api/auth/email/send-verification
Content-Type: application/json

{
  "email": "test@example.com"
}
```

**예상 응답 (200):**
```json
{
  "message": "인증 코드가 발송되었습니다",
  "expiresIn": 180
}
```

### 2. 회원가입 인증 코드 검증
```http
POST http://localhost:8080/api/auth/email/verify
Content-Type: application/json

{
  "email": "test@example.com",
  "code": "123456"
}
```

**예상 응답 (200):**
```json
{
  "message": "이메일 인증이 완료되었습니다"
}
```

### 3. 비밀번호 재설정 코드 발송
```http
POST http://localhost:8080/api/auth/email/send-reset-code
Content-Type: application/json

{
  "email": "test@example.com"
}
```

### 4. 비밀번호 재설정 코드 검증
```http
POST http://localhost:8080/api/auth/email/verify-reset
Content-Type: application/json

{
  "email": "test@example.com",
  "code": "654321"
}
```

---

## 🔐 보안 고려사항

### Controller 레벨
- `VerificationType`을 Controller에서 강제 설정 (클라이언트 조작 방지)
- `@Valid`로 입력 검증 (이메일 형식, 코드 6자리)

### 예외 핸들러 레벨
- 차단 시간을 응답에 포함 (`blockedUntil`)
- 민감한 에러 정보 노출 방지 (스택 트레이스 미포함)

### Service 통합 레벨
- 회원가입 전 반드시 이메일 인증 체크
- 비밀번호 재설정 전 반드시 이메일 인증 체크
