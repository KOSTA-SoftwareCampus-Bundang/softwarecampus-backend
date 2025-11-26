# Phase 1: 기본 설정 및 기초 구조

## 📋 작업 목표
- Spring Boot Mail 의존성 추가
- SMTP 설정 파일 구성
- 엔티티 및 DTO 생성
- 기본 상수 및 Enum 정의

---

## 1️⃣ 의존성 추가

### `pom.xml`
```xml
<!-- Spring Boot Mail Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

**위치:** `<dependencies>` 섹션 내부 추가

---

## 2️⃣ SMTP 설정

### `src/main/resources/application.properties`
```properties
# ========================================
# SMTP Email Configuration
# ========================================
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_APP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Email Verification Settings
email.verification.code-length=6
email.verification.expiry-minutes=3
email.verification.max-attempts=5
email.verification.block-duration-minutes=30
email.verification.resend-cooldown-seconds=60
```

### `.env` (프로젝트 루트)
```env
MAIL_USERNAME=your-email@gmail.com
MAIL_APP_PASSWORD=your-16-digit-app-password
```

**⚠️ 주의사항:**
- `.env` 파일은 `.gitignore`에 추가 필수
- Gmail 앱 비밀번호 생성: https://myaccount.google.com/apppasswords
- 2단계 인증 활성화 필요

---

## 3️⃣ Enum 정의

### `src/main/java/com/softwarecampus/backend/model/enums/VerificationType.java`
```java
package com.softwarecampus.backend.model.enums;

/**
 * 이메일 인증 타입
 */
public enum VerificationType {
    SIGNUP("회원가입"),
    PASSWORD_RESET("비밀번호 재설정");
    
    private final String description;
    
    VerificationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/model/enums/`

---

## 4️⃣ Entity 생성

### `src/main/java/com/softwarecampus/backend/model/entity/EmailVerification.java`
```java
package com.softwarecampus.backend.model.entity;

import com.softwarecampus.backend.model.enums.VerificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 이메일 인증 엔티티
 * - 회원가입 및 비밀번호 재설정 시 이메일 인증 코드 관리
 */
@Entity
@Table(
    name = "email_verification",
    indexes = {
        @Index(name = "idx_email_type", columnList = "email, type"),
        @Index(name = "idx_expires_at", columnList = "expires_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 인증 대상 이메일
     */
    @Column(nullable = false, length = 100)
    private String email;
    
    /**
     * 6자리 인증 코드 (000000 ~ 999999)
     */
    @Column(nullable = false, length = 6)
    private String code;
    
    /**
     * 인증 타입 (SIGNUP, PASSWORD_RESET)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationType type;
    
    /**
     * 인증 완료 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
    
    /**
     * 인증 시도 횟수
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;
    
    /**
     * 계정 차단 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean blocked = false;
    
    /**
     * 차단 해제 시간
     */
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;
    
    /**
     * 코드 만료 시간 (생성 후 3분)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * 생성 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 인증 완료 시간
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    /**
     * 인증 시도 증가
     */
    public void incrementAttempts() {
        this.attempts++;
    }
    
    /**
     * 계정 차단 설정
     */
    public void block(int blockDurationMinutes) {
        this.blocked = true;
        this.blockedUntil = LocalDateTime.now().plusMinutes(blockDurationMinutes);
    }
    
    /**
     * 인증 완료 처리
     */
    public void markAsVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }
    
    /**
     * 차단 상태 확인
     */
    public boolean isBlocked() {
        if (!blocked) {
            return false;
        }
        
        if (blockedUntil != null && LocalDateTime.now().isAfter(blockedUntil)) {
            // 차단 시간이 지나면 자동 해제
            this.blocked = false;
            this.blockedUntil = null;
            this.attempts = 0;
            return false;
        }
        
        return true;
    }
    
    /**
     * 코드 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/model/entity/`

---

## 5️⃣ DTO 생성

### `src/main/java/com/softwarecampus/backend/model/dto/email/EmailVerificationRequest.java`
```java
package com.softwarecampus.backend.model.dto.email;

import com.softwarecampus.backend.model.enums.VerificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이메일 인증 코드 발송 요청
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationRequest {
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String email;
    
    // 컨트롤러에서 자동 설정되므로 클라이언트는 보내지 않아도 됨
    // Phase 4에서 각 엔드포인트가 타입을 자동으로 설정함
    private VerificationType type;
}
```

**⚠️ 설계 변경 사항 (Phase 4):**
- `type` 필드는 클라이언트가 보내지 않음
- 각 API 엔드포인트(`/send-verification`, `/send-reset-code`)가 서버에서 타입을 강제 설정
- 이를 통해 클라이언트가 잘못된 타입을 보내는 것을 방지

### `src/main/java/com/softwarecampus/backend/model/dto/email/EmailVerificationCodeRequest.java`
```java
package com.softwarecampus.backend.model.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이메일 인증 코드 검증 요청
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationCodeRequest {
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String email;
    
    @NotBlank(message = "인증 코드는 필수입니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다")
    private String code;
}
```

### `src/main/java/com/softwarecampus/backend/model/dto/email/EmailVerificationResponse.java`
```java
package com.softwarecampus.backend.model.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이메일 인증 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationResponse {
    
    private String message;
    private Integer expiresIn; // 만료까지 남은 시간(초)
    private Integer remainingAttempts; // 남은 시도 횟수
    
    public static EmailVerificationResponse success(String message) {
        return EmailVerificationResponse.builder()
                .message(message)
                .build();
    }
    
    public static EmailVerificationResponse withExpiry(String message, int expiresIn) {
        return EmailVerificationResponse.builder()
                .message(message)
                .expiresIn(expiresIn)
                .build();
    }
    
    public static EmailVerificationResponse withAttempts(String message, int remainingAttempts) {
        return EmailVerificationResponse.builder()
                .message(message)
                .remainingAttempts(remainingAttempts)
                .build();
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/model/dto/email/`

---

## 6️⃣ 상수 정의

### `src/main/java/com/softwarecampus/backend/common/constants/EmailConstants.java`
```java
package com.softwarecampus.backend.common.constants;

/**
 * 이메일 관련 상수
 */
public final class EmailConstants {
    
    private EmailConstants() {
        throw new AssertionError("상수 클래스는 인스턴스화할 수 없습니다");
    }
    
    // 이메일 발신자 정보
    public static final String SENDER_EMAIL = "noreply@softwarecampus.com";
    public static final String SENDER_NAME = "소프트웨어캠퍼스";
    
    // 인증 코드 설정
    public static final int CODE_LENGTH = 6;
    public static final int CODE_MIN = 0;
    public static final int CODE_MAX = 999999;
    
    // 만료 시간
    public static final int EXPIRY_MINUTES = 3;
    public static final int EXPIRY_SECONDS = EXPIRY_MINUTES * 60; // 180초
    
    // 보안 설정
    public static final int MAX_ATTEMPTS = 5;
    public static final int BLOCK_DURATION_MINUTES = 30;
    public static final int RESEND_COOLDOWN_SECONDS = 60;
    
    // 이메일 제목
    public static final String SUBJECT_SIGNUP = "[소프트웨어캠퍼스] 회원가입 인증 코드";
    public static final String SUBJECT_PASSWORD_RESET = "[소프트웨어캠퍼스] 비밀번호 재설정 인증 코드";
}
```

**경로:** `src/main/java/com/softwarecampus/backend/common/constants/`

---

## 7️⃣ 예외 클래스 생성

### `src/main/java/com/softwarecampus/backend/exception/email/EmailSendException.java`
```java
package com.softwarecampus.backend.exception.email;

/**
 * 이메일 발송 실패 예외
 */
public class EmailSendException extends RuntimeException {
    
    public EmailSendException(String message) {
        super(message);
    }
    
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### `src/main/java/com/softwarecampus/backend/exception/email/EmailVerificationException.java`
```java
package com.softwarecampus.backend.exception.email;

/**
 * 이메일 인증 관련 예외
 */
public class EmailVerificationException extends RuntimeException {
    
    public EmailVerificationException(String message) {
        super(message);
    }
}
```

### `src/main/java/com/softwarecampus/backend/exception/email/EmailNotVerifiedException.java`
```java
package com.softwarecampus.backend.exception.email;

/**
 * 이메일 미인증 예외
 */
public class EmailNotVerifiedException extends RuntimeException {
    
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
```

### `src/main/java/com/softwarecampus/backend/exception/email/VerificationCodeExpiredException.java`
```java
package com.softwarecampus.backend.exception.email;

/**
 * 인증 코드 만료 예외
 */
public class VerificationCodeExpiredException extends RuntimeException {
    
    public VerificationCodeExpiredException(String message) {
        super(message);
    }
}
```

### `src/main/java/com/softwarecampus/backend/exception/email/TooManyAttemptsException.java`
```java
package com.softwarecampus.backend.exception.email;

import java.time.LocalDateTime;

/**
 * 인증 시도 횟수 초과 예외
 */
public class TooManyAttemptsException extends RuntimeException {
    
    private final LocalDateTime blockedUntil;
    
    public TooManyAttemptsException(String message, LocalDateTime blockedUntil) {
        super(message);
        this.blockedUntil = blockedUntil;
    }
    
    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/exception/email/`

---

## ✅ Phase 1 완료 체크리스트

- [ ] `pom.xml`에 `spring-boot-starter-mail` 의존성 추가
- [ ] `application.properties`에 SMTP 설정 추가
- [ ] `.env` 파일 생성 및 Gmail 계정 정보 설정
- [ ] `.gitignore`에 `.env` 추가
- [ ] `VerificationType` Enum 생성
- [ ] `EmailVerification` Entity 생성
- [ ] `EmailVerificationRequest` DTO 생성
- [ ] `EmailVerificationCodeRequest` DTO 생성
- [ ] `EmailVerificationResponse` DTO 생성
- [ ] `EmailConstants` 상수 클래스 생성
- [ ] 이메일 관련 예외 클래스 5개 생성
- [ ] Maven 프로젝트 빌드 성공 확인

---

## 📌 다음 단계 (Phase 2)

- Repository 인터페이스 생성
- 인증 코드 생성 Util 클래스
- 이메일 HTML 템플릿 작성
