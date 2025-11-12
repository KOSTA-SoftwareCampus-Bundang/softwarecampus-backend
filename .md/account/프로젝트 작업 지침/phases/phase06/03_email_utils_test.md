# 3. EmailUtilsTest 구현

**경로:** `test/java/com/softwarecampus/backend/util/EmailUtilsTest.java`

**설명:** 이메일 형식 검증 및 마스킹 Utility 테스트

---

## 📋 테스트 개요

EmailUtils의 정적 메서드를 검증합니다:
- `isValidFormat(String)`: RFC 5322, RFC 1035 기반 이메일 형식 검증
- `maskEmail(String)`: 이메일 주소 마스킹 (PII 보호)

---

## 🔧 전체 코드

```java
package com.softwarecampus.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * EmailUtils 단위 테스트
 * 
 * 테스트 대상:
 * - isValidFormat(String): RFC 5322, RFC 1035 기반 이메일 검증
 * - maskEmail(String): 이메일 마스킹 (로깅용)
 * 
 * 특징:
 * - @ParameterizedTest: 여러 입력 값에 대해 동일 로직 테스트
 * - @ValueSource: 문자열 배열 입력
 */
@DisplayName("EmailUtils 단위 테스트")
class EmailUtilsTest {
    
    // ========== 이메일 형식 검증 ==========
    
    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "test.email@domain.co.kr",
        "admin123@company.com",
        "first.last@sub.domain.com",
        "email+tag@example.com"
    })
    @DisplayName("유효한 이메일 형식 - 성공")
    void isValidFormat_유효한형식(String email) {
        // When & Then
        assertThat(EmailUtils.isValidFormat(email)).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email",           // @ 없음
        "@example.com",            // 로컬 파트 없음
        "user@",                   // 도메인 없음
        "user@-invalid.com",       // 도메인 레이블 하이픈 시작 (RFC 1035 위반)
        "user@invalid-.com",       // 도메인 레이블 하이픈 종료 (RFC 1035 위반)
        "user@.com",               // 빈 도메인 레이블
        "user@domain..com",        // 연속 점
        "user @example.com",       // 공백 포함
        "user@domain .com",        // 도메인 공백
        ""                         // 빈 문자열
    })
    @DisplayName("유효하지 않은 이메일 형식 - 실패")
    void isValidFormat_유효하지않은형식(String email) {
        // When & Then
        assertThat(EmailUtils.isValidFormat(email)).isFalse();
    }
    
    @Test
    @DisplayName("null 이메일 - 실패")
    void isValidFormat_null() {
        // When & Then
        assertThat(EmailUtils.isValidFormat(null)).isFalse();
    }
    
    // ========== 이메일 마스킹 ==========
    
    @Test
    @DisplayName("이메일 마스킹 - 기본 형식")
    void maskEmail_기본형식() {
        // Given
        String email = "user@example.com";
        
        // When
        String masked = EmailUtils.maskEmail(email);
        
        // Then
        assertThat(masked).isEqualTo("u***@example.com");
    }
    
    @Test
    @DisplayName("이메일 마스킹 - 짧은 로컬 파트 (2글자)")
    void maskEmail_짧은로컬파트() {
        // Given
        String email = "ab@example.com";
        
        // When
        String masked = EmailUtils.maskEmail(email);
        
        // Then
        assertThat(masked).isEqualTo("a***@example.com");
    }
    
    @Test
    @DisplayName("이메일 마스킹 - 1글자 로컬 파트")
    void maskEmail_1글자() {
        // Given
        String email = "a@example.com";
        
        // When
        String masked = EmailUtils.maskEmail(email);
        
        // Then
        assertThat(masked).isEqualTo("a***@example.com");
    }
    
    @Test
    @DisplayName("이메일 마스킹 - 긴 로컬 파트")
    void maskEmail_긴로컬파트() {
        // Given
        String email = "verylongemail@example.com";  // 13글자 → 13/3=4 → min(4,3)=3
        
        // When
        String masked = EmailUtils.maskEmail(email);
        
        // Then
        assertThat(masked).isEqualTo("ver***@example.com");  // 3글자 표시
    }
    
    @Test
    @DisplayName("이메일 마스킹 - 점 포함")
    void maskEmail_점포함() {
        // Given
        String email = "first.last@example.com";  // 10글자 → 10/3=3
        
        // When
        String masked = EmailUtils.maskEmail(email);
        
        // Then
        assertThat(masked).isEqualTo("fir***@example.com");  // 3글자 표시
    }
    
    @Test
    @DisplayName("이메일 마스킹 - null 입력")
    void maskEmail_null() {
        // When & Then
        assertThatThrownBy(() -> EmailUtils.maskEmail(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이메일은 null일 수 없습니다.");
    }
    
    @Test
    @DisplayName("이메일 마스킹 - @ 없는 잘못된 형식")
    void maskEmail_잘못된형식() {
        // Given
        String invalidEmail = "invalid-email";
        
        // When & Then
        assertThatThrownBy(() -> EmailUtils.maskEmail(invalidEmail))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("유효하지 않은 이메일 형식입니다.");
    }
    
    @Test
    @DisplayName("이메일 마스킹 - 빈 문자열")
    void maskEmail_빈문자열() {
        // When & Then
        assertThatThrownBy(() -> EmailUtils.maskEmail(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이메일은 비어있을 수 없습니다.");
    }
}
```

---

## 📊 테스트 시나리오

### 이메일 검증 (`isValidFormat`)

| 번호 | 입력 | 검증 규칙 | 예상 결과 |
|------|------|----------|----------|
| 1 | `user@example.com` | 정상 형식 | `true` |
| 2 | `test.email@domain.co.kr` | 점, 다중 레벨 | `true` |
| 3 | `email+tag@example.com` | + 기호 | `true` |
| 4 | `invalid-email` | @ 없음 | `false` |
| 5 | `user@-invalid.com` | 하이픈 시작 (RFC 1035) | `false` |
| 6 | `user@invalid-.com` | 하이픈 종료 (RFC 1035) | `false` |
| 7 | `user@.com` | 빈 레이블 | `false` |
| 8 | `user@domain..com` | 연속 점 | `false` |
| 9 | `null` | null 입력 | `false` |
| 10 | `""` | 빈 문자열 | `false` |

### 이메일 마스킹 (`maskEmail`)

**마스킹 규칙:** 로컬 파트 길이의 1/3, 최소 1자, 최대 3자 표시

| 번호 | 입력 | 로컬 파트 길이 | 표시 글자 수 | 출력 | 계산 |
|------|------|--------------|------------|------|------|
| 1 | `user@example.com` | 4 | 1 | `u***@example.com` | 4/3=1 |
| 2 | `ab@example.com` | 2 | 1 | `a***@example.com` | 2/3=0 → max(1,0)=1 |
| 3 | `a@example.com` | 1 | 1 | `a***@example.com` | 1/3=0 → max(1,0)=1 |
| 4 | `verylongemail@example.com` | 13 | 3 | `ver***@example.com` | 13/3=4 → min(4,3)=3 |
| 5 | `first.last@example.com` | 10 | 3 | `fir***@example.com` | 10/3=3 |
| 6 | `null` | - | - | 예외 발생 | IllegalArgumentException |
| 7 | `invalid-email` | - | - | 예외 발생 | @ 없음 |
| 8 | `""` | - | - | 예외 발생 | 빈 문자열 |

---

## 🎯 검증 포인트

### 1. @ParameterizedTest
```java
@ParameterizedTest
@ValueSource(strings = {"email1@test.com", "email2@test.com"})
void testMultipleInputs(String email) {
    assertThat(EmailUtils.isValidEmail(email)).isTrue();
}
```

**장점:**
- 하나의 테스트 메서드로 여러 입력 검증
- 코드 중복 제거
- 새로운 케이스 추가 용이

### 2. RFC 5322 검증
```java
// @ 필수
"invalid-email" → false

// 로컬 파트, 도메인 파트 모두 필수
"@example.com" → false
"user@" → false
```

### 3. RFC 1035 검증 (도메인 레이블)
```java
// 하이픈 시작/종료 불가
"user@-invalid.com" → false
"user@invalid-.com" → false

// 빈 레이블 불가
"user@.com" → false
"user@domain..com" → false
```

### 4. 마스킹 규칙
```java
// 로컬 파트 길이의 1/3, 최소 1자, 최대 3자 표시
// visibleChars = Math.max(1, Math.min(localPart.length() / 3, 3))

"user@example.com" → "u***@example.com"        // 4/3=1글자
"verylongemail@example.com" → "ver***@example.com"  // 13/3=4 → min(4,3)=3글자
"first.last@example.com" → "fir***@example.com"     // 10/3=3글자

// 1글자인 경우도 첫 글자 표시
"a@example.com" → "a***@example.com"           // max(1, 1/3)=1글자
```

---

## 📝 주요 패턴

### @ParameterizedTest 사용법
```java
@ParameterizedTest
@ValueSource(strings = {"input1", "input2", "input3"})
void testWithMultipleValues(String input) {
    // 각 입력값에 대해 반복 실행
    assertThat(someMethod(input)).isTrue();
}
```

### 예외 검증 (null, 빈 문자열)
```java
assertThatThrownBy(() -> EmailUtils.maskEmail(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("이메일은 null일 수 없습니다.");
```

### Boolean 반환 검증
```java
// true/false 직접 검증
assertThat(EmailUtils.isValidFormat(email)).isTrue();
assertThat(EmailUtils.isValidFormat(invalidEmail)).isFalse();
```

---

## 🔍 RFC 표준 참고

### RFC 5322 (Internet Message Format)
- 이메일 주소 기본 형식: `local-part@domain`
- 로컬 파트: 점(.), 하이픈(-), 밑줄(_), + 등 허용
- @ 기호 필수

### RFC 1035 (Domain Names)
- 도메인 레이블: 알파벳, 숫자, 하이픈 허용
- 하이픈으로 시작/종료 불가
- 빈 레이블 불가 (연속 점 불가)

---

## ✅ 완료 체크리스트

- [ ] `@ParameterizedTest` 사용
- [ ] `@ValueSource` 다중 입력
- [ ] 유효한 이메일 형식 (5개 이상)
- [ ] 유효하지 않은 형식 (10개 이상)
- [ ] RFC 5322 위반 케이스
- [ ] RFC 1035 위반 케이스
- [ ] 마스킹 규칙 (1글자, 2글자, 긴 이메일)
- [ ] null, 빈 문자열 예외 처리
- [ ] AssertJ 사용 (isTrue, isFalse, isEqualTo)

---

## 🔗 관련 문서

- [Mockito 패턴](04_mockito_patterns.md) - 테스트 패턴 총정리
- [SignupServiceImplTest](01_signup_service_test.md) - 이메일 검증 사용 예시
