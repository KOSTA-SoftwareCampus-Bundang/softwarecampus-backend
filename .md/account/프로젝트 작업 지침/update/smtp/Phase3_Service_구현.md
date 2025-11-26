# Phase 3: Service 구현

## 📋 작업 목표
- EmailSendService 구현 (SMTP 이메일 발송)
- EmailVerificationService 구현 (코드 생성, 검증, 보안 정책)
- 재발송 쿨다운, 시도 횟수 제한, 차단 로직 구현

---

## 1️⃣ EmailSendService 인터페이스

### `src/main/java/com/softwarecampus/backend/service/email/EmailSendService.java`
```java
package com.softwarecampus.backend.service.email;

import com.softwarecampus.backend.model.enums.VerificationType;

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
}
```

**경로:** `src/main/java/com/softwarecampus/backend/service/email/`

---

## 2️⃣ EmailSendService 구현체

### `src/main/java/com/softwarecampus/backend/service/email/EmailSendServiceImpl.java`
```java
package com.softwarecampus.backend.service.email;

import com.softwarecampus.backend.common.constants.EmailConstants;
import com.softwarecampus.backend.exception.email.EmailSendException;
import com.softwarecampus.backend.model.enums.VerificationType;
import com.softwarecampus.backend.util.EmailTemplateLoader;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendServiceImpl implements EmailSendService {
    
    private final JavaMailSender mailSender;
    private final EmailTemplateLoader templateLoader;
    
    @Override
    public void sendVerificationCode(String to, String code, VerificationType type) {
        try {
            MimeMessage message = createMessage(to, code, type);
            mailSender.send(message);
            log.info("이메일 발송 성공 - to: {}, type: {}", to, type);
        } catch (MessagingException e) {
            log.error("이메일 발송 실패 - to: {}, type: {}, error: {}", to, type, e.getMessage());
            throw new EmailSendException("이메일 발송에 실패했습니다", e);
        }
    }
    
    /**
     * MIME 메시지 생성
     */
    private MimeMessage createMessage(String to, String code, VerificationType type) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(EmailConstants.SENDER_EMAIL, EmailConstants.SENDER_NAME);
        helper.setTo(to);
        helper.setSubject(getSubject(type));
        helper.setText(getHtmlContent(code, type), true); // HTML 모드
        
        return message;
    }
    
    /**
     * 이메일 제목 가져오기
     */
    private String getSubject(VerificationType type) {
        return switch (type) {
            case SIGNUP -> EmailConstants.SUBJECT_SIGNUP;
            case PASSWORD_RESET -> EmailConstants.SUBJECT_PASSWORD_RESET;
        };
    }
    
    /**
     * HTML 본문 생성
     */
    private String getHtmlContent(String code, VerificationType type) {
        String templateName = switch (type) {
            case SIGNUP -> "signup-verification.html";
            case PASSWORD_RESET -> "password-reset-verification.html";
        };
        
        String template = templateLoader.loadTemplate(templateName);
        return templateLoader.replaceVariable(template, "code", code);
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/service/email/`

**주요 기능:**
- `JavaMailSender`로 MIME 메시지 발송
- HTML 템플릿 로드 및 변수 치환 (`${code}`)
- 발송 성공/실패 로그 기록
- `MessagingException` 처리 및 커스텀 예외 변환

---

## 3️⃣ EmailVerificationService 인터페이스

### `src/main/java/com/softwarecampus/backend/service/email/EmailVerificationService.java`
```java
package com.softwarecampus.backend.service.email;

import com.softwarecampus.backend.model.dto.email.EmailVerificationCodeRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationResponse;

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
     * 인증 코드 검증
     * 
     * @param request 이메일 및 인증 코드
     * @return 검증 결과
     */
    EmailVerificationResponse verifyCode(EmailVerificationCodeRequest request);
    
    /**
     * 이메일 인증 완료 여부 확인
     * 
     * @param email 확인할 이메일
     * @param type 인증 타입
     * @return 인증 완료 여부
     */
    boolean isEmailVerified(String email, com.softwarecampus.backend.model.enums.VerificationType type);
}
```

**경로:** `src/main/java/com/softwarecampus/backend/service/email/`

---

## 4️⃣ EmailVerificationService 구현체

### `src/main/java/com/softwarecampus/backend/service/email/EmailVerificationServiceImpl.java`
```java
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
import com.softwarecampus.backend.util.VerificationCodeGenerator;
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
        String code = VerificationCodeGenerator.generateCode();
        
        // 4. DB 저장
        EmailVerification verification = createVerification(email, code, type);
        verificationRepository.save(verification);
        
        // 5. 이메일 발송
        emailSendService.sendVerificationCode(email, code, type);
        
        log.info("인증 코드 발송 완료 - email: {}, type: {}", email, type);
        
        return EmailVerificationResponse.withExpiry(
            "인증 코드가 발송되었습니다",
            EmailConstants.EXPIRY_SECONDS
        );
    }
    
    @Override
    @Transactional
    public EmailVerificationResponse verifyCode(EmailVerificationCodeRequest request) {
        String email = request.getEmail();
        String code = request.getCode();
        
        // 1. 최근 인증 레코드 조회
        EmailVerification verification = verificationRepository
            .findTopByEmailAndTypeOrderByCreatedAtDesc(email, VerificationType.SIGNUP)
            .orElseThrow(() -> new EmailVerificationException("인증 요청 기록이 없습니다"));
        
        // 2. 차단 상태 체크
        if (verification.isBlocked()) {
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
        
        log.info("이메일 인증 성공 - email: {}", email);
        
        return EmailVerificationResponse.success("이메일 인증이 완료되었습니다");
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isEmailVerified(String email, VerificationType type) {
        return verificationRepository.existsByEmailAndTypeAndVerifiedTrue(email, type);
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
        
        if (recent.isPresent() && recent.get().isBlocked()) {
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
```

**경로:** `src/main/java/com/softwarecampus/backend/service/email/`

**주요 로직:**

### `sendVerificationCode()` - 인증 코드 발송
1. **재발송 쿨다운 체크** (60초)
   - 마지막 발송 후 60초 이내면 예외 발생
2. **차단 상태 체크**
   - 5회 실패로 차단된 경우 예외 발생
3. **코드 생성** (`VerificationCodeGenerator`)
4. **DB 저장** (만료 시간 = 현재 + 3분)
5. **이메일 발송** (`EmailSendService`)

### `verifyCode()` - 인증 코드 검증
1. **레코드 조회** (최근 인증 요청)
2. **차단 상태 체크**
3. **만료 체크** (3분 초과 시 예외)
4. **중복 인증 체크** (이미 완료된 경우)
5. **코드 일치 검증**
   - 불일치: 시도 횟수 증가 → 5회 초과 시 30분 차단
   - 일치: `verified = true`, `verifiedAt` 설정
6. **성공 응답**

### `isEmailVerified()` - 인증 완료 여부 확인
- 회원가입 시 이메일 인증 완료 여부 체크용

---

## 5️⃣ 비밀번호 재설정용 Service 확장

### `EmailVerificationServiceImpl.java`에 메서드 추가
```java
/**
 * 비밀번호 재설정 인증 코드 검증
 * (기존 verifyCode와 동일하지만 VerificationType.PASSWORD_RESET 사용)
 */
@Transactional
public EmailVerificationResponse verifyResetCode(EmailVerificationCodeRequest request) {
    String email = request.getEmail();
    String code = request.getCode();
    
    // 1. 최근 인증 레코드 조회 (PASSWORD_RESET 타입)
    EmailVerification verification = verificationRepository
        .findTopByEmailAndTypeOrderByCreatedAtDesc(email, VerificationType.PASSWORD_RESET)
        .orElseThrow(() -> new EmailVerificationException("인증 요청 기록이 없습니다"));
    
    // 2. 차단 상태 체크
    if (verification.isBlocked()) {
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
    
    log.info("비밀번호 재설정 인증 성공 - email: {}", email);
    
    return EmailVerificationResponse.success("인증이 완료되었습니다. 새 비밀번호를 설정하세요");
}
```

**인터페이스에도 추가:**
```java
// EmailVerificationService.java
EmailVerificationResponse verifyResetCode(EmailVerificationCodeRequest request);
```

---

## 6️⃣ 배치 스케줄러 구현

### `src/main/java/com/softwarecampus/backend/scheduler/EmailVerificationCleanupScheduler.java`
```java
package com.softwarecampus.backend.scheduler;

import com.softwarecampus.backend.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 이메일 인증 데이터 정리 스케줄러
 * - 매일 새벽 2시 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {
    
    private final EmailVerificationRepository verificationRepository;
    
    /**
     * 만료된 인증 데이터 삭제
     * - 인증 완료 후 24시간 지난 데이터
     * - 미인증 상태로 24시간 지난 데이터
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    @Transactional
    public void cleanupExpiredVerifications() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        
        try {
            // 인증 완료 후 24시간 지난 데이터 삭제
            verificationRepository.deleteByExpiresAtBeforeAndVerifiedTrue(threshold);
            
            // 미인증 상태로 24시간 지난 데이터 삭제
            verificationRepository.deleteByCreatedAtBeforeAndVerifiedFalse(threshold);
            
            log.info("만료된 이메일 인증 데이터 정리 완료 - threshold: {}", threshold);
        } catch (Exception e) {
            log.error("이메일 인증 데이터 정리 실패", e);
        }
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/scheduler/`

### Application에 `@EnableScheduling` 추가
```java
// src/main/java/com/softwarecampus/backend/Application.java
package com.softwarecampus.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // 스케줄링 활성화
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## ✅ Phase 3 완료 체크리스트

### Service 구현
- [ ] `EmailSendService` 인터페이스 생성
- [ ] `EmailSendServiceImpl` 구현
  - [ ] `sendVerificationCode()` - MIME 메시지 발송
  - [ ] HTML 템플릿 로드 및 변수 치환
  - [ ] 발송 성공/실패 로그
- [ ] `EmailVerificationService` 인터페이스 생성
- [ ] `EmailVerificationServiceImpl` 구현
  - [ ] `sendVerificationCode()` - 코드 생성 및 발송
  - [ ] `verifyCode()` - 회원가입 코드 검증
  - [ ] `verifyResetCode()` - 비밀번호 재설정 코드 검증
  - [ ] `isEmailVerified()` - 인증 완료 여부 확인
  - [ ] 재발송 쿨다운 체크 (60초)
  - [ ] 차단 상태 체크
  - [ ] 시도 횟수 증가 및 차단 로직

### 보안 정책
- [ ] 재발송 60초 쿨다운 구현
- [ ] 5회 실패 시 30분 차단 구현
- [ ] 코드 만료 3분 체크
- [ ] 차단 자동 해제 로직 (`isBlocked()` 메서드)

### 배치 작업
- [ ] `EmailVerificationCleanupScheduler` 생성
- [ ] 매일 새벽 2시 실행 설정 (`@Scheduled`)
- [ ] 인증 완료 후 24시간 지난 데이터 삭제
- [ ] 미인증 상태 24시간 지난 데이터 삭제
- [ ] `Application.java`에 `@EnableScheduling` 추가

### 테스트
- [ ] 이메일 발송 테스트 (실제 Gmail SMTP)
- [ ] 코드 생성 및 검증 테스트
- [ ] 재발송 쿨다운 테스트
- [ ] 5회 실패 차단 테스트
- [ ] 만료 코드 검증 실패 테스트

---

## 📌 다음 단계 (Phase 4)

- Controller 구현 (REST API 엔드포인트)
- 글로벌 예외 핸들러 구현
- API 응답 형식 통일

---

## 🔍 참고사항

### 트랜잭션 관리
- `@Transactional`: DB 작업 원자성 보장
- `readOnly = true`: 읽기 전용 최적화

### 로그 레벨
- `log.info`: 정상 플로우 (발송 성공, 인증 성공)
- `log.error`: 예외 상황 (발송 실패, 데이터 정리 실패)

### 보안 고려사항
- 차단 시간은 `LocalDateTime`으로 저장 (서버 재시작 시에도 유지)
- 차단 해제는 자동 처리 (`isBlocked()` 메서드에서 시간 체크)
- 코드는 DB에만 저장 (Redis 미사용)
