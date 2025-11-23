# 5. 전체 E2E 통합 테스트

**목표:** 회원가입부터 로그인, 프로필 조회/수정까지 전체 플로우 통합 테스트

---

## 📂 생성 파일

```
src/test/java/com/softwarecampus/backend/
└─ integration/
   └─ FullE2ETest.java
```

---

## 5.1 FullE2ETest.java

**경로:** `test/java/com/softwarecampus/backend/integration/FullE2ETest.java`

**설명:** 전체 E2E 통합 테스트 (15-20개 테스트)

```java
package com.softwarecampus.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.repository.user.AccountRepository;
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
 * 전체 E2E 통합 테스트
 * 
 * 테스트 시나리오:
 * 1. 회원가입 → 로그인 → 프로필 조회
 * 2. 회원가입 → 로그인 → 프로필 수정 → 재조회
 * 3. 프로필 수정 (부분 업데이트)
 * 4. 전화번호 수정 (중복 검증)
 * 5. 빈 요청 (모든 필드 null)
 * 6. JWT 토큰 없이 마이페이지 접근
 * 7. 잘못된 JWT 토큰으로 접근
 * 8. Refresh Token 갱신 후 프로필 수정
 * 9. ACADEMY 계정 전체 플로우 (회원가입 → 로그인 실패 → 승인 → 재로그인 → 프로필 수정)
 * 10. 여러 사용자 동시 프로필 수정
 * 
 * @author 태윤
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("전체 E2E 통합 테스트")
class FullE2ETest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AccountRepository accountRepository;
    
    private SignupRequest userSignupRequest;
    private LoginRequest userLoginRequest;
    
    @BeforeEach
    void setUp() {
        userSignupRequest = new SignupRequest(
            "e2euser@example.com",
            "Password123!",
            "E2E테스트",
            "010-1111-2222",
            "서울시 강남구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        userLoginRequest = new LoginRequest(
            "e2euser@example.com",
            "Password123!"
        );
    }
    
    @Test
    @DisplayName("시나리오 1: 회원가입 → 로그인 → 프로필 조회")
    void scenario1_SignupLoginGetProfile() throws Exception {
        // 1. 회원가입
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("e2euser@example.com"))
            .andExpect(jsonPath("$.userName").value("E2E테스트"))
            .andExpect(jsonPath("$.accountType").value("USER"))
            .andExpect(jsonPath("$.accountApproved").value("APPROVED"));
        
        // 2. 로그인
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andReturn();
        
        String accessToken = objectMapper.readTree(
            loginResult.getResponse().getContentAsString()
        ).get("accessToken").asText();
        
        // 3. 프로필 조회
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("e2euser@example.com"))
            .andExpect(jsonPath("$.userName").value("E2E테스트"))
            .andExpect(jsonPath("$.phoneNumber").value("010-1111-2222"))
            .andExpect(jsonPath("$.address").value("서울시 강남구"));
    }
    
    @Test
    @DisplayName("시나리오 2: 회원가입 → 로그인 → 프로필 수정 → 재조회")
    void scenario2_SignupLoginUpdateProfile() throws Exception {
        // 1. 회원가입
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        // 2. 로그인
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String accessToken = objectMapper.readTree(
            loginResult.getResponse().getContentAsString()
        ).get("accessToken").asText();
        
        // 3. 프로필 수정
        UpdateProfileRequest updateRequest = new UpdateProfileRequest(
            "E2E테스트 (수정)",
            "010-9999-8888",
            "서울시 종로구",
            "소프트웨어 캠퍼스",
            "수강생"
        );
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("E2E테스트 (수정)"))
            .andExpect(jsonPath("$.phoneNumber").value("010-9999-8888"))
            .andExpect(jsonPath("$.address").value("서울시 종로구"))
            .andExpect(jsonPath("$.affiliation").value("소프트웨어 캠퍼스"))
            .andExpect(jsonPath("$.position").value("수강생"));
        
        // 4. 프로필 재조회 (변경 확인)
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("E2E테스트 (수정)"))
            .andExpect(jsonPath("$.phoneNumber").value("010-9999-8888"))
            .andExpect(jsonPath("$.address").value("서울시 종로구"));
    }
    
    @Test
    @DisplayName("시나리오 3: 프로필 부분 업데이트 (userName만 변경)")
    void scenario3_PartialUpdate() throws Exception {
        // 1. 회원가입 + 로그인
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String accessToken = objectMapper.readTree(
            loginResult.getResponse().getContentAsString()
        ).get("accessToken").asText();
        
        // 2. userName만 변경
        UpdateProfileRequest partialUpdate = new UpdateProfileRequest(
            "새이름",
            null,
            null,
            null,
            null
        );
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("새이름"))
            .andExpect(jsonPath("$.phoneNumber").value("010-1111-2222"))  // 변경 안 됨
            .andExpect(jsonPath("$.address").value("서울시 강남구"));      // 변경 안 됨
    }
    
    @Test
    @DisplayName("시나리오 4: 전화번호 중복 검증")
    void scenario4_PhoneNumberDuplicate() throws Exception {
        // 1. 첫 번째 사용자 회원가입
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        // 2. 두 번째 사용자 회원가입 (다른 전화번호)
        SignupRequest user2Signup = new SignupRequest(
            "user2@example.com",
            "Password123!",
            "사용자2",
            "010-3333-4444",
            "부산시 해운대구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Signup)))
            .andExpect(status().isCreated());
        
        // 3. 두 번째 사용자 로그인
        LoginRequest user2Login = new LoginRequest("user2@example.com", "Password123!");
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Login)))
            .andExpect(status().isOk())
            .andReturn();
        
        String accessToken = objectMapper.readTree(
            loginResult.getResponse().getContentAsString()
        ).get("accessToken").asText();
        
        // 4. 첫 번째 사용자의 전화번호로 변경 시도 (중복)
        UpdateProfileRequest duplicatePhoneUpdate = new UpdateProfileRequest(
            null,
            "010-1111-2222",  // 첫 번째 사용자 전화번호
            null,
            null,
            null
        );
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicatePhoneUpdate)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("이미 사용 중인 전화번호입니다"));
    }
    
    @Test
    @DisplayName("시나리오 5: 빈 요청 (모든 필드 null)")
    void scenario5_EmptyUpdateRequest() throws Exception {
        // 1. 회원가입 + 로그인
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String accessToken = objectMapper.readTree(
            loginResult.getResponse().getContentAsString()
        ).get("accessToken").asText();
        
        // 2. 빈 요청 (모든 필드 null)
        UpdateProfileRequest emptyRequest = new UpdateProfileRequest(
            null,
            null,
            null,
            null,
            null
        );
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("변경할 항목이 없습니다"));
    }
    
    @Test
    @DisplayName("시나리오 6: JWT 토큰 없이 마이페이지 접근")
    void scenario6_AccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/mypage/profile"))
            .andExpect(status().isUnauthorized());
        
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("시나리오 7: 잘못된 JWT 토큰으로 접근")
    void scenario7_AccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer invalid-token-123"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("시나리오 8: Refresh Token 갱신 후 프로필 수정")
    void scenario8_RefreshTokenAndUpdateProfile() throws Exception {
        // 1. 회원가입 + 로그인
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String loginResponse = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();
        
        // 2. Refresh Token으로 새로운 Access Token 발급
        String refreshRequestBody = String.format(
            "{\"refreshToken\":\"%s\",\"email\":\"%s\"}",
            refreshToken,
            "e2euser@example.com"
        );
        
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshRequestBody))
            .andExpect(status().isOk())
            .andReturn();
        
        String newAccessToken = objectMapper.readTree(
            refreshResult.getResponse().getContentAsString()
        ).get("accessToken").asText();
        
        // 3. 새로운 Access Token으로 프로필 수정
        UpdateProfileRequest updateRequest = new UpdateProfileRequest(
            "갱신 후 수정",
            null,
            null,
            null,
            null
        );
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + newAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("갱신 후 수정"));
    }
    
    @Test
    @DisplayName("시나리오 9: ACADEMY 계정 전체 플로우")
    void scenario9_AcademyAccountFullFlow() throws Exception {
        // 1. ACADEMY 계정 회원가입
        SignupRequest academySignup = new SignupRequest(
            "academy@example.com",
            "Password123!",
            "김선생",
            "010-5555-6666",
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
            .andExpect(jsonPath("$.accountType").value("ACADEMY"))
            .andExpect(jsonPath("$.accountApproved").value("PENDING"));
        
        // 2. 로그인 시도 (승인 대기 상태)
        LoginRequest academyLogin = new LoginRequest("academy@example.com", "Password123!");
        
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(academyLogin)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("승인 대기 중인 계정입니다"));
        
        // 3. 관리자가 승인 (DB 직접 업데이트)
        Account academyAccount = accountRepository.findByEmail("academy@example.com")
            .orElseThrow();
        academyAccount.approve();  // APPROVED로 변경
        accountRepository.save(academyAccount);
        
        // 4. 재로그인 성공
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(academyLogin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account.accountApproved").value("APPROVED"))
            .andReturn();
        
        String accessToken = objectMapper.readTree(
            loginResult.getResponse().getContentAsString()
        ).get("accessToken").asText();
        
        // 5. 프로필 수정
        UpdateProfileRequest academyUpdate = new UpdateProfileRequest(
            "김선생 (승인 후)",
            null,
            null,
            "소프트웨어 캠퍼스 (수정)",
            "수석강사"
        );
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(academyUpdate)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("김선생 (승인 후)"))
            .andExpect(jsonPath("$.affiliation").value("소프트웨어 캠퍼스 (수정)"))
            .andExpect(jsonPath("$.position").value("수석강사"));
    }
    
    @Test
    @DisplayName("시나리오 10: 여러 사용자 동시 프로필 수정")
    void scenario10_MultipleUsersUpdateProfile() throws Exception {
        // 1. 사용자 1 회원가입 + 로그인
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        MvcResult user1Login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String user1Token = objectMapper.readTree(
            user1Login.getResponse().getContentAsString()
        ).get("accessToken").asText();
        
        // 2. 사용자 2 회원가입 + 로그인
        SignupRequest user2Signup = new SignupRequest(
            "user2@example.com",
            "Password123!",
            "사용자2",
            "010-7777-8888",
            "부산시 해운대구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Signup)))
            .andExpect(status().isCreated());
        
        LoginRequest user2Login = new LoginRequest("user2@example.com", "Password123!");
        
        MvcResult user2LoginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Login)))
            .andExpect(status().isOk())
            .andReturn();
        
        String user2Token = objectMapper.readTree(
            user2LoginResult.getResponse().getContentAsString()
        ).get("accessToken").asText();
        
        // 3. 사용자 1 프로필 수정
        UpdateProfileRequest user1Update = new UpdateProfileRequest(
            "사용자1 (수정)",
            null,
            null,
            null,
            null
        );
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1Update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("사용자1 (수정)"))
            .andExpect(jsonPath("$.email").value("e2euser@example.com"));
        
        // 4. 사용자 2 프로필 수정
        UpdateProfileRequest user2Update = new UpdateProfileRequest(
            "사용자2 (수정)",
            null,
            null,
            null,
            null
        );
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("사용자2 (수정)"))
            .andExpect(jsonPath("$.email").value("user2@example.com"));
        
        // 5. 각자 프로필 재조회 (독립성 검증)
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + user1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("사용자1 (수정)"))
            .andExpect(jsonPath("$.email").value("e2euser@example.com"));
        
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + user2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("사용자2 (수정)"))
            .andExpect(jsonPath("$.email").value("user2@example.com"));
    }
}
```

---

## 📊 테스트 커버리지

| 시나리오 | 테스트 내용 |
|---------|-----------|
| 1 | 회원가입 → 로그인 → 프로필 조회 |
| 2 | 회원가입 → 로그인 → 프로필 수정 → 재조회 |
| 3 | 프로필 부분 업데이트 (userName만) |
| 4 | 전화번호 중복 검증 |
| 5 | 빈 요청 (모든 필드 null) |
| 6 | JWT 토큰 없이 마이페이지 접근 |
| 7 | 잘못된 JWT 토큰으로 접근 |
| 8 | Refresh Token 갱신 후 프로필 수정 |
| 9 | ACADEMY 계정 전체 플로우 (회원가입 → 승인 대기 → 승인 → 로그인 → 프로필 수정) |
| 10 | 여러 사용자 동시 프로필 수정 (독립성 검증) |

**총 10개 시나리오 (15-20개 검증 포인트)**

---

## 🔄 주요 E2E 플로우

### 시나리오 1: 기본 플로우
```text
POST /api/auth/signup
→ 201 Created + AccountResponse

POST /api/auth/login
→ 200 OK + accessToken

GET /api/mypage/profile
Authorization: Bearer {accessToken}
→ 200 OK + AccountResponse
```

### 시나리오 9: ACADEMY 승인 플로우
```text
POST /api/auth/signup (ACADEMY)
→ 201 Created + accountApproved: PENDING

POST /api/auth/login
→ 401 Unauthorized (승인 대기)

[관리자 승인 - DB 업데이트]
Account.approve() → APPROVED

POST /api/auth/login
→ 200 OK + accessToken

PATCH /api/mypage/profile
→ 200 OK + 수정된 프로필
```

---

## 🔗 완료!

Phase 15 설계 문서 5개 모두 완성:
1. ✅ **01_dto_layer.md** - UpdateProfileRequest
2. ✅ **02_service_layer.md** - ProfileService.updateProfile()
3. ✅ **03_controller_layer.md** - MyPageController
4. ✅ **04_controller_test.md** - MyPageControllerTest (11개)
5. ✅ **05_full_e2e_test.md** - FullE2ETest (10 시나리오)

**Phase 11-15 전체 설계 문서 완성! 🎉**
