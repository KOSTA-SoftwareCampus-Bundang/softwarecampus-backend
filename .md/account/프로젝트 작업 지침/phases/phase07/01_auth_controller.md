# 1. AuthController 구현

**경로:** `src/main/java/com/softwarecampus/backend/controller/user/AuthController.java`

**설명:** 회원가입 및 인증 관련 API 컨트롤러

---

## 🔧 전체 코드

```java
package com.softwarecampus.backend.controller.user;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.MessageResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.service.user.signup.SignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 회원가입 및 인증 API 컨트롤러
 * 
 * 엔드포인트:
 * - POST /api/v1/auth/signup: 회원가입
 * - GET /api/v1/auth/check-email: 이메일 중복 확인
 * 
 * RESTful 원칙:
 * - HTTP 201 Created + Location 헤더 (리소스 URI)
 * - Bean Validation (@Valid)
 * - RFC 9457 ProblemDetail 오류 응답
 * 
 * @author 태윤
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final SignupService signupService;
    
    /**
     * 회원가입 API
     * 
     * @param request 회원가입 요청 (email, password, userName, phoneNumber, 
     *                address, affiliation, position, accountType, academyId)
     * @return 201 Created + Location 헤더 + AccountResponse
     * 
     * @throws InvalidInputException 400 - 이메일 형식 오류 (RFC 5322, RFC 1035)
     * @throws DuplicateEmailException 409 - 이메일 중복
     * @throws InvalidInputException 400 - 전화번호 중복
     * @throws InvalidInputException 400 - ADMIN 계정 회원가입 시도
     * @throws InvalidInputException 400 - ACADEMY 타입 academyId 누락
     */
    @PostMapping("/signup")
    public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청 - accountType: {}", request.accountType());
        if (log.isDebugEnabled()) {
            log.debug("회원가입 요청 - affiliation: {}, academyId: {}", 
                request.affiliation() != null ? "있음" : "없음",
                request.academyId() != null ? "있음" : "없음");
        }
        
        AccountResponse response = signupService.signup(request);
        
        // Location 헤더 생성 (RESTful)
        URI location = URI.create("/api/v1/accounts/" + response.id());
        
        log.info("회원가입 성공 - accountId: {}, accountType: {}, accountApproved: {}", 
            response.id(), response.accountType(), response.accountApproved());
        
        return ResponseEntity
            .created(location)
            .body(response);
    }
    
    /**
     * 이메일 중복 확인 API
     * 
     * @param email 확인할 이메일
     * @return 200 OK - 사용 가능 여부
     * 
     * @throws InvalidInputException 400 - 이메일 형식 오류
     */
    @GetMapping("/check-email")
    public ResponseEntity<MessageResponse> checkEmail(@RequestParam String email) {
        log.debug("이메일 중복 확인 요청");
        
        boolean available = signupService.isEmailAvailable(email);
        
        String message = available 
            ? "사용 가능한 이메일입니다." 
            : "이미 사용 중인 이메일입니다.";
        
        log.debug("이메일 중복 확인 결과 - available: {}", available);
        
        return ResponseEntity.ok(MessageResponse.of(message));
    }
}
```

---

## 📋 설계 포인트

### 1. RESTful 원칙

```java
// POST 회원가입 → 201 Created
return ResponseEntity
    .created(location)  // Location 헤더
    .body(response);    // 생성된 리소스 정보
```

**Location 헤더:**
- `/api/v1/accounts/{accountId}`
- 생성된 리소스의 URI
- 클라이언트가 GET 요청으로 리소스 조회 가능

### 2. Bean Validation

```java
@PostMapping("/signup")
public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request)
```

- `@Valid`: SignupRequest의 Bean Validation 검증
- 검증 실패 → `MethodArgumentNotValidException`
- GlobalExceptionHandler에서 처리 → 400 Bad Request

### 3. 로깅 전략

```java
// INFO 레벨: accountId, accountType, accountApproved (PII 없음)
log.info("회원가입 성공 - accountId: {}, accountType: {}, accountApproved: {}", 
    response.id(), response.accountType(), response.accountApproved());

// DEBUG 레벨: 일반화된 정보만 (있음/없음)
if (log.isDebugEnabled()) {
    log.debug("회원가입 요청 - affiliation: {}, academyId: {}", 
        request.affiliation() != null ? "있음" : "없음",
        request.academyId() != null ? "있음" : "없음");
}
```

**PII 로깅 금지:**
- ❌ 이메일 원본
- ❌ 비밀번호
- ❌ 전화번호
- ❌ 주소
- ✅ accountId, accountType, accountApproved

### 4. 예외 처리

```java
/**
 * @throws InvalidInputException 400 - 이메일 형식 오류 (RFC 5322, RFC 1035)
 * @throws DuplicateEmailException 409 - 이메일 중복
 * @throws InvalidInputException 400 - 전화번호 중복
 * @throws InvalidInputException 400 - ADMIN 계정 회원가입 시도
 * @throws InvalidInputException 400 - ACADEMY 타입 academyId 누락
 */
```

- Service Layer에서 예외 발생
- GlobalExceptionHandler에서 처리
- RFC 9457 ProblemDetail 응답

---

## 🔗 Service 확장

### SignupService 인터페이스 수정

**파일:** `service/user/signup/SignupService.java`

```java
package com.softwarecampus.backend.service.user.signup;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;

/**
 * 회원가입 서비스 인터페이스
 */
public interface SignupService {
    
    /**
     * 회원가입 처리
     * 
     * @param request 회원가입 요청
     * @return 생성된 계정 정보
     * @throws InvalidInputException 이메일 형식 오류, ADMIN 차단, ACADEMY academyId 누락
     * @throws DuplicateEmailException 이메일 중복
     */
    AccountResponse signup(SignupRequest request);
    
    /**
     * 이메일 중복 확인
     * 
     * @param email 확인할 이메일
     * @return true: 사용 가능, false: 사용 불가
     * @throws InvalidInputException 이메일 형식 오류
     */
    boolean isEmailAvailable(String email);
}
```

---

### SignupServiceImpl 구현 추가

**파일:** `service/user/signup/SignupServiceImpl.java`

```java
@Override
public boolean isEmailAvailable(String email) {
    // 이메일 형식 검증
    if (!EmailUtils.isValidEmail(email)) {
        throw new InvalidInputException("올바른 이메일 형식이 아닙니다.");
    }
    
    // 중복 확인
    return !accountRepository.existsByEmail(email);
}
```

**설계 포인트:**
- 이메일 형식 검증 (RFC 5322, RFC 1035)
- `existsByEmail()` 호출 (Repository)
- 반환: `true` (사용 가능), `false` (중복)

---

## 📝 MessageResponse DTO

**파일:** `dto/user/MessageResponse.java`

```java
package com.softwarecampus.backend.dto.user;

/**
 * 단순 메시지 응답 DTO
 * 
 * @param message 응답 메시지
 */
public record MessageResponse(String message) {
    
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
```

**사용 예시:**
```java
return ResponseEntity.ok(MessageResponse.of("사용 가능한 이메일입니다."));
```

---

## 🎯 RESTful API 설계

### HTTP 메서드 선택

| 작업 | HTTP 메서드 | 엔드포인트 | 상태 코드 |
|------|-------------|-----------|-----------|
| 회원가입 | POST | /api/v1/auth/signup | 201 Created |
| 이메일 중복 확인 | GET | /api/v1/auth/check-email | 200 OK |

### Location 헤더 생성

```java
URI location = URI.create("/api/v1/accounts/" + response.id());

return ResponseEntity
    .created(location)  // Location: /api/v1/accounts/1
    .body(response);
```

**RFC 7231 준수:**
- 201 Created 응답 시 Location 헤더 필수
- 생성된 리소스의 URI 표시
- 클라이언트가 즉시 조회 가능

---

## ✅ 구현 체크리스트

- [ ] `AuthController.java` 생성
- [ ] `@RestController`, `@RequestMapping` 적용
- [ ] `@RequiredArgsConstructor` DI
- [ ] POST /signup 엔드포인트
- [ ] GET /check-email 엔드포인트
- [ ] `@Valid` Bean Validation
- [ ] Location 헤더 생성
- [ ] PII 로깅 제거
- [ ] INFO/DEBUG 로깅 분리
- [ ] `isEmailAvailable()` Service 메서드 추가
- [ ] `MessageResponse` DTO 생성

---

## 🔗 관련 문서

- [API 명세서](02_api_specification.md) - 요청/응답 예시
- [Controller 테스트](03_controller_test.md) - AuthControllerTest 구현
- [보안 및 RESTful 원칙](04_security_restful.md) - 보안 가이드
