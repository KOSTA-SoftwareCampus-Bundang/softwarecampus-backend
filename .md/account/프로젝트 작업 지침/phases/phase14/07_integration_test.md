# 7. 통합 테스트 (로그인 E2E)

**목표:** 로그인 전체 플로우 통합 테스트 (회원가입 → 로그인 → JWT 인증 API 호출)

---

## 📂 생성 파일

```
src/test/java/com/softwarecampus/backend/
└─ integration/
   └─ LoginIntegrationTest.java
```

---

## 7.1 LoginIntegrationTest.java

**경로:** `test/java/com/softwarecampus/backend/integration/LoginIntegrationTest.java`

**설명:** 로그인 통합 테스트 (5-8개 테스트)

```java
package com.softwarecampus.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 로그인 통합 테스트
 * 
 * 테스트 시나리오:
 * 1. 회원가입 → 로그인 성공
 * 2. 로그인 후 JWT 토큰으로 보호된 API 호출
 * 3. 잘못된 비밀번호로 로그인 실패
 * 4. 존재하지 않는 이메일로 로그인 실패
 * 5. ACADEMY 계정 로그인 (승인 대기)
 * 6. Access Token으로 마이페이지 조회
 * 7. Refresh Token으로 Access Token 갱신 후 API 호출
 * 
 * @author 태윤
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("로그인 통합 테스트")
class LoginIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private SignupRequest userSignupRequest;
    private LoginRequest userLoginRequest;
    
    @BeforeEach
    void setUp() {
        userSignupRequest = new SignupRequest(
            "integrationuser@example.com",
            "Password123!",
            "통합테스트",
            "010-9999-8888",
            "서울시 종로구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        userLoginRequest = new LoginRequest(
            "integrationuser@example.com",
            "Password123!"
        );
    }
    
    @Test
    @DisplayName("시나리오 1: 회원가입 → 로그인 성공")
    void scenario_SignupAndLogin_Success() throws Exception {
        // 1. 회원가입
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("integrationuser@example.com"));
        
        // 2. 로그인
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.account.email").value("integrationuser@example.com"))
            .andExpect(jsonPath("$.account.userName").value("통합테스트"))
            .andExpect(jsonPath("$.account.accountType").value("USER"))
            .andExpect(jsonPath("$.account.accountApproved").value("APPROVED"));
    }
    
    @Test
    @DisplayName("시나리오 2: 로그인 후 JWT 토큰으로 보호된 API 호출")
    void scenario_LoginAndAccessProtectedEndpoint() throws Exception {
        // 1. 회원가입
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        // 2. 로그인 후 Access Token 추출
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        
        // 3. JWT 토큰으로 보호된 엔드포인트 호출 (마이페이지)
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("integrationuser@example.com"))
            .andExpect(jsonPath("$.userName").value("통합테스트"));
    }
    
    @Test
    @DisplayName("시나리오 3: 잘못된 비밀번호로 로그인 실패")
    void scenario_Login_WrongPassword() throws Exception {
        // 1. 회원가입
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        // 2. 잘못된 비밀번호로 로그인
        LoginRequest wrongPasswordRequest = new LoginRequest(
            "integrationuser@example.com",
            "WrongPassword123!"
        );
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value("이메일 또는 비밀번호가 올바르지 않습니다"));
    }
    
    @Test
    @DisplayName("시나리오 4: 존재하지 않는 이메일로 로그인 실패")
    void scenario_Login_EmailNotFound() throws Exception {
        // 회원가입 없이 바로 로그인 시도
        LoginRequest nonExistentRequest = new LoginRequest(
            "nonexistent@example.com",
            "Password123!"
        );
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("이메일 또는 비밀번호가 올바르지 않습니다"));
    }
    
    @Test
    @DisplayName("시나리오 5: ACADEMY 계정 로그인 실패 (승인 대기)")
    void scenario_Login_PendingAcademy() throws Exception {
        // 1. ACADEMY 계정 회원가입
        SignupRequest academySignup = new SignupRequest(
            "academy@example.com",
            "Password123!",
            "김선생",
            "010-7777-6666",
            "서울시 서초구",
            "소프트웨어 캠퍼스",
            "강사",
            AccountType.ACADEMY,
            100L
        );
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(academySignup)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountApproved").value("PENDING"));
        
        // 2. 로그인 시도 (승인 대기 상태)
        LoginRequest academyLoginRequest = new LoginRequest(
            "academy@example.com",
            "Password123!"
        );
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(academyLoginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("승인 대기 중인 계정입니다"));
    }
    
    @Test
    @DisplayName("시나리오 6: Access Token 없이 보호된 엔드포인트 접근 실패")
    void scenario_AccessProtectedEndpoint_NoToken() throws Exception {
        // JWT 토큰 없이 보호된 엔드포인트 호출
        mockMvc.perform(get("/api/mypage/profile"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("시나리오 7: 잘못된 Access Token으로 접근 실패")
    void scenario_AccessProtectedEndpoint_InvalidToken() throws Exception {
        // 잘못된 JWT 토큰으로 호출
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer invalid-token-123"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("시나리오 8: Refresh Token으로 Access Token 갱신 후 API 호출")
    void scenario_RefreshTokenAndAccessProtectedEndpoint() throws Exception {
        // 1. 회원가입
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        // 2. 로그인 후 Access Token, Refresh Token 추출
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String loginResponse = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();
        
        // 3. Refresh Token으로 새로운 Access Token 발급
        String refreshRequestBody = String.format(
            "{\"refreshToken\":\"%s\",\"email\":\"%s\"}",
            refreshToken,
            "integrationuser@example.com"
        );
        
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshRequestBody))
            .andExpect(status().isOk())
            .andReturn();
        
        String refreshResponse = refreshResult.getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(refreshResponse).get("accessToken").asText();
        
        // 4. 새로운 Access Token으로 보호된 API 호출
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + newAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("integrationuser@example.com"));
    }
}
```

---

## 📊 테스트 커버리지

| 테스트 케이스 | 검증 내용 |
|------------|---------|
| scenario_SignupAndLogin_Success | 회원가입 → 로그인 성공 플로우 |
| scenario_LoginAndAccessProtectedEndpoint | 로그인 후 JWT로 보호된 API 호출 |
| scenario_Login_WrongPassword | 잘못된 비밀번호 로그인 실패 |
| scenario_Login_EmailNotFound | 존재하지 않는 이메일 로그인 실패 |
| scenario_Login_PendingAcademy | ACADEMY 승인 대기 로그인 실패 |
| scenario_AccessProtectedEndpoint_NoToken | 토큰 없이 보호된 API 접근 실패 |
| scenario_AccessProtectedEndpoint_InvalidToken | 잘못된 토큰으로 접근 실패 |
| scenario_RefreshTokenAndAccessProtectedEndpoint | Refresh Token 갱신 후 API 호출 |

**총 8개 테스트**

---

## 🔄 E2E 플로우

### 시나리오 1: 회원가입 → 로그인
```text
1. POST /api/auth/signup
   → 201 Created + AccountResponse
   
2. POST /api/auth/login
   → 200 OK + LoginResponse {
        accessToken,
        refreshToken,
        tokenType: "Bearer",
        expiresIn: 900,
        account
     }
```

### 시나리오 2: 로그인 → JWT 인증 API 호출
```text
1. POST /api/auth/signup
   → 201 Created
   
2. POST /api/auth/login
   → 200 OK + accessToken
   
3. GET /api/mypage/profile
   Authorization: Bearer {accessToken}
   → 200 OK + AccountResponse
```

### 시나리오 8: Refresh Token 갱신
```text
1. POST /api/auth/login
   → accessToken, refreshToken
   
2. POST /api/auth/refresh
   Authorization: Bearer {oldAccessToken}
   Body: { refreshToken, email }
   → 200 OK + newAccessToken
   
3. GET /api/mypage/profile
   Authorization: Bearer {newAccessToken}
   → 200 OK
```

---

## 🔐 JWT 인증 검증

### Authorization 헤더 형식
```java
.header("Authorization", "Bearer " + accessToken)
```

### 토큰 추출
```java
MvcResult result = mockMvc.perform(...).andReturn();
String responseBody = result.getResponse().getContentAsString();
String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
```

### 보호된 엔드포인트 호출
```java
mockMvc.perform(get("/api/mypage/profile")
        .header("Authorization", "Bearer " + accessToken))
    .andExpect(status().isOk());
```

---

## 🧪 통합 테스트 특징

### 1. @SpringBootTest
- **전체 ApplicationContext 로드**: 실제 Bean 사용 (Mock 없음)
- **실제 DB 연동**: H2 또는 TestContainers
- **실제 JWT 생성/검증**: JwtTokenProvider 실제 동작

### 2. @Transactional
- **테스트 격리**: 각 테스트 후 자동 롤백
- **데이터 독립성**: 테스트 간 영향 없음

### 3. @ActiveProfiles("test")
- **테스트 전용 설정**: application-test.yml 사용
- **H2 인메모리 DB**: 테스트 속도 향상

---

## 🔗 다음 단계

통합 테스트 완료 후:
1. **mvn test** 실행하여 전체 테스트 통과 확인
2. **Phase 15** 마이페이지 API 구현 준비
