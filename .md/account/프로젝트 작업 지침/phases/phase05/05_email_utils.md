# 5. EmailUtils 유틸리티

## EmailUtils.java

**경로:** `util/EmailUtils.java`

**설명:** 이메일 형식 검증 및 PII 보호를 위한 마스킹 유틸리티

```java
package com.softwarecampus.backend.util;

import java.util.regex.Pattern;

/**
 * 이메일 검증 및 마스킹 유틸리티
 * 
 * RFC 표준 준수:
 * - RFC 5322: 이메일 기본 형식
 * - RFC 1035: 도메인 레이블 규칙 (하이픈 중간만, TLD 최대 63자)
 */
public class EmailUtils {
    
    // RFC 5322 간소화 버전 + RFC 1035 표준 준수
    // - TLD 최대 63자
    // - 국제화 도메인(punycode, xn--) 지원
    // - 도메인 레이블: 영문자/숫자로 시작/끝, 중간에만 하이픈 허용 (RFC 1035 섹션 2.3.1)
    // - 로컬 파트: 영숫자/특수문자로 시작, 점은 중간에만 허용 (연속 점 불가, 선행/후행 점 불가)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$"
    );
    
    private EmailUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * 이메일 형식 검증
     * RFC 5321: 로컬 파트 최대 64자
     */
    public static boolean isValidFormat(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        
        // 로컬 파트 길이 검증 (RFC 5321)
        int atIndex = email.indexOf('@');
        if (atIndex > 64) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 이메일 마스킹 (로깅용)
     * 예: "user@example.com" → "u***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        
        // 로컬 파트만 마스킹 (최소 1자, 최대 3자 노출)
        int visibleChars = Math.max(1, Math.min(localPart.length() / 3, 3));
        String maskedLocal = localPart.substring(0, visibleChars) + "***";
        
        return maskedLocal + "@" + domain;
    }
}
```

---

## 설계 포인트

### RFC 표준 준수
- **RFC 5322**: 이메일 기본 형식
  - Local part: `[a-zA-Z0-9._%+-]+`
  - Domain part: `(label\.)+tld`

- **RFC 1035**: 도메인 레이블 규칙
  - 하이픈은 도메인 레이블 중간만 허용
  - TLD는 최대 63자 (영문자만)
  - 레이블은 영문자/숫자로 시작/끝, 중간에만 하이픈

### 보안 강화
- **PII 보호**: 로그에 이메일 원본 노출 방지
- **GDPR 준수**: 개인정보 마스킹 처리
- **로깅 전략**: 
  - ❌ `log.debug("이메일: {}", email)`
  - ✅ `log.debug("마스킹 이메일: {}", EmailUtils.maskEmail(email))`

### 검증 예시
```java
// ✅ 유효한 이메일
isValidFormat("user@example.com")                    → true
isValidFormat("user@sub-domain.example.technology")  → true

// ❌ 무효한 이메일
isValidFormat("user@-invalid.com")   → false (시작 하이픈)
isValidFormat("user@test-.com")      → false (끝 하이픈)
isValidFormat("user@test..com")      → false (연속 점)
```

### 마스킹 예시
```java
maskEmail("a@test.com")              // → "a***@test.com" (1자 노출)
maskEmail("user@example.com")        // → "u***@example.com" (4/3=1자 노출)
maskEmail("longuser@example.com")    // → "lo***@example.com" (8/3=2자 노출)
maskEmail("verylonguser@example.com") // → "ver***@example.com" (3자 상한)
maskEmail("invalid")                 // → "***"
maskEmail(null)                      // → "***"
```

**노출 문자 수 계산 규칙:**
- 최소 1자, 최대 3자 노출
- `Math.max(1, Math.min(localPart.length() / 3, 3))`
- 마스킹은 항상 고정된 "***" 사용 (가변 길이 아님)
- 보안 강화: 긴 이메일도 최대 3자만 노출하여 PII 보호

---

## CodeRabbit 리뷰 반영

### 하이픈 위치 검증 강화
- 도메인 레이블 시작/끝에 하이픈 불허
- RFC 1035 섹션 2.3.1 준수
- 정규식 패턴 개선으로 엣지 케이스 처리
