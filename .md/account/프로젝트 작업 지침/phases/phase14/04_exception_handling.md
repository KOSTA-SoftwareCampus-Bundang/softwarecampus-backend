# 4. Exception Handling (로그인)

**목표:** 로그인 인증 실패 예외 처리 구현

---

## 📂 생성 파일

```
src/main/java/com/softwarecampus/backend/
└─ exception/user/
   └─ InvalidCredentialsException.java
```

---

## 4.1 InvalidCredentialsException.java

**경로:** `exception/user/InvalidCredentialsException.java`

**설명:** 로그인 인증 실패 예외 (이메일 없음 또는 비밀번호 불일치)

```java
package com.softwarecampus.backend.exception.user;

/**
 * 로그인 인증 실패 예외
 * 
 * 발생 시나리오:
 * - 존재하지 않는 이메일로 로그인 시도
 * - 비밀번호 불일치
 * - 비활성화된 계정 (isActive = false)
 * - 미승인 ACADEMY 계정 (accountApproved = PENDING/REJECTED)
 * 
 * HTTP 상태 코드: 401 Unauthorized
 * 
 * @author 태윤
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
```

**핵심 포인트:**

### 1. RuntimeException 상속
- Spring의 `@Transactional` 롤백 트리거
- Checked Exception 불필요 (인증 실패는 복구 불가능)

### 2. 보안 원칙
- **이메일 존재 여부 유출 방지**: 
  - "존재하지 않는 이메일" ❌
  - "이메일 또는 비밀번호가 올바르지 않습니다" ✅
  
- **계정 상태별 메시지**:
  - 일반 인증 실패: "이메일 또는 비밀번호가 올바르지 않습니다"
  - 비활성화: "비활성화된 계정입니다"
  - 미승인: "승인 대기 중인 계정입니다"

---

## 4.2 GlobalExceptionHandler 확장

**경로:** `exception/GlobalExceptionHandler.java`

**설명:** InvalidCredentialsException을 401 Unauthorized로 매핑

### 추가할 메서드

```java
package com.softwarecampus.backend.exception;

import com.softwarecampus.backend.exception.user.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 기존 핸들러 유지 (DuplicateEmailException, InvalidInputException 등)
    
    /**
     * 로그인 인증 실패 예외 처리
     * 
     * @param ex InvalidCredentialsException
     * @return 401 Unauthorized + RFC 9457 ProblemDetail
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("인증 실패: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage()
        );
        
        problemDetail.setTitle("Unauthorized");
        
        return problemDetail;
    }
}
```

**핵심 로직:**

### 1. HTTP 상태 코드: 401 Unauthorized
```java
ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage())
```
- **401 Unauthorized**: 인증 실패 (자격증명 오류)
- **403 Forbidden**: 인증은 성공했지만 권한 부족 (로그인 후 접근 거부)

### 2. RFC 9457 ProblemDetail 응답
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "이메일 또는 비밀번호가 올바르지 않습니다",
  "instance": "/api/auth/login"
}
```

### 3. 로깅
```java
log.warn("인증 실패: {}", ex.getMessage());
```
- **warn 레벨**: 보안 이벤트 (무차별 대입 공격 탐지용)
- **메시지만 로깅**: 이메일은 Service Layer에서 이미 마스킹됨

---

## 📊 예외 처리 플로우

```text
LoginServiceImpl
    ↓
이메일 없음 or 비밀번호 틀림
    ↓
throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다")
    ↓
GlobalExceptionHandler
    ↓
@ExceptionHandler(InvalidCredentialsException.class)
    ↓
ProblemDetail (401 Unauthorized)
    ↓
클라이언트 수신
```

---

## 🧪 테스트 시나리오

### 1. 존재하지 않는 이메일

**요청:**
```json
{
  "email": "nonexistent@example.com",
  "password": "Password123!"
}
```

**응답 (401):**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "이메일 또는 비밀번호가 올바르지 않습니다",
  "instance": "/api/auth/login"
}
```

### 2. 비밀번호 불일치

**요청:**
```json
{
  "email": "user@example.com",
  "password": "WrongPassword"
}
```

**응답 (401):**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "이메일 또는 비밀번호가 올바르지 않습니다",
  "instance": "/api/auth/login"
}
```

### 3. 비활성화된 계정

**요청:**
```json
{
  "email": "inactive@example.com",
  "password": "Password123!"
}
```

**응답 (401):**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "비활성화된 계정입니다",
  "instance": "/api/auth/login"
}
```

### 4. 미승인 ACADEMY 계정

**요청:**
```json
{
  "email": "academy@example.com",
  "password": "Password123!"
}
```

**응답 (401):**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "승인 대기 중인 계정입니다",
  "instance": "/api/auth/login"
}
```

---

## 🔐 보안 Best Practice

### 1. 동일한 예외 메시지 사용
```java
// ❌ 나쁜 예: 정보 유출
if (!accountExists) {
    throw new InvalidCredentialsException("존재하지 않는 이메일입니다");
}
if (!passwordMatches) {
    throw new InvalidCredentialsException("비밀번호가 틀렸습니다");
}

// ✅ 좋은 예: 정보 유출 방지
if (!accountExists || !passwordMatches) {
    throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다");
}
```

### 2. 응답 시간 일정하게 유지
- `PasswordEncoder.matches()`는 BCrypt로 항상 일정 시간 소요
- Timing Attack 방지

### 3. 로그인 시도 횟수 제한 (향후 구현)
```java
// Phase 12.5에서 구현된 RateLimitService 활용
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    // Rate Limiting 체크
    rateLimitService.checkAndIncrementLoginAttempts(request.email());
    
    LoginResponse response = loginService.login(request);
    
    // 성공 시 카운터 리셋
    rateLimitService.resetLoginAttempts(request.email());
    
    return ResponseEntity.ok(response);
}
```

---

## 🔗 다음 단계

Exception Handling 완료 후:
1. **LoginServiceImplTest** 단위 테스트 작성 ([05_service_test.md](05_service_test.md))
2. **AuthControllerTest** 로그인 테스트 추가 ([06_controller_test.md](06_controller_test.md))
