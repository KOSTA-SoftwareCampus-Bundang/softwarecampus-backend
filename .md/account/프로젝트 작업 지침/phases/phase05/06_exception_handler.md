# 6. GlobalExceptionHandler 수정

**기존 파일 수정:** `exception/GlobalExceptionHandler.java`

도메인 예외 핸들러 추가 및 RFC 9457 ProblemDetail 적용

---

## 추가된 핸들러

### 1. InvalidInputException 핸들러

```java
/**
 * 잘못된 입력값 예외 처리
 * HTTP 400 Bad Request
 */
@ExceptionHandler(InvalidInputException.class)
public ProblemDetail handleInvalidInputException(InvalidInputException ex) {
    log.warn("Invalid input detected for a request");
    if (log.isDebugEnabled()) {
        log.debug("InvalidInputException details: {}", ex.getMessage());
    }
    
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "입력값이 올바르지 않습니다."  // 고정된 일반화 메시지
    );
    problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/invalid-input"));
    problemDetail.setTitle("Invalid Input");
    
    return problemDetail;
}
```

---

### 2. DuplicateEmailException 핸들러

```java
/**
 * 이메일 중복 예외 처리
 * HTTP 409 Conflict
 */
@ExceptionHandler(DuplicateEmailException.class)
public ProblemDetail handleDuplicateEmailException(DuplicateEmailException ex) {
    log.warn("Email duplicate detected for a request");
    if (log.isDebugEnabled()) {
        log.debug("DuplicateEmailException details", ex);
    }
    
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "이메일이 이미 등록되었습니다."
    );
    problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/duplicate-email"));
    problemDetail.setTitle("Duplicate Email");
    
    return problemDetail;
}
```

---

### 3. AccountNotFoundException 핸들러

```java
/**
 * 계정 미존재 예외 처리
 * HTTP 404 Not Found
 */
@ExceptionHandler(AccountNotFoundException.class)
public ProblemDetail handleAccountNotFoundException(AccountNotFoundException ex) {
    log.warn("Account not found for a request");
    if (log.isDebugEnabled()) {
        log.debug("AccountNotFoundException details", ex);
    }
    
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        "요청한 계정을 찾을 수 없습니다."
    );
    problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/account-not-found"));
    problemDetail.setTitle("Account Not Found");
    
    return problemDetail;
}
```

---

## RFC 9457 ProblemDetail 준수

### 표준 필드
- `type`: 문제 유형을 나타내는 URI
- `title`: 간단한 설명
- `status`: HTTP 상태 코드
- `detail`: 구체적인 오류 사유

### 응답 예시

**InvalidInputException (400)**
```json
{
  "type": "https://api.softwarecampus.com/problems/invalid-input",
  "title": "Invalid Input",
  "status": 400,
  "detail": "올바른 이메일 형식이 아닙니다."
}
```

**DuplicateEmailException (409)**
```json
{
  "type": "https://api.softwarecampus.com/problems/duplicate-email",
  "title": "Duplicate Email",
  "status": 409,
  "detail": "이메일이 이미 등록되었습니다."
}
```

**AccountNotFoundException (404)**
```json
{
  "type": "https://api.softwarecampus.com/problems/account-not-found",
  "title": "Account Not Found",
  "status": 404,
  "detail": "요청한 계정을 찾을 수 없습니다."
}
```

---

## 보안 강화

### PII 로깅 제거
- ❌ `log.warn("이메일 중복: {}", email)`
- ✅ `log.warn("Email duplicate detected for a request")`

### 로깅 전략
- **WARN 레벨**: 일반화된 메시지 (PII 없음)
- **DEBUG 레벨**: 상세 스택 트레이스 (개발 환경만)

### 일반화된 메시지
- 공격자에게 시스템 내부 정보 노출 방지
- "이메일이 이미 등록되었습니다" (구체적 이메일 미노출)
- "요청한 계정을 찾을 수 없습니다" (계정 ID 미노출)
