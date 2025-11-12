# 4. 보안 및 RESTful 원칙

**보안 가이드, RESTful API 설계, Postman 테스트**

---

## 🔒 보안 고려사항

### 1. PII 로깅 제거

**PII (Personally Identifiable Information):** 개인 식별 정보
- 이메일 원본
- 비밀번호
- 전화번호
- 주소
- 실명

---

#### ❌ 나쁜 예

```java
@PostMapping("/signup")
public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request) {
    log.info("회원가입 요청 - email: {}", request.email());  // ❌ 이메일 원본 노출
    log.info("비밀번호: {}", request.password());           // ❌❌❌ 절대 금지!
    
    AccountResponse response = signupService.signup(request);
    
    log.info("회원가입 성공 - email: {}", response.email());  // ❌ 이메일 원본 노출
    
    return ResponseEntity.created(location).body(response);
}
```

---

#### ✅ 좋은 예

```java
@PostMapping("/signup")
public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request) {
    log.info("회원가입 요청 - accountType: {}", request.accountType());  // ✅ PII 없음
    
    if (log.isDebugEnabled()) {
        log.debug("회원가입 요청 - affiliation: {}, academyId: {}", 
            request.affiliation() != null ? "있음" : "없음",
            request.academyId() != null ? "있음" : "없음");  // ✅ 일반화된 정보
    }
    
    AccountResponse response = signupService.signup(request);
    
    log.info("회원가입 성공 - accountId: {}, accountType: {}, accountApproved: {}", 
        response.id(), response.accountType(), response.accountApproved());  // ✅ PII 없음
    
    return ResponseEntity.created(location).body(response);
}
```

---

### 2. 로깅 레벨 전략

| 레벨 | 용도 | 로깅 내용 | 프로덕션 |
|------|------|----------|----------|
| ERROR | 에러 | 예외 메시지, 스택 트레이스 | ✅ |
| WARN | 경고 | 비정상적이지만 복구 가능한 상황 | ✅ |
| INFO | 정보 | 비즈니스 흐름 (accountId, accountType) | ✅ |
| DEBUG | 디버그 | 일반화된 정보 (있음/없음) | ❌ (개발만) |
| TRACE | 상세 | 상세한 실행 흐름 | ❌ (개발만) |

---

### 3. 이메일 마스킹

**Service Layer에서 마스킹 후 로깅:**

```java
@Override
public AccountResponse signup(SignupRequest request) {
    // 이메일 마스킹 (로깅용)
    String maskedEmail = EmailUtils.maskEmail(request.email());
    log.debug("회원가입 시도 - email: {}", maskedEmail);
    // 출력: u***@example.com
    
    // 실제 비즈니스 로직
    Account account = Account.builder()
        .email(request.email())  // 실제 저장은 원본
        .build();
    
    return AccountResponse.from(account);
}
```

---

### 4. 비밀번호 보안

```java
// ✅ 좋은 예
@Override
public AccountResponse signup(SignupRequest request) {
    // 비밀번호 즉시 암호화
    String encodedPassword = passwordEncoder.encode(request.password());
    
    Account account = Account.builder()
        .password(encodedPassword)  // 암호화된 비밀번호 저장
        .build();
    
    // 비밀번호는 절대 로깅하지 않음
    return AccountResponse.from(account);
}

// ❌ 나쁜 예
log.info("비밀번호: {}", request.password());  // ❌❌❌ 절대 금지!
```

---

### 5. CORS 설정 (Phase 8)

**파일:** `config/WebConfig.java`

```java
package com.softwarecampus.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")  // 프론트엔드 주소
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 🎯 RESTful API 설계 원칙

### 1. HTTP 메서드 의미

| 메서드 | CRUD | 멱등성 | 안전성 | 용도 |
|--------|------|--------|--------|------|
| GET | Read | ✅ | ✅ | 리소스 조회 |
| POST | Create | ❌ | ❌ | 리소스 생성 |
| PUT | Update | ✅ | ❌ | 리소스 전체 수정 |
| PATCH | Update | ❌ | ❌ | 리소스 부분 수정 |
| DELETE | Delete | ✅ | ❌ | 리소스 삭제 |

**멱등성 (Idempotent):** 동일 요청 반복 시 결과 동일  
**안전성 (Safe):** 서버 상태 변경 없음

---

### 2. HTTP 상태 코드

#### 2xx 성공

| 코드 | 의미 | 사용 예시 |
|------|------|----------|
| 200 OK | 조회 성공 | GET /accounts/1 |
| 201 Created | 생성 성공 | POST /signup + Location 헤더 |
| 204 No Content | 수정/삭제 성공 | PUT /accounts/1, DELETE /accounts/1 |

#### 4xx 클라이언트 오류

| 코드 | 의미 | 사용 예시 |
|------|------|----------|
| 400 Bad Request | 요청 오류 | Bean Validation 실패, 이메일 형식 오류 |
| 401 Unauthorized | 인증 필요 | 로그인 필요 |
| 403 Forbidden | 권한 없음 | ADMIN 전용 API |
| 404 Not Found | 리소스 미존재 | GET /accounts/999 |
| 409 Conflict | 리소스 충돌 | 이메일 중복 |

#### 5xx 서버 오류

| 코드 | 의미 | 사용 예시 |
|------|------|----------|
| 500 Internal Server Error | 서버 오류 | 예상치 못한 예외 |

---

### 3. Location 헤더 (RFC 7231)

**201 Created 응답 시 필수:**

```http
HTTP/1.1 201 Created
Location: /api/v1/accounts/1
Content-Type: application/json

{
  "id": 1,
  "email": "user@example.com"
}
```

**Controller 구현:**

```java
@PostMapping("/signup")
public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request) {
    AccountResponse response = signupService.signup(request);
    
    // Location 헤더 생성
    URI location = URI.create("/api/v1/accounts/" + response.id());
    
    return ResponseEntity
        .created(location)  // 201 + Location 헤더
        .body(response);
}
```

---

### 4. ProblemDetail (RFC 9457)

**표준화된 오류 응답:**

```json
{
  "type": "https://api.softwarecampus.com/problems/duplicate-email",
  "title": "Duplicate Email",
  "status": 409,
  "detail": "이미 사용 중인 이메일입니다."
}
```

**GlobalExceptionHandler 구현:**

```java
@ExceptionHandler(DuplicateEmailException.class)
public ResponseEntity<ProblemDetail> handleDuplicateEmail(DuplicateEmailException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/duplicate-email"));
    problemDetail.setTitle("Duplicate Email");
    
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
}
```

---

## 🧪 Postman 테스트

### 1. 회원가입 성공 (USER)

```http
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "user1@example.com",
  "password": "password123!",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구",
  "accountType": "USER"
}
```

**예상 응답:**
```http
HTTP/1.1 201 Created
Location: /api/v1/accounts/1

{
  "id": 1,
  "accountType": "USER",
  "accountApproved": "APPROVED"
}
```

---

### 2. 회원가입 성공 (ACADEMY)

```http
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "teacher@example.com",
  "password": "password123!",
  "userName": "김선생",
  "phoneNumber": "010-9876-5432",
  "address": "서울시 서초구",
  "affiliation": "ABC학원",
  "position": "수학 강사",
  "accountType": "ACADEMY",
  "academyId": 100
}
```

**예상 응답:**
```http
HTTP/1.1 201 Created
Location: /api/v1/accounts/2

{
  "id": 2,
  "accountType": "ACADEMY",
  "accountApproved": "PENDING",
  "academyId": 100
}
```

---

### 3. 이메일 중복 (409)

```http
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "user1@example.com",
  "password": "password123!",
  "userName": "이순신",
  "phoneNumber": "010-5555-6666",
  "accountType": "USER"
}
```

**예상 응답:**
```http
HTTP/1.1 409 Conflict

{
  "type": "https://api.softwarecampus.com/problems/duplicate-email",
  "title": "Duplicate Email",
  "status": 409,
  "detail": "이미 사용 중인 이메일입니다."
}
```

---

### 4. 이메일 형식 오류 (400)

```http
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "invalid-email",
  "password": "password123!",
  "userName": "세종대왕",
  "phoneNumber": "010-7777-8888",
  "accountType": "USER"
}
```

**예상 응답:**
```http
HTTP/1.1 400 Bad Request

{
  "type": "https://api.softwarecampus.com/problems/invalid-input",
  "title": "Invalid Input",
  "status": 400,
  "detail": "올바른 이메일 형식이 아닙니다."
}
```

---

### 5. ADMIN 계정 차단 (400)

```http
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "password123!",
  "userName": "관리자",
  "phoneNumber": "010-0000-0000",
  "accountType": "ADMIN"
}
```

**예상 응답:**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/invalid-account-type",
  "title": "Invalid Account Type",
  "status": 400,
  "detail": "관리자 계정은 회원가입으로 생성할 수 없습니다."
}
```

---

### 6. ACADEMY academyId 누락 (400)

```http
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "email": "teacher2@example.com",
  "password": "password123!",
  "userName": "박선생",
  "phoneNumber": "010-1111-2222",
  "accountType": "ACADEMY"
}
```

**예상 응답:**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/missing-required-field",
  "title": "Missing Required Field",
  "status": 400,
  "detail": "기관 회원은 기관 ID가 필수입니다."
}
```

---

### 7. 이메일 중복 확인 (사용 가능)

```http
GET http://localhost:8080/api/v1/auth/check-email?email=newuser@example.com
```

**예상 응답:**
```http
HTTP/1.1 200 OK

{
  "message": "사용 가능한 이메일입니다."
}
```

---

### 8. 이메일 중복 확인 (사용 불가)

```http
GET http://localhost:8080/api/v1/auth/check-email?email=user1@example.com
```

**예상 응답:**
```http
HTTP/1.1 200 OK

{
  "message": "이미 사용 중인 이메일입니다."
}
```

---

## 🛠️ 테스트 실행 명령어

### Maven

```powershell
# Controller 테스트만 실행
mvn test -Dtest=AuthControllerTest

# 모든 Controller 테스트
mvn test -Dtest=*ControllerTest

# 전체 테스트
mvn test

# 컴파일 확인
mvn clean compile

# 빌드 검증
mvn clean verify
```

---

## ✅ 보안 체크리스트

- [ ] **PII 로깅 제거**
  - [ ] 이메일 원본 노출 금지
  - [ ] 비밀번호 로깅 절대 금지
  - [ ] 전화번호 로깅 금지
  - [ ] 주소 로깅 금지

- [ ] **로깅 레벨 분리**
  - [ ] INFO: accountId, accountType만
  - [ ] DEBUG: 일반화된 정보 (있음/없음)

- [ ] **이메일 마스킹**
  - [ ] Service Layer에서 마스킹 후 로깅
  - [ ] u***@example.com 형식

- [ ] **비밀번호 보안**
  - [ ] Controller → Service 즉시 암호화
  - [ ] 로그 절대 출력 금지
  - [ ] AccountResponse 제외

- [ ] **CORS 설정**
  - [ ] 프론트엔드 도메인 허용
  - [ ] allowedMethods 제한
  - [ ] allowCredentials 설정

---

## 🔗 관련 문서

- [AuthController 구현](01_auth_controller.md) - Controller 코드
- [API 명세서](02_api_specification.md) - 요청/응답 예시
- [Controller 테스트](03_controller_test.md) - MockMvc 테스트

---

## 📚 참고 자료

### RESTful API
- [RFC 7231 - HTTP/1.1 Semantics](https://www.rfc-editor.org/rfc/rfc7231)
- [RFC 9457 - Problem Details](https://www.rfc-editor.org/rfc/rfc9457.html)
- [REST API Best Practices](https://restfulapi.net/)

### 보안
- [OWASP API Security](https://owasp.org/www-project-api-security/)
- [PII 데이터 보호](https://www.privacy.go.kr/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
