# 3. Controller Layer (로그인 API)

**목표:** 로그인 API 엔드포인트 구현

---

## 📂 수정 파일

```
src/main/java/com/softwarecampus/backend/
└─ controller/user/
   └─ AuthController.java   (login() 메서드 추가)
```

---

## 3.1 AuthController 확장

**경로:** `controller/user/AuthController.java`

**설명:** 기존 AuthController에 로그인 메서드 추가

### 추가 의존성

```java
// 기존 import 유지
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.LoginResponse;
import com.softwarecampus.backend.service.user.login.LoginService;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final SignupService signupService;
    private final LoginService loginService;  // ← 추가
    private final TokenService tokenService;
    
    // 기존 signup(), checkEmail(), refresh() 메서드 유지
    
    /**
     * 로그인 API
     * 
     * @param request 로그인 요청 (email, password)
     * @return 200 OK + LoginResponse (accessToken, refreshToken, account)
     * 
     * @throws InvalidCredentialsException 401 - 이메일 없음 또는 비밀번호 불일치
     * @throws InvalidCredentialsException 401 - 비활성화된 계정
     * @throws InvalidCredentialsException 401 - 미승인 ACADEMY 계정
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 API 호출");
        
        LoginResponse response = loginService.login(request);
        
        log.info("로그인 성공 - accountType: {}", response.account().accountType());
        
        return ResponseEntity.ok(response);
    }
}
```

**핵심 포인트:**

### 1. HTTP 메서드: POST
```java
@PostMapping("/login")
```
- **POST**: 로그인은 리소스 생성이 아니지만 보안상 POST 사용
  - GET: URL에 비밀번호 노출 위험 (브라우저 히스토리, 서버 로그)
  - POST: Body에 포함되어 안전

### 2. Bean Validation
```java
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request)
```
- `@Valid`: LoginRequest의 `@NotBlank`, `@Email` 검증
- 검증 실패 시 자동으로 400 Bad Request

### 3. 상태 코드: 200 OK
```java
return ResponseEntity.ok(response);
```
- **200 OK**: 로그인 성공 (세션/토큰 발급)
- **401 Unauthorized**: 인증 실패 (GlobalExceptionHandler 처리)

### 4. 로깅
```java
log.info("로그인 API 호출");
log.info("로그인 성공 - accountType: {}", response.account().accountType());
```
- **주의**: 이메일은 Service Layer에서 마스킹하여 로깅
- Controller에서는 accountType만 로깅 (민감 정보 없음)

---

## 📋 API 명세

### POST /api/auth/login

**요청 (Request)**

```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**성공 응답 (200 OK)**

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "account": {
    "id": 1,
    "email": "user@example.com",
    "userName": "홍길동",
    "phoneNumber": "010-1234-5678",
    "address": "서울시 강남구",
    "affiliation": null,
    "position": null,
    "accountType": "USER",
    "accountApproved": "APPROVED",
    "createdDate": "2024-11-23T10:30:00"
  }
}
```

**실패 응답 (401 Unauthorized) - 잘못된 자격증명**

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "이메일 또는 비밀번호가 올바르지 않습니다",
  "instance": "/api/auth/login"
}
```

**실패 응답 (401 Unauthorized) - 미승인 ACADEMY 계정**

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "승인 대기 중인 계정입니다",
  "instance": "/api/auth/login"
}
```

**실패 응답 (400 Bad Request) - Bean Validation 실패**

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값 검증 실패",
  "instance": "/api/auth/login",
  "errors": {
    "email": "유효한 이메일 형식이 아닙니다",
    "password": "비밀번호는 필수입니다"
  }
}
```

---

## 🔄 로그인 플로우

```text
1. 클라이언트 요청
   POST /api/auth/login
   Body: { email, password }
   
2. AuthController
   @Valid → Bean Validation 검증
   
3. LoginService
   ├─ AccountRepository.findByEmail()
   ├─ PasswordEncoder.matches()
   ├─ 계정 상태 검증
   ├─ JwtTokenProvider.generateToken()
   ├─ JwtTokenProvider.generateRefreshToken()
   └─ TokenService.saveRefreshToken()
   
4. 응답 생성
   LoginResponse {
     accessToken,
     refreshToken,
     tokenType: "Bearer",
     expiresIn: 900,
     account
   }
   
5. 클라이언트 저장
   localStorage.setItem('accessToken', ...)
   localStorage.setItem('refreshToken', ...)
```

---

## 🔐 JWT 사용 예시

### 프론트엔드: Access Token 저장

```javascript
// 로그인 성공 후
const response = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password })
});

const { accessToken, refreshToken, expiresIn, account } = await response.json();

// 저장
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// 자동 갱신 타이머 (만료 2분 전)
setTimeout(() => refreshAccessToken(), (expiresIn - 120) * 1000);
```

### 프론트엔드: 인증된 API 호출

```javascript
// 보호된 엔드포인트 호출
const response = await fetch('/api/mypage/profile', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
});
```

---

## 🧪 Postman 테스트

### 1. 로그인 성공

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123!"
}

Expected: 200 OK + accessToken + refreshToken
```

### 2. 잘못된 비밀번호

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "WrongPassword"
}

Expected: 401 Unauthorized
```

### 3. Bean Validation 실패

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "invalid-email",
  "password": ""
}

Expected: 400 Bad Request
```

---

## 🔗 다음 단계

Controller 구현 후:
1. **InvalidCredentialsException** 예외 클래스 생성 ([04_exception_handling.md](04_exception_handling.md))
2. **LoginServiceImplTest** 단위 테스트 작성 ([05_service_test.md](05_service_test.md))
