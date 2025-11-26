# Phase 15-1: 테스트

**파일:**
- `MyPageControllerTest.java` (Controller 슬라이스)
- `MyPageIntegrationTest.java` (통합 테스트)

**목표:** 총 18개 테스트 작성

---

## 📋 테스트 구성

### 1. Controller 슬라이스 테스트 (10개)
- @WebMvcTest(MyPageController.class)
- ProfileService 모킹
- @WithMockUser로 인증 처리

### 2. Integration 테스트 (8개)
- @SpringBootTest + @AutoConfigureMockMvc
- 실제 DB, Redis 사용
- JWT 토큰 발급부터 전체 플로우

---

## 📄 MyPageControllerTest (Controller 슬라이스)

```java
package com.softwarecampus.backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.PhoneNumberAlreadyExistsException;
import com.softwarecampus.backend.service.user.profile.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MyPageController.class)
@Import(SecurityConfig.class)  // Security 설정 import
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    // 1. 프로필 조회 성공
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("프로필 조회 성공 - 200 OK")
    void getProfile_Success() throws Exception {
        // given
        AccountResponse response = AccountResponse.builder()
            .email("user@test.com")
            .userName("홍길동")
            .phoneNumber("010-1234-5678")
            .build();
        
        when(profileService.getProfile("user@test.com")).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/mypage/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@test.com"))
            .andExpect(jsonPath("$.userName").value("홍길동"));
    }

    // 2. 인증 없이 프로필 조회 시도
    @Test
    @DisplayName("인증 없이 프로필 조회 - 401 Unauthorized")
    void getProfile_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/mypage/profile"))
            .andExpect(status().isUnauthorized());
    }

    // 3. 프로필 수정 성공
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("프로필 수정 성공 - 200 OK")
    void updateProfile_Success() throws Exception {
        // given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .userName("김철수")
            .phoneNumber("010-9999-8888")
            .build();
        
        AccountResponse response = AccountResponse.builder()
            .email("user@test.com")
            .userName("김철수")
            .phoneNumber("010-9999-8888")
            .build();
        
        when(profileService.updateProfile(eq("user@test.com"), any()))
            .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("김철수"));
    }

    // 4. 프로필 수정 - Bean Validation 실패 (이름 길이 초과)
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("프로필 수정 실패 - 이름 길이 초과 (400)")
    void updateProfile_UserNameTooLong() throws Exception {
        // given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .userName("가".repeat(51))  // 51자
            .build();

        // when & then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // 5. 프로필 수정 - 잘못된 전화번호 형식
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("프로필 수정 실패 - 전화번호 형식 오류 (400)")
    void updateProfile_InvalidPhoneNumber() throws Exception {
        // given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .phoneNumber("02-1234-5678")  // 지역번호 불가
            .build();

        // when & then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // 6. 프로필 수정 - 전화번호 중복
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("프로필 수정 실패 - 전화번호 중복 (409)")
    void updateProfile_PhoneNumberDuplicate() throws Exception {
        // given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .phoneNumber("010-9999-8888")
            .build();
        
        when(profileService.updateProfile(eq("user@test.com"), any()))
            .thenThrow(new PhoneNumberAlreadyExistsException("010-9999-8888"));

        // when & then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    // 7. 프로필 수정 - 계정 없음
    @Test
    @WithMockUser(username = "unknown@test.com")
    @DisplayName("프로필 수정 실패 - 계정 없음 (404)")
    void updateProfile_AccountNotFound() throws Exception {
        // given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .userName("홍길동")
            .build();
        
        when(profileService.updateProfile(eq("unknown@test.com"), any()))
            .thenThrow(new AccountNotFoundException("unknown@test.com"));

        // when & then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    // 8. 계정 삭제 성공
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("계정 삭제 성공 - 204 No Content")
    void deleteAccount_Success() throws Exception {
        // given
        doNothing().when(profileService).deleteAccount("user@test.com");

        // when & then
        mockMvc.perform(delete("/api/mypage/account"))
            .andExpect(status().isNoContent());
        
        verify(profileService, times(1)).deleteAccount("user@test.com");
    }

    // 9. 인증 없이 계정 삭제 시도
    @Test
    @DisplayName("인증 없이 계정 삭제 - 401 Unauthorized")
    void deleteAccount_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/mypage/account"))
            .andExpect(status().isUnauthorized());
    }

    // 10. 계정 삭제 - 계정 없음
    @Test
    @WithMockUser(username = "unknown@test.com")
    @DisplayName("계정 삭제 실패 - 계정 없음 (404)")
    void deleteAccount_AccountNotFound() throws Exception {
        // given
        doThrow(new AccountNotFoundException("unknown@test.com"))
            .when(profileService).deleteAccount("unknown@test.com");

        // when & then
        mockMvc.perform(delete("/api/mypage/account"))
            .andExpect(status().isNotFound());
    }
}
```

**총 10개 테스트**

---

## 📄 MyPageIntegrationTest (E2E 통합)

```java
package com.softwarecampus.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.repository.user.AccountRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MyPageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    // 1. 회원가입 → 로그인 → 프로필 조회
    @Test
    @DisplayName("E2E: 회원가입 → 로그인 → 프로필 조회")
    void scenario_SignupLoginGetProfile() throws Exception {
        // 1. 회원가입
        SignupRequest signupReq = SignupRequest.builder()
            .email("test@example.com")
            .password("Test1234!")
            .userName("홍길동")
            .phoneNumber("010-1111-2222")
            .accountType("USER")
            .build();
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupReq)))
            .andExpect(status().isCreated());

        // 2. 로그인
        LoginRequest loginReq = new LoginRequest("test@example.com", "Test1234!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andExpect(status().isOk())
            .andReturn();
        
        String accessToken = extractToken(loginResult);

        // 3. 프로필 조회
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.userName").value("홍길동"));
    }

    // 2-8. 추가 시나리오 (코드 생략, 아래 목록 참조)
    
    private String extractToken(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("accessToken").asText();
    }
}
```

**통합 테스트 시나리오 (8개):**
1. 회원가입 → 로그인 → 프로필 조회
2. 회원가입 → 로그인 → 프로필 수정 → 재조회
3. 프로필 수정 - 전화번호 중복 검증
4. 토큰 없이 프로필 조회 시도 (401)
5. 잘못된 토큰으로 프로필 조회 (401)
6. 회원가입 → 로그인 → 계정 삭제 → 삭제 확인
7. 계정 삭제 후 로그인 시도 (비활성 계정)
8. ACADEMY 계정 승인 후 프로필 조회

**시나리오 8 구현 예시:**
```java
@Test
@DisplayName("E2E: ACADEMY 계정 승인 후 프로필 조회")
void scenario_AcademyApprovalAndProfile() throws Exception {
    // 1. ACADEMY 회원가입
    SignupRequest signupReq = SignupRequest.builder()
        .email("academy@example.com")
        .password("Test1234!")
        .userName("소프트캠퍼스")
        .phoneNumber("010-3333-4444")
        .accountType("ACADEMY")
        .build();
    
    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(signupReq)))
        .andExpect(status().isCreated());

    // 2. 관리자가 승인 처리 (직접 DB 수정)
    Account academyAccount = accountRepository.findByEmail("academy@example.com")
        .orElseThrow();
    academyAccount.setAccountApproved(ApprovalStatus.APPROVED);
    accountRepository.save(academyAccount);

    // 3. 로그인
    LoginRequest loginReq = new LoginRequest("academy@example.com", "Test1234!");
    MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.account.approvalStatus").value("APPROVED"))
        .andReturn();
    
    String accessToken = extractToken(loginResult);

    // 4. 프로필 조회
    mockMvc.perform(get("/api/mypage/profile")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("academy@example.com"))
        .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
}
```

---

## 📊 테스트 커버리지

| 기능 | Controller 테스트 | Integration 테스트 |
|------|------------------|-------------------|
| 프로필 조회 | ✅ 성공, 인증 없음 | ✅ E2E 플로우 |
| 프로필 수정 | ✅ 성공, Validation, 중복, 404 | ✅ 수정 후 재조회 |
| 계정 삭제 | ✅ 성공, 인증 없음, 404 | ✅ 삭제 후 로그인 |

**총 테스트:** 10 (Controller) + 8 (Integration) = **18개**

---

## 📌 체크리스트

- [ ] MyPageControllerTest.java 생성 (10개)
- [ ] MyPageIntegrationTest.java 생성 (8개)
- [ ] @WithMockUser 인증 모킹
- [ ] JWT 토큰 발급 헬퍼 메서드
- [ ] 모든 테스트 통과 확인
