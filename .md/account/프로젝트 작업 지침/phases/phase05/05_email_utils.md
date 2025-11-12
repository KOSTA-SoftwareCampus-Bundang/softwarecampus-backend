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
    
    /**
     * RFC 5322 + RFC 1035 이메일 정규식
     * 
     * 구조: localPart@domainPart
     * - localPart: [a-zA-Z0-9._%+-]+ (영문자, 숫자, 특수문자)
     * - domainPart: (label\.)+tld
     *   - label: 영문자/숫자로 시작, 중간에만 하이픈, 영문자/숫자로 끝
     *   - tld: 영문자 2~63자 (RFC 1035 섹션 2.3.1)
     * 
     * 예시:
     * - ✅ user@example.com
     * - ✅ user@sub-domain.example.technology (10자 TLD)
     * - ❌ user@-invalid.com (시작 하이픈)
     * - ❌ user@test-.com (끝 하이픈)
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@" +
        "(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)*" +
        "[a-zA-Z]{2,63}$"
    );
    
    /**
     * 이메일 형식 검증
     * 
     * @param email 검증할 이메일
     * @return 유효 여부
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 이메일 마스킹 (PII 보호)
     * 
     * @param email 마스킹할 이메일
     * @return 마스킹된 이메일 (예: u****@example.com)
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        
        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domainPart = parts[1];
        
        if (localPart.length() <= 1) {
            return "*@" + domainPart;
        }
        
        return localPart.charAt(0) + "****@" + domainPart;
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
isValidEmail("user@example.com")                    → true
isValidEmail("user@sub-domain.example.technology")  → true

// ❌ 무효한 이메일
isValidEmail("user@-invalid.com")   → false (시작 하이픈)
isValidEmail("user@test-.com")      → false (끝 하이픈)
isValidEmail("user@test..com")      → false (연속 점)
```

### 마스킹 예시
```java
maskEmail("user@example.com")      → "u****@example.com"
maskEmail("a@test.com")            → "*@test.com"
maskEmail("invalid")               → "***"
maskEmail(null)                    → "***"
```

---

## CodeRabbit 리뷰 반영

### 하이픈 위치 검증 강화
- 도메인 레이블 시작/끝에 하이픈 불허
- RFC 1035 섹션 2.3.1 준수
- 정규식 패턴 개선으로 엣지 케이스 처리
