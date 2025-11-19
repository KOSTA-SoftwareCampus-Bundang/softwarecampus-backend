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
import com.softwarecampus.backend.util.EmailUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@Validated
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
     * ⚠️ 보안 고려사항:
     * - Rate Limiting 필수 (이메일 열거 공격 방지)
     * - IP 기반 제한 권장: 60 req/min per IP
     * - 로깅 및 모니터링 필요
     * 
     * TODO Phase 8: Rate Limiter 구현
     * - Bucket4j + Redis 또는 Spring Cloud Gateway rate limiter
     * - IP 기반 제한: @RateLimit(permits=60, window=1, unit=MINUTES)
     * - 초과 시: 429 Too Many Requests 응답
     * 
     * @param email 확인할 이메일
     * @return 200 OK - 사용 가능 여부
     * 
     * @throws InvalidInputException 400 - 이메일 형식 오류
     * @throws ConstraintViolationException 400 - Bean Validation 실패 (@Email, @NotBlank, @Size)
     */
    @GetMapping("/check-email")
    public ResponseEntity<MessageResponse> checkEmail(
            @RequestParam 
            @NotBlank(message = "이메일은 필수입니다.") 
            @Size(max = 254, message = "이메일은 최대 254자까지 입력 가능합니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.") 
            String email) {
        log.info("이메일 중복 확인 요청: email={}", EmailUtils.maskEmail(email));
        
        boolean available = signupService.isEmailAvailable(email);
        
        String message = available 
            ? "사용 가능한 이메일입니다." 
            : "이미 사용 중인 이메일입니다.";
        
        log.info("이메일 중복 확인 결과 - available: {}", available);
        
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

### 5. 보안 고려사항 (Phase 8에서 구현 예정)

#### 5.1 Rate Limiting - 이메일 중복 확인 API

**보안 위험:**
- 이메일 열거 공격 (Email Enumeration)
- DoS 공격 (무제한 요청)
- 브루트 포스 공격

### ⚠️ Phase 8 대기 중 - 현재 엔드포인트 취약점 존재

현재 `/check-email` 엔드포인트는 Rate Limiting이 적용되지 않아 이메일 열거 공격에 노출되어 있습니다.

**임시 완화 조치 (Phase 8 이전):**
1. **게이트웨이/미들웨어 레벨 제한**: Nginx, API Gateway에서 IP 기반 제한
2. **IP 기반 로깅 및 차단**: 비정상 패턴 감지 시 수동 차단
3. **모니터링**: 비정상 요청 패턴 알림 설정

**Phase 8 구현 계획:**
- **목표**: IP 기반 Rate Limiting (60 req/min)
- **응답**: 429 Too Many Requests
- **구현 방식**: Phase 8 설계 시 결정 (Bucket4j, Spring Cloud Gateway, 또는 Custom Interceptor)
- **Note**: 현재는 설계/구현하지 않음

**Rate Limit 정책 (예정):**
- **임계값**: 60 req/min per IP
- **초과 시**: `429 Too Many Requests`
- **헤더 추가**:
  - `X-RateLimit-Limit: 60`
  - `X-RateLimit-Remaining: 45`
  - `X-RateLimit-Reset: 1699876543`

**모니터링:**
```java
// 로깅 레벨 변경: DEBUG → INFO (보안 감사용)
log.info("이메일 중복 확인 요청: email={}", EmailUtils.maskEmail(email));
```

**추가 방어 기법:**
- CAPTCHA 추가 (과도한 요청 시)
- 응답 시간 균일화 (Timing Attack 방지)
- IP 블랙리스트 관리

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
    if (!EmailUtils.isValidFormat(email)) {
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

**보안 참고:**
- 현재는 인증 없이 접근 가능 (Phase 7)
- Phase 8에서 Rate Limiting 추가 필수
- 로그 레벨: `DEBUG` → `INFO` (보안 감사)

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

### Phase 7 (현재)
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

### Phase 8 (보안 강화 - TODO)
- [ ] **Rate Limiting 구현 (필수)**
  - [ ] Bucket4j + Redis 설정
  - [ ] IP 기반 제한: 60 req/min
  - [ ] 429 Too Many Requests 응답
  - [ ] Rate limit 헤더 추가
- [ ] **모니터링 강화**
  - [ ] 이메일 중복 확인 로그: DEBUG → INFO
  - [ ] 이상 트래픽 알림 설정
  - [ ] IP 블랙리스트 관리
- [ ] **추가 방어 기법**
  - [ ] CAPTCHA 통합 (과도한 요청 시)
  - [ ] 응답 시간 균일화 (Timing Attack 방지)
  - [ ] 계정 잠금 정책 (연속 실패 시)

---

## 🔗 관련 문서

- [API 명세서](02_api_specification.md) - 요청/응답 예시
- [Controller 테스트](03_controller_test.md) - AuthControllerTest 구현
- [보안 & RESTful](04_security_restful.md) - Rate Limiting 상세 구현
- [보안 및 RESTful 원칙](04_security_restful.md) - 보안 가이드
