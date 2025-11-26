# Phase 2: Repository, Util, 이메일 템플릿

## 📋 작업 목표
- EmailVerification Repository 인터페이스 생성
- 인증 코드 생성 Util 클래스 구현
- HTML 이메일 템플릿 작성 (회원가입, 비밀번호 재설정)

---

## 1️⃣ Repository 생성

### `src/main/java/com/softwarecampus/backend/repository/EmailVerificationRepository.java`
```java
package com.softwarecampus.backend.repository;

import com.softwarecampus.backend.model.entity.EmailVerification;
import com.softwarecampus.backend.model.enums.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 이메일 인증 Repository
 */
@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    
    /**
     * 이메일과 타입으로 가장 최근 인증 레코드 조회
     * (생성 시간 기준 내림차순)
     */
    Optional<EmailVerification> findTopByEmailAndTypeOrderByCreatedAtDesc(
            String email, 
            VerificationType type
    );
    
    /**
     * 인증 완료된 레코드 존재 여부 확인
     * (회원가입 시 이메일 인증 완료 여부 체크용)
     */
    boolean existsByEmailAndTypeAndVerifiedTrue(
            String email, 
            VerificationType type
    );
    
    /**
     * 이메일과 타입으로 인증되지 않은 레코드 조회
     */
    Optional<EmailVerification> findByEmailAndTypeAndVerifiedFalse(
            String email, 
            VerificationType type
    );
    
    /**
     * 특정 시간 이전에 생성되고 인증 완료된 레코드 삭제
     * (배치 작업용 - 인증 완료 후 24시간 지난 데이터 삭제)
     */
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :threshold AND e.verified = true")
    void deleteByExpiresAtBeforeAndVerifiedTrue(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 특정 시간 이전에 생성되고 미인증 상태인 레코드 삭제
     * (배치 작업용 - 미인증 상태로 24시간 지난 데이터 삭제)
     */
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.createdAt < :threshold AND e.verified = false")
    void deleteByCreatedAtBeforeAndVerifiedFalse(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 이메일, 타입, 코드로 레코드 조회
     * (인증 코드 검증용)
     */
    Optional<EmailVerification> findByEmailAndTypeAndCode(
            String email, 
            VerificationType type, 
            String code
    );
}
```

**경로:** `src/main/java/com/softwarecampus/backend/repository/`

**주요 메서드:**
- `findTopByEmailAndTypeOrderByCreatedAtDesc`: 최근 인증 레코드 조회 (재발송 체크용)
- `existsByEmailAndTypeAndVerifiedTrue`: 인증 완료 여부 확인 (회원가입 허용 여부)
- `findByEmailAndTypeAndCode`: 코드 검증용
- `deleteByExpiresAtBeforeAndVerifiedTrue`: 배치 작업 - 인증 완료 데이터 삭제
- `deleteByCreatedAtBeforeAndVerifiedFalse`: 배치 작업 - 미인증 데이터 삭제

---

## 2️⃣ Util 클래스 생성

### `src/main/java/com/softwarecampus/backend/util/VerificationCodeGenerator.java`
```java
package com.softwarecampus.backend.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * 이메일 인증 코드 생성 유틸리티
 * - SecureRandom과 ByteBuffer를 사용한 암호학적으로 안전한 난수 생성
 */
public final class VerificationCodeGenerator {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int CODE_MAX = 1_000_000; // 0 ~ 999999
    
    private VerificationCodeGenerator() {
        throw new AssertionError("Utility 클래스는 인스턴스화할 수 없습니다");
    }
    
    /**
     * 6자리 숫자 인증 코드 생성
     * 
     * @return 000000 ~ 999999 범위의 6자리 문자열
     */
    public static String generateCode() {
        // SecureRandom으로 4바이트 생성
        byte[] randomBytes = new byte[4];
        SECURE_RANDOM.nextBytes(randomBytes);
        
        // ByteBuffer로 int 변환 (음수 방지를 위해 절댓값 사용)
        int randomInt = Math.abs(ByteBuffer.wrap(randomBytes).getInt());
        
        // 0 ~ 999999 범위로 제한
        int code = randomInt % CODE_MAX;
        
        // 6자리 문자열로 포맷 (앞자리 0 포함)
        return String.format("%06d", code);
    }
    
    /**
     * 코드 유효성 검증
     * 
     * @param code 검증할 코드
     * @return 유효하면 true
     */
    public static boolean isValidFormat(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return false;
        }
        
        return code.matches("^[0-9]{6}$");
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/util/`

**보안 특징:**
- `SecureRandom`: 예측 불가능한 난수 생성
- `ByteBuffer`: 바이트 배열을 정수로 안전하게 변환
- `Math.abs()`: 음수 방지
- `String.format("%06d")`: 6자리 고정 (앞자리 0 포함)

---

## 3️⃣ 이메일 HTML 템플릿

### `src/main/resources/templates/email/signup-verification.html`
```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>회원가입 인증 코드</title>
    <style>
        body {
            font-family: 'Malgun Gothic', '맑은 고딕', Arial, sans-serif;
            background-color: #f4f4f4;
            margin: 0;
            padding: 0;
        }
        .email-container {
            max-width: 600px;
            margin: 40px auto;
            background-color: #ffffff;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            overflow: hidden;
        }
        .email-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #ffffff;
            padding: 30px;
            text-align: center;
        }
        .email-header h1 {
            margin: 0;
            font-size: 24px;
            font-weight: bold;
        }
        .email-body {
            padding: 40px 30px;
            color: #333333;
            line-height: 1.6;
        }
        .email-body p {
            margin: 0 0 20px 0;
            font-size: 15px;
        }
        .verification-code {
            background-color: #f8f9fa;
            border: 2px dashed #667eea;
            border-radius: 8px;
            padding: 20px;
            text-align: center;
            margin: 30px 0;
        }
        .verification-code .code {
            font-size: 32px;
            font-weight: bold;
            color: #667eea;
            letter-spacing: 8px;
            font-family: 'Courier New', monospace;
        }
        .verification-code .expiry {
            font-size: 13px;
            color: #dc3545;
            margin-top: 10px;
            font-weight: bold;
        }
        .info-box {
            background-color: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 15px;
            margin: 20px 0;
            font-size: 14px;
            color: #856404;
        }
        .email-footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            font-size: 12px;
            color: #6c757d;
            border-top: 1px solid #dee2e6;
        }
        .email-footer a {
            color: #667eea;
            text-decoration: none;
        }
    </style>
</head>
<body>
    <div class="email-container">
        <!-- 헤더 -->
        <div class="email-header">
            <h1>🎓 소프트웨어캠퍼스</h1>
        </div>
        
        <!-- 본문 -->
        <div class="email-body">
            <p>안녕하세요,</p>
            <p>소프트웨어캠퍼스 회원가입을 위한 <strong>이메일 인증 코드</strong>를 안내드립니다.</p>
            
            <!-- 인증 코드 -->
            <div class="verification-code">
                <div class="code">${code}</div>
                <div class="expiry">⏰ 이 코드는 3분 후 만료됩니다</div>
            </div>
            
            <p>위 인증 코드를 회원가입 페이지에 입력해 주세요.</p>
            
            <!-- 안내 사항 -->
            <div class="info-box">
                ⚠️ <strong>주의사항</strong><br>
                • 본인이 요청하지 않은 경우 이 이메일을 무시하세요.<br>
                • 인증 코드는 타인에게 절대 공유하지 마세요.<br>
                • 5회 이상 잘못 입력 시 30분간 인증이 차단됩니다.
            </div>
        </div>
        
        <!-- 푸터 -->
        <div class="email-footer">
            <p>이 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해 주세요.</p>
            <p>&copy; 2025 소프트웨어캠퍼스. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
```

**경로:** `src/main/resources/templates/email/signup-verification.html`

**치환 변수:**
- `${code}`: 6자리 인증 코드 (예: 123456)

---

### `src/main/resources/templates/email/password-reset-verification.html`
```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>비밀번호 재설정 인증 코드</title>
    <style>
        body {
            font-family: 'Malgun Gothic', '맑은 고딕', Arial, sans-serif;
            background-color: #f4f4f4;
            margin: 0;
            padding: 0;
        }
        .email-container {
            max-width: 600px;
            margin: 40px auto;
            background-color: #ffffff;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            overflow: hidden;
        }
        .email-header {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
            color: #ffffff;
            padding: 30px;
            text-align: center;
        }
        .email-header h1 {
            margin: 0;
            font-size: 24px;
            font-weight: bold;
        }
        .email-body {
            padding: 40px 30px;
            color: #333333;
            line-height: 1.6;
        }
        .email-body p {
            margin: 0 0 20px 0;
            font-size: 15px;
        }
        .verification-code {
            background-color: #f8f9fa;
            border: 2px dashed #f5576c;
            border-radius: 8px;
            padding: 20px;
            text-align: center;
            margin: 30px 0;
        }
        .verification-code .code {
            font-size: 32px;
            font-weight: bold;
            color: #f5576c;
            letter-spacing: 8px;
            font-family: 'Courier New', monospace;
        }
        .verification-code .expiry {
            font-size: 13px;
            color: #dc3545;
            margin-top: 10px;
            font-weight: bold;
        }
        .security-notice {
            background-color: #f8d7da;
            border-left: 4px solid #dc3545;
            padding: 15px;
            margin: 20px 0;
            font-size: 14px;
            color: #721c24;
        }
        .info-box {
            background-color: #d1ecf1;
            border-left: 4px solid #17a2b8;
            padding: 15px;
            margin: 20px 0;
            font-size: 14px;
            color: #0c5460;
        }
        .email-footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            font-size: 12px;
            color: #6c757d;
            border-top: 1px solid #dee2e6;
        }
        .email-footer a {
            color: #f5576c;
            text-decoration: none;
        }
    </style>
</head>
<body>
    <div class="email-container">
        <!-- 헤더 -->
        <div class="email-header">
            <h1>🔐 소프트웨어캠퍼스</h1>
        </div>
        
        <!-- 본문 -->
        <div class="email-body">
            <p>안녕하세요,</p>
            <p>비밀번호 재설정을 위한 <strong>인증 코드</strong>를 안내드립니다.</p>
            
            <!-- 인증 코드 -->
            <div class="verification-code">
                <div class="code">${code}</div>
                <div class="expiry">⏰ 이 코드는 3분 후 만료됩니다</div>
            </div>
            
            <p>위 인증 코드를 비밀번호 재설정 페이지에 입력해 주세요.</p>
            
            <!-- 보안 경고 -->
            <div class="security-notice">
                🚨 <strong>보안 경고</strong><br>
                본인이 비밀번호 재설정을 요청하지 않았다면, 즉시 계정 보안을 확인하세요.<br>
                타인이 귀하의 계정에 접근을 시도할 수 있습니다.
            </div>
            
            <!-- 안내 사항 -->
            <div class="info-box">
                ℹ️ <strong>안내사항</strong><br>
                • 인증 코드는 타인에게 절대 공유하지 마세요.<br>
                • 5회 이상 잘못 입력 시 30분간 인증이 차단됩니다.<br>
                • 비밀번호 재설정 후 보안을 위해 로그아웃됩니다.
            </div>
        </div>
        
        <!-- 푸터 -->
        <div class="email-footer">
            <p>이 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해 주세요.</p>
            <p>&copy; 2025 소프트웨어캠퍼스. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
```

**경로:** `src/main/resources/templates/email/password-reset-verification.html`

**치환 변수:**
- `${code}`: 6자리 인증 코드 (예: 987654)

**디자인 차이점:**
- 헤더 색상: 빨강-핑크 그라디언트 (보안 경고 느낌)
- 보안 경고 박스 추가 (본인이 요청하지 않은 경우 대응 안내)

---

## 4️⃣ 템플릿 로더 Util (선택)

### `src/main/java/com/softwarecampus/backend/util/EmailTemplateLoader.java`
```java
package com.softwarecampus.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 이메일 HTML 템플릿 로더
 */
@Slf4j
@Component
public class EmailTemplateLoader {
    
    private static final String TEMPLATE_PATH = "templates/email/";
    
    /**
     * HTML 템플릿 파일 로드
     * 
     * @param templateName 템플릿 파일명 (예: "signup-verification.html")
     * @return HTML 문자열
     */
    public String loadTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH + templateName);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("이메일 템플릿 로드 실패: {}", templateName, e);
            throw new RuntimeException("이메일 템플릿을 불러올 수 없습니다", e);
        }
    }
    
    /**
     * 템플릿 변수 치환
     * 
     * @param template HTML 템플릿
     * @param placeholder 치환할 변수명 (예: "code")
     * @param value 치환할 값
     * @return 치환된 HTML 문자열
     */
    public String replaceVariable(String template, String placeholder, String value) {
        return template.replace("${" + placeholder + "}", value);
    }
}
```

**경로:** `src/main/java/com/softwarecampus/backend/util/`

**사용 예시:**
```java
String template = templateLoader.loadTemplate("signup-verification.html");
String html = templateLoader.replaceVariable(template, "code", "123456");
```

---

## ✅ Phase 2 완료 체크리스트

- [ ] `EmailVerificationRepository` 인터페이스 생성
  - [ ] 최근 레코드 조회 메서드
  - [ ] 인증 완료 여부 확인 메서드
  - [ ] 코드 검증 메서드
  - [ ] 배치 삭제 메서드 2개
- [ ] `VerificationCodeGenerator` Util 클래스 생성
  - [ ] `generateCode()` 메서드 (SecureRandom + ByteBuffer)
  - [ ] `isValidFormat()` 검증 메서드
- [ ] 이메일 HTML 템플릿 2개 생성
  - [ ] `signup-verification.html` (회원가입용)
  - [ ] `password-reset-verification.html` (비밀번호 재설정용)
- [ ] `EmailTemplateLoader` Util 클래스 생성 (선택)
  - [ ] `loadTemplate()` 메서드
  - [ ] `replaceVariable()` 메서드
- [ ] 코드 생성 테스트 (6자리 숫자 확인)
- [ ] 템플릿 로드 테스트 (변수 치환 확인)

---

## 📌 다음 단계 (Phase 3)

- EmailSendService 구현 (이메일 발송)
- EmailVerificationService 구현 (코드 생성, 검증, 보안 정책)
- JavaMailSender 설정 및 MIME 메시지 구성

---

## 🔍 참고사항

### Repository 메서드 네이밍 규칙
- `findTopBy...OrderBy...Desc`: 최근 1개 조회
- `existsBy...And...`: 존재 여부 Boolean 반환
- `deleteBy...And...`: 조건부 삭제 (배치용)

### SecureRandom vs Random
- ❌ `Random`: 예측 가능한 난수 (보안 취약)
- ✅ `SecureRandom`: 암호학적으로 안전한 난수 (인증 코드용)

### HTML 템플릿 스타일
- 인라인 CSS 사용 (대부분의 이메일 클라이언트 호환)
- 반응형 디자인 (max-width: 600px)
- 브랜드 컬러 일관성 유지
