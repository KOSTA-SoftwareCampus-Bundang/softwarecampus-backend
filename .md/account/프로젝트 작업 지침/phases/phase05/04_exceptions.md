# 4. 도메인 예외

## InvalidInputException.java

**경로:** `exception/user/InvalidInputException.java`

**설명:** 잘못된 입력 (이메일 형식 오류 등) 시 발생하는 도메인 예외

```java
package com.softwarecampus.backend.exception.user;

/**
 * 잘못된 입력 예외
 * - 이메일 형식 오류 (RFC 5322, RFC 1035 위반)
 * - 닉네임 중복 등
 */
public class InvalidInputException extends RuntimeException {
    
    public InvalidInputException(String message) {
        super(message);
    }
    
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**설계 포인트:**
- `RuntimeException` 상속 (Unchecked Exception)
- 입력 검증 실패 시 명확한 예외 표현
- GlobalExceptionHandler에서 400 Bad Request 응답

---

## DuplicateEmailException.java

**경로:** `exception/user/DuplicateEmailException.java`

**설명:** 이메일 중복 시 발생하는 도메인 예외

```java
package com.softwarecampus.backend.exception.user;

/**
 * 이메일 중복 예외
 * - 회원가입 시 이미 존재하는 이메일로 가입 시도할 때 발생
 * - DB UNIQUE 제약 위반 시에도 발생 (동시성 안전)
 */
public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException(String message) {
        super(message);
    }
    
    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**설계 포인트:**
- `RuntimeException` 상속 (Unchecked Exception)
- 비즈니스 로직에서 발생하는 예외는 명시적으로 처리
- GlobalExceptionHandler에서 409 Conflict 응답

---

## AccountNotFoundException.java

**경로:** `exception/user/AccountNotFoundException.java`

**설명:** 계정 미존재 시 발생하는 도메인 예외

```java
package com.softwarecampus.backend.exception.user;

/**
 * 계정 미존재 예외
 * - ID 또는 이메일로 계정 조회 시 존재하지 않을 때 발생
 */
public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException(String message) {
        super(message);
    }
    
    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**설계 포인트:**
- 마이페이지 조회 시 사용 (Phase 18)
- 로그인 시 계정 존재 여부 체크 시 사용 (Phase 16)
- GlobalExceptionHandler에서 404 Not Found 응답

---

## 예외 처리 전략

### HTTP 상태 코드 매핑
| 예외 | 상태 코드 | 설명 |
|------|----------|------|
| `InvalidInputException` | 400 Bad Request | 입력 검증 실패 |
| `DuplicateEmailException` | 409 Conflict | 이메일 중복 |
| `AccountNotFoundException` | 404 Not Found | 계정 미존재 |

### RuntimeException 선택 이유
- Spring은 RuntimeException만 자동 롤백
- 비즈니스 예외는 필수 처리 불필요
- GlobalExceptionHandler에서 일괄 처리
