# Phase 7: Controller Layer (회원가입 API)

**목표:** 회원가입 API 엔드포인트 구현 (RESTful)  
**담당자:** 태윤  
**상태:** 🚧 준비 중

---

## 📋 작업 개요

Phase 5(Service Layer)와 Phase 6(단위 테스트)를 기반으로 회원가입 API 엔드포인트를 구현합니다. RESTful API 원칙을 준수하며, Bean Validation, RFC 9457 ProblemDetail 표준을 적용합니다.

**API 원칙:**
- **RESTful**: HTTP 메서드(POST), 상태 코드(201, 400, 409), Location 헤더
- **Bean Validation**: `@Valid` + `@NotBlank`, `@Email`, `@Size`
- **ProblemDetail**: RFC 9457 표준 오류 응답
- **보안**: PII 로깅 제거, 비밀번호 평문 노출 방지
- **문서화**: OpenAPI (Swagger) 자동 생성

---

## 📂 생성 파일

```text
src/main/java/com/softwarecampus/backend/
└─ controller/user/
   └─ AuthController.java                 ✅ 회원가입 API Controller
```

---

## 🔧 구현 내용

### 1. AuthController.java

**경로:** `src/main/java/com/softwarecampus/backend/controller/user/AuthController.java`

**설명:** 회원가입 및 인증 관련 API 컨트롤러

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
     * @param request 회원가입 요청 (email, password, userName, phoneNumber, address, affiliation, position)
     * @return 201 Created + Location 헤더 + AccountResponse
     * 
     * @throws InvalidInputException 400 - 이메일 형식 오류 (RFC 5322, RFC 1035)
     * @throws DuplicateEmailException 409 - 이메일 중복
     * @throws InvalidInputException 400 - 전화번호 중복
     */
    @PostMapping("/signup")
    public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청");
        if (log.isDebugEnabled()) {
            log.debug("회원가입 요청 - affiliation: {}", 
                request.affiliation() != null ? "있음" : "없음");
        }
        
        AccountResponse response = signupService.signup(request);
        
        // Location 헤더 생성 (RESTful)
        URI location = URI.create("/api/v1/accounts/" + response.accountId());
        
        log.info("회원가입 성공 - accountId: {}", response.accountId());
        
        return ResponseEntity
            .created(location)
            .body(response);
    }
    
    /**
     * 이메일 중복 확인 API (선택적)
     * 
     * @param email 확인할 이메일
     * @return 200 OK - 사용 가능 여부
     */
    @GetMapping("/check-email")
    public ResponseEntity<MessageResponse> checkEmail(@RequestParam String email) {
        log.debug("이메일 중복 확인 요청");
        
        boolean available = signupService.isEmailAvailable(email);
        
        String message = available 
            ? "사용 가능한 이메일입니다." 
            : "이미 사용 중인 이메일입니다.";
        
        return ResponseEntity.ok(MessageResponse.of(message));
    }
}
```

**설계 포인트:**
1. **RESTful 원칙**:
   - POST /api/v1/auth/signup → 201 Created
   - Location 헤더: `/api/v1/accounts/{accountId}`
   - Body: `AccountResponse` (생성된 리소스 정보)

2. **Bean Validation**:
   - `@Valid`: SignupRequest 검증
   - `MethodArgumentNotValidException` → GlobalExceptionHandler (400)

3. **로깅 전략**:
   - INFO: 회원가입 성공 (accountId만)
   - DEBUG: affiliation 유무 (PII 제외)
   - PII 로깅 금지 (이메일 원본 노출 방지)

4. **예외 처리**:
   - Service Layer에서 발생
   - GlobalExceptionHandler에서 처리
   - ProblemDetail 응답 (RFC 9457)

---

## 📝 API 명세

### 1. POST /api/v1/auth/signup (회원가입)

**요청:**
```http
POST /api/v1/auth/signup HTTP/1.1
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123!",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구",
  "affiliation": "ABC학원",
  "position": "강사"
}
```

**응답 (201 Created):**
```http
HTTP/1.1 201 Created
Location: /api/v1/accounts/1
Content-Type: application/json

{
  "accountId": 1,
  "email": "user@example.com",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구",
  "affiliation": "ABC학원",
  "position": "강사",
  "accountType": "ACADEMY",
  "accountApproved": "APPROVED",
  "createdAt": "2024-01-01T12:00:00"
}
```

**에러 응답 (400 Bad Request - Bean Validation):**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "요청 본문에 유효하지 않은 필드가 있습니다.",
  "errors": {
    "email": "이메일 형식이 올바르지 않습니다.",
    "password": "비밀번호는 8자 이상이어야 합니다."
  }
}
```

**에러 응답 (400 Bad Request - RFC 5322 위반):**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/invalid-input",
  "title": "Invalid Input",
  "status": 400,
  "detail": "잘못된 이메일 형식입니다."
}
```

**에러 응답 (409 Conflict - 이메일 중복):**
```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/duplicate-email",
  "title": "Duplicate Email",
  "status": 409,
  "detail": "이미 사용 중인 이메일입니다."
}
```

**에러 응답 (400 Bad Request - 전화번호 중복):**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/invalid-input",
  "title": "Invalid Input",
  "status": 400,
  "detail": "이미 사용 중인 전화번호입니다."
}
```

---

### 2. GET /api/v1/auth/check-email (이메일 중복 확인)

**요청:**
```http
GET /api/v1/auth/check-email?email=user@example.com HTTP/1.1
```

**응답 (200 OK - 사용 가능):**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "사용 가능한 이메일입니다."
}
```

**응답 (200 OK - 사용 불가):**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "이미 사용 중인 이메일입니다."
}
```

---

## 🔗 SignupService 인터페이스 확장

**기존 파일 수정:** `service/user/signup/SignupService.java`

### isEmailAvailable() 메서드 추가

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
     * @throws InvalidInputException 이메일 형식 오류
     * @throws DuplicateEmailException 이메일 중복
     */
    AccountResponse signup(SignupRequest request);
    
    /**
     * 이메일 중복 확인
     * 
     * @param email 확인할 이메일
     * @return true: 사용 가능, false: 사용 불가
     */
    boolean isEmailAvailable(String email);
}
```

---

## 🔗 SignupServiceImpl 구현 확장

**기존 파일 수정:** `service/user/signup/SignupServiceImpl.java`

### isEmailAvailable() 메서드 구현

```java
@Override
public boolean isEmailAvailable(String email) {
    // 이메일 형식 검증
    if (!EmailUtils.isValidEmail(email)) {
        throw new InvalidInputException("잘못된 이메일 형식입니다.");
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

## 📊 의존성 관계도

```text
AuthController
    ↓
SignupService (인터페이스)
    ↓
SignupServiceImpl (구현체)
    ↓
    ├─ AccountRepository.existsByEmail(String)
    ├─ AccountRepository.save(Account)
    └─ PasswordEncoder.encode(String)

예외 처리 플로우:
Controller (Bean Validation 실패)
    ↓
MethodArgumentNotValidException
    ↓
GlobalExceptionHandler
    ↓
RFC 9457 ProblemDetail (400 Bad Request)

Controller → Service (예외 발생)
    ↓
InvalidInputException / DuplicateEmailException
    ↓
GlobalExceptionHandler
    ↓
RFC 9457 ProblemDetail (400 / 409)
```

---

## 📝 테스트 전략

### 1. 단위 테스트 (Phase 6 완료)
- SignupServiceImplTest (8개 테스트)
- ProfileServiceImplTest (6개 테스트)
- EmailUtilsTest (12개 테스트)

### 2. Controller 통합 테스트 (Phase 7)
- `AuthControllerTest.java` 작성
- `@WebMvcTest(AuthController.class)` 사용
- MockMvc로 HTTP 요청/응답 테스트
- Service Layer 모킹 (`@MockBean`)

**테스트 시나리오 (총 12개):**
1. ✅ POST /signup - 정상 회원가입 (201 Created)
2. ✅ POST /signup - Bean Validation 실패 (400)
3. ✅ POST /signup - 이메일 형식 오류 (400)
4. ✅ POST /signup - 이메일 중복 (409)
5. ✅ POST /signup - 전화번호 중복 (400)
6. ✅ POST /signup - Location 헤더 검증
7. ✅ POST /signup - AccountResponse 검증
8. ✅ GET /check-email - 사용 가능 (200)
9. ✅ GET /check-email - 사용 불가 (200)
10. ✅ GET /check-email - 이메일 형식 오류 (400)
11. ✅ 로깅 검증 (INFO, DEBUG)
12. ✅ PII 로깅 제거 검증

### 3. E2E 테스트 (Phase 8)
- Postman / REST Client 사용
- 실제 DB 연동 테스트
- 전체 플로우 검증

---

## 🎯 RESTful API 설계 원칙

### 1. HTTP 메서드
- **POST**: 리소스 생성 (회원가입)
- **GET**: 리소스 조회 (이메일 중복 확인)
- **PUT**: 리소스 전체 수정 (Phase 8 - 프로필 수정)
- **PATCH**: 리소스 부분 수정 (Phase 8 - 비밀번호 변경)
- **DELETE**: 리소스 삭제 (Phase 8 - 회원 탈퇴)

### 2. HTTP 상태 코드
- **200 OK**: 조회 성공 (GET)
- **201 Created**: 생성 성공 (POST) + Location 헤더
- **204 No Content**: 수정/삭제 성공 (PUT/DELETE)
- **400 Bad Request**: 요청 오류 (Bean Validation, 형식 오류)
- **404 Not Found**: 리소스 미존재
- **409 Conflict**: 리소스 충돌 (중복)
- **500 Internal Server Error**: 서버 오류

### 3. Location 헤더 (RFC 7231)
```http
HTTP/1.1 201 Created
Location: /api/v1/accounts/1
```
- 생성된 리소스의 URI 표시
- 클라이언트가 GET 요청으로 리소스 조회 가능

### 4. ProblemDetail (RFC 9457)
```json
{
  "type": "https://api.softwarecampus.com/problems/duplicate-email",
  "title": "Duplicate Email",
  "status": 409,
  "detail": "이미 사용 중인 이메일입니다."
}
```
- 표준화된 오류 응답 형식
- `type`: 문제 유형 URI
- `title`: 사람이 읽을 수 있는 제목
- `status`: HTTP 상태 코드
- `detail`: 구체적인 설명

---

## 🔒 보안 고려사항

### 1. PII 로깅 제거
```java
// ❌ 나쁜 예
log.info("회원가입 성공 - email: {}", response.email());

// ✅ 좋은 예
log.info("회원가입 성공 - accountId: {}", response.accountId());

// ✅ 디버그 레벨 (개발 환경만)
if (log.isDebugEnabled()) {
    log.debug("회원가입 요청 - affiliation: {}", 
        request.affiliation() != null ? "있음" : "없음");
}
```

### 2. 비밀번호 평문 노출 방지
- Controller → Service 전달 시 평문 (피할 수 없음)
- Service Layer에서 즉시 암호화
- 로그에 비밀번호 절대 출력 금지
- AccountResponse에 비밀번호 제외

### 3. 이메일 마스킹
```java
// Service Layer (로깅 시)
String maskedEmail = EmailUtils.maskEmail(request.email());
log.debug("회원가입 시도 - email: {}", maskedEmail);
// 출력: u****@example.com
```

### 4. CORS 설정 (Phase 8)
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

---

## ✅ 검증 방법

### 1. 컴파일 확인
```bash
mvn clean compile
```

### 2. Controller 통합 테스트 실행
```bash
# 전체 Controller 테스트
mvn test -Dtest=*ControllerTest

# AuthController만 테스트
mvn test -Dtest=AuthControllerTest
```

### 3. Postman / REST Client 테스트

**회원가입 성공 (201):**
```http
### 회원가입 - 일반 사용자
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "user1@example.com",
  "password": "password123!",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구"
}
```

**회원가입 성공 - 학원 계정 (201):**
```http
### 회원가입 - 학원 계정
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "teacher@example.com",
  "password": "password123!",
  "userName": "김선생",
  "phoneNumber": "010-9876-5432",
  "address": "서울시 서초구",
  "affiliation": "ABC학원",
  "position": "수학 강사"
}
```

**이메일 중복 (409):**
```http
### 이메일 중복
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "user1@example.com",
  "password": "password123!",
  "userName": "이순신"
}
```

**이메일 형식 오류 (400):**
```http
### 이메일 형식 오류
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "invalid-email",
  "password": "password123!",
  "userName": "세종대왕"
}
```

**이메일 중복 확인:**
```http
### 이메일 중복 확인 - 사용 가능
GET http://localhost:8080/api/v1/auth/check-email?email=newuser@example.com

### 이메일 중복 확인 - 사용 불가
GET http://localhost:8080/api/v1/auth/check-email?email=user1@example.com
```

### 4. 빌드 검증
```bash
mvn clean verify
```

---

## 📝 Phase 완료 기준

- [ ] **Controller 파일 생성**
  - [ ] `AuthController.java` 생성
  - [ ] `@RestController`, `@RequestMapping` 적용
  - [ ] `@RequiredArgsConstructor` (DI)

- [ ] **API 엔드포인트 구현**
  - [ ] POST /api/v1/auth/signup (회원가입)
  - [ ] GET /api/v1/auth/check-email (이메일 중복 확인)
  - [ ] `@Valid` Bean Validation 적용
  - [ ] Location 헤더 생성 (RESTful)

- [ ] **Service 인터페이스 확장**
  - [ ] `isEmailAvailable(String)` 메서드 추가
  - [ ] SignupServiceImpl 구현

- [ ] **로깅 및 보안**
  - [ ] PII 로깅 제거 (이메일 원본 노출 방지)
  - [ ] INFO 레벨: accountId만 로깅
  - [ ] DEBUG 레벨: 일반화된 정보만
  - [ ] 비밀번호 평문 로깅 금지

- [ ] **Controller 통합 테스트 (Phase 7)**
  - [ ] `AuthControllerTest.java` 작성 (12개 테스트)
  - [ ] `@WebMvcTest` + MockMvc 사용
  - [ ] Service Layer 모킹 (`@MockBean`)
  - [ ] HTTP 요청/응답 검증
  - [ ] Location 헤더 검증
  - [ ] ProblemDetail 검증

- [ ] **Postman 테스트**
  - [ ] 회원가입 성공 (일반/학원)
  - [ ] 이메일 형식 오류
  - [ ] 이메일 중복
  - [ ] 전화번호 중복
  - [ ] 이메일 중복 확인

- [ ] **문서화**
  - [ ] Phase 7 설계 문서 작성
  - [ ] API 명세서 작성
  - [ ] RESTful 원칙 정리
  - [ ] 보안 고려사항 정리

---

## 🔜 다음 단계

**Phase 8: 프로필 조회 API**
- `AccountController.java` 작성
- GET /api/v1/accounts/{accountId} (ID로 조회)
- GET /api/v1/accounts/email/{email} (이메일로 조회)
- HTTP 200 OK / 404 Not Found
- ProfileService 활용

**Phase 9: 통합 테스트 및 E2E 테스트**
- Spring Boot Test (`@SpringBootTest`)
- 실제 DB 연동 (TestContainers 또는 H2)
- 전체 플로우 검증
- Postman Collection 작성

**Phase 10: OpenAPI (Swagger) 문서 자동 생성**
- Springdoc OpenAPI 의존성 추가
- `@Operation`, `@ApiResponse` 애노테이션
- Swagger UI 활성화
- API 문서 자동 생성

---

## 📚 참고 자료

### Spring MVC 문서
- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [@RestController](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/RestController.html)
- [Bean Validation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)

### RESTful API 표준
- [RFC 7231 - HTTP/1.1 Semantics](https://www.rfc-editor.org/rfc/rfc7231)
- [RFC 9457 - Problem Details](https://www.rfc-editor.org/rfc/rfc9457.html)
- [REST API 디자인 가이드](https://restfulapi.net/)

### 테스트 문서
- [@WebMvcTest](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/autoconfigure/web/servlet/WebMvcTest.html)
- [MockMvc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/MockMvc.html)
- [Testing Spring Boot Applications](https://spring.io/guides/gs/testing-web)

---

## 📊 테스트 통계

**총 테스트 개수:** 38개
- Phase 6 단위 테스트: 26개
  - SignupServiceImplTest: 8개
  - ProfileServiceImplTest: 6개
  - EmailUtilsTest: 12개
- Phase 7 Controller 통합 테스트: 12개
  - AuthControllerTest: 12개

**커버리지 목표:**
- Line Coverage: 85% 이상
- Branch Coverage: 75% 이상
- Method Coverage: 90% 이상

**예상 소요 시간:** 2-3시간
- Controller 구현: 1시간
- Controller 테스트: 1시간
- Postman 테스트: 30분
