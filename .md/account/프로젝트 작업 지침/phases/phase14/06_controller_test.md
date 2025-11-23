# 6. Controller 슬라이스 테스트 (로그인)

**목표:** AuthController 로그인 엔드포인트 테스트

---

## 📂 수정 파일

```
src/test/java/com/softwarecampus/backend/
└─ controller/user/
   └─ AuthControllerTest.java   (login 테스트 추가)
```

---

## 6.1 AuthControllerTest 확장

**경로:** `test/java/com/softwarecampus/backend/controller/user/AuthControllerTest.java`

**설명:** 기존 AuthControllerTest에 로그인 테스트 메서드 추가 (5-7개)

### 추가 Mock 및 import

```java
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.LoginResponse;
import com.softwarecampus.backend.exception.user.InvalidCredentialsException;
import com.softwarecampus.backend.service.user.login.LoginService;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController 슬라이스 테스트")
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private SignupService signupService;
    
    @MockBean
    private LoginService loginService;  // ← 추가
    
    @MockBean
    private TokenService tokenService;
    
    // 기존 signup(), checkEmail(), refresh() 테스트 유지
    
    // ===== 로그인 테스트 추가 =====
    
    @Nested
    @DisplayName("POST /api/auth/login - 로그인")
    class Login {
        
        private LoginRequest validLoginRequest;
        private LoginResponse successLoginResponse;
        private AccountResponse userAccountResponse;
        
        @BeforeEach
        void setUp() {
            validLoginRequest = new LoginRequest(
                "user@example.com",
                "Password123!"
            );
            
            userAccountResponse = new AccountResponse(
                1L,
                "user@example.com",
                "홍길동",
                "010-1234-5678",
                "서울시 강남구",
                null,
                null,
                "USER",
                "APPROVED",
                LocalDateTime.now()
            );
            
            successLoginResponse = LoginResponse.of(
                "access-token-123",
                "refresh-token-456",
                900L,
                userAccountResponse
            );
        }
        
        @Test
        @DisplayName("로그인 성공")
        void login_Success() throws Exception {
            // given
            when(loginService.login(any(LoginRequest.class)))
                .thenReturn(successLoginResponse);
            
            // when & then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.account.email").value("user@example.com"))
                .andExpect(jsonPath("$.account.userName").value("홍길동"))
                .andExpect(jsonPath("$.account.accountType").value("USER"));
            
            verify(loginService).login(any(LoginRequest.class));
        }
        
        @Test
        @DisplayName("Bean Validation 실패 - 이메일 누락")
        void login_Fail_EmailBlank() throws Exception {
            // given
            LoginRequest invalidRequest = new LoginRequest(
                "",  // 빈 이메일
                "Password123!"
            );
            
            // when & then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
            
            verify(loginService, never()).login(any(LoginRequest.class));
        }
        
        @Test
        @DisplayName("Bean Validation 실패 - 이메일 형식 오류")
        void login_Fail_EmailInvalid() throws Exception {
            // given
            LoginRequest invalidRequest = new LoginRequest(
                "invalid-email",  // 잘못된 이메일 형식
                "Password123!"
            );
            
            // when & then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.email").value("유효한 이메일 형식이 아닙니다"));
            
            verify(loginService, never()).login(any(LoginRequest.class));
        }
        
        @Test
        @DisplayName("Bean Validation 실패 - 비밀번호 누락")
        void login_Fail_PasswordBlank() throws Exception {
            // given
            LoginRequest invalidRequest = new LoginRequest(
                "user@example.com",
                ""  // 빈 비밀번호
            );
            
            // when & then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.password").value("비밀번호는 필수입니다"));
            
            verify(loginService, never()).login(any(LoginRequest.class));
        }
        
        @Test
        @DisplayName("인증 실패 - 잘못된 자격증명")
        void login_Fail_InvalidCredentials() throws Exception {
            // given
            when(loginService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다"));
            
            // when & then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("이메일 또는 비밀번호가 올바르지 않습니다"));
            
            verify(loginService).login(any(LoginRequest.class));
        }
        
        @Test
        @DisplayName("인증 실패 - 비활성화된 계정")
        void login_Fail_InactiveAccount() throws Exception {
            // given
            when(loginService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("비활성화된 계정입니다"));
            
            // when & then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("비활성화된 계정입니다"));
        }
        
        @Test
        @DisplayName("인증 실패 - 미승인 ACADEMY 계정")
        void login_Fail_PendingAcademy() throws Exception {
            // given
            when(loginService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("승인 대기 중인 계정입니다"));
            
            // when & then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("승인 대기 중인 계정입니다"));
        }
    }
}
```

---

## 📊 테스트 커버리지

| 테스트 케이스 | HTTP 메서드 | 상태 코드 | 검증 내용 |
|------------|-----------|---------|---------|
| login_Success | POST | 200 | 로그인 성공 + JWT 토큰 발급 |
| login_Fail_EmailBlank | POST | 400 | 이메일 누락 |
| login_Fail_EmailInvalid | POST | 400 | 이메일 형식 오류 |
| login_Fail_PasswordBlank | POST | 400 | 비밀번호 누락 |
| login_Fail_InvalidCredentials | POST | 401 | 잘못된 자격증명 |
| login_Fail_InactiveAccount | POST | 401 | 비활성화된 계정 |
| login_Fail_PendingAcademy | POST | 401 | 미승인 ACADEMY 계정 |

**총 7개 테스트**

---

## 🔍 핵심 검증 포인트

### 1. 성공 응답 검증
```java
.andExpect(status().isOk())
.andExpect(jsonPath("$.accessToken").value("access-token-123"))
.andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
.andExpect(jsonPath("$.tokenType").value("Bearer"))
.andExpect(jsonPath("$.expiresIn").value(900))
.andExpect(jsonPath("$.account.email").value("user@example.com"))
```
- **200 OK**: 로그인 성공
- **accessToken, refreshToken**: JWT 토큰 발급 확인
- **tokenType**: "Bearer" 고정값
- **expiresIn**: 900초 (15분)
- **account**: 사용자 정보 포함

### 2. Bean Validation 검증
```java
.andExpect(status().isBadRequest())
.andExpect(jsonPath("$.status").value(400))
.andExpect(jsonPath("$.errors.email").value("유효한 이메일 형식이 아닙니다"))
```
- **400 Bad Request**: Bean Validation 실패
- **RFC 9457 ProblemDetail**: errors 필드에 검증 오류 상세

### 3. 인증 실패 검증
```java
.andExpect(status().isUnauthorized())
.andExpect(jsonPath("$.status").value(401))
.andExpect(jsonPath("$.detail").value("이메일 또는 비밀번호가 올바르지 않습니다"))
```
- **401 Unauthorized**: 인증 실패
- **동일한 메시지**: 이메일 오류와 비밀번호 오류 구분 없음

### 4. Service 호출 검증
```java
verify(loginService).login(any(LoginRequest.class));
verify(loginService, never()).login(any(LoginRequest.class));  // Bean Validation 실패 시
```
- **성공 시**: LoginService.login() 호출됨
- **Bean Validation 실패 시**: Service 호출 안 됨 (Controller 레벨에서 차단)

---

## 🧪 MockMvc 요청 예시

### 로그인 성공
```java
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123!"
}

Response: 200 OK
{
  "accessToken": "access-token-123",
  "refreshToken": "refresh-token-456",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "account": {
    "id": 1,
    "email": "user@example.com",
    "userName": "홍길동",
    "accountType": "USER",
    "accountApproved": "APPROVED"
  }
}
```

### Bean Validation 실패
```java
POST /api/auth/login
Content-Type: application/json

{
  "email": "invalid-email",
  "password": ""
}

Response: 400 Bad Request
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값 검증 실패",
  "errors": {
    "email": "유효한 이메일 형식이 아닙니다",
    "password": "비밀번호는 필수입니다"
  }
}
```

### 인증 실패
```java
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "WrongPassword"
}

Response: 401 Unauthorized
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "이메일 또는 비밀번호가 올바르지 않습니다"
}
```

---

## 🔗 다음 단계

Controller 슬라이스 테스트 완료 후:
1. **LoginIntegrationTest** 통합 테스트 작성 ([07_integration_test.md](07_integration_test.md))
