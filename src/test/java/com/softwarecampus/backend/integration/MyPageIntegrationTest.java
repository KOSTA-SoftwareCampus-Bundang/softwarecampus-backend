package com.softwarecampus.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
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
 * 마이페이지 통합 테스트
 * 
 * 테스트 시나리오:
 * 1. 회원가입 → 로그인 → 프로필 조회 성공
 * 2. 회원가입 → 로그인 → 프로필 전체 수정 → 재조회 검증
 * 3. 전화번호 중복 검증
 * 4. 토큰 없이 프로필 조회 실패 (401)
 * 5. 잘못된 토큰으로 프로필 조회 실패 (401)
 * 6. 계정 삭제 → 로그인 시도 (비활성 계정)
 * 7. ACADEMY 계정 승인 후 프로필 조회
 * 8. Refresh Token 갱신 후 프로필 수정
 * 
 * @author 태윤
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("마이페이지 통합 테스트")
class MyPageIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AccountRepository accountRepository;
    
    private SignupRequest userSignupRequest;
    private LoginRequest userLoginRequest;
    private String accessToken;
    private String refreshToken;
    
    @BeforeEach
    void setUp() {
        userSignupRequest = new SignupRequest(
            "mypage@example.com",
            "Password123!",
            "마이페이지",
            "010-1234-5678",
            "서울시 강남구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        userLoginRequest = new LoginRequest(
            "mypage@example.com",
            "Password123!"
        );
    }
    
    /**
     * 회원가입 → 로그인하여 JWT 토큰 발급
     */
    private void signupAndLogin() throws Exception {
        // 회원가입
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignupRequest)))
            .andExpect(status().isCreated());
        
        // 로그인하여 토큰 발급
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        
        String loginResponse = loginResult.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(loginResponse).get("accessToken").asText();
        refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();
    }
    
    @Test
    @DisplayName("시나리오 1: 회원가입 → 로그인 → 프로필 조회 성공")
    void scenario_SignupLoginAndGetProfile_Success() throws Exception {
        // 1. 회원가입 → 로그인
        signupAndLogin();
        
        // 2. 프로필 조회
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("mypage@example.com"))
            .andExpect(jsonPath("$.userName").value("마이페이지"))
            .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
            .andExpect(jsonPath("$.address").value("서울시 강남구"))
            .andExpect(jsonPath("$.accountType").value("USER"))
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
    }
    
    @Test
    @DisplayName("시나리오 2: 회원가입 → 로그인 → 프로필 전체 수정 → 재조회 검증")
    void scenario_UpdateProfileAndVerify_Success() throws Exception {
        // 1. 회원가입 → 로그인
        signupAndLogin();
        
        // 2. 프로필 수정 (모든 필드)
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
            .userName("수정된이름")
            .phoneNumber("010-9999-8888")
            .address("부산시 해운대구")
            .affiliation("소프트웨어캠퍼스")
            .position("백엔드 개발자")
            .build();
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("수정된이름"))
            .andExpect(jsonPath("$.phoneNumber").value("010-9999-8888"))
            .andExpect(jsonPath("$.address").value("부산시 해운대구"))
            .andExpect(jsonPath("$.affiliation").value("소프트웨어캠퍼스"))
            .andExpect(jsonPath("$.position").value("백엔드 개발자"));
        
        // 3. 재조회하여 수정 확인
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("수정된이름"))
            .andExpect(jsonPath("$.phoneNumber").value("010-9999-8888"))
            .andExpect(jsonPath("$.address").value("부산시 해운대구"))
            .andExpect(jsonPath("$.affiliation").value("소프트웨어캠퍼스"))
            .andExpect(jsonPath("$.position").value("백엔드 개발자"));
    }
    
    @Test
    @DisplayName("시나리오 3: 전화번호 중복 검증")
    void scenario_PhoneNumberDuplicate_Conflict() throws Exception {
        // 1. 첫 번째 계정 회원가입
        signupAndLogin();
        
        // 2. 두 번째 계정 회원가입
        SignupRequest secondUser = new SignupRequest(
            "second@example.com",
            "Password123!",
            "두번째사용자",
            "010-7777-6666",
            "인천시 남동구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondUser)))
            .andExpect(status().isCreated());
        
        LoginRequest secondLogin = new LoginRequest("second@example.com", "Password123!");
        MvcResult secondLoginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondLogin)))
            .andExpect(status().isOk())
            .andReturn();
        
        String secondToken = objectMapper.readTree(secondLoginResult.getResponse().getContentAsString())
            .get("accessToken").asText();
        
        // 3. 두 번째 계정에서 첫 번째 계정의 전화번호로 수정 시도 (중복)
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
            .phoneNumber("010-1234-5678")  // 첫 번째 계정의 번호
            .build();
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + secondToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Duplicate Phone Number"))
            .andExpect(jsonPath("$.detail").value("이미 사용 중인 전화번호입니다."));
    }
    
    @Test
    @DisplayName("시나리오 4: 토큰 없이 프로필 조회 실패 (401)")
    void scenario_GetProfileWithoutToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/mypage/profile"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("시나리오 5: 잘못된 토큰으로 프로필 조회 실패 (401)")
    void scenario_GetProfileWithInvalidToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer invalid.token.here"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("시나리오 6: 계정 삭제 → 로그인 시도 (비활성 계정)")
    void scenario_DeleteAccountAndTryLogin_Inactive() throws Exception {
        // 1. 회원가입 → 로그인
        signupAndLogin();
        
        // 2. 계정 삭제
        mockMvc.perform(delete("/api/mypage/account")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNoContent());
        
        // 3. 삭제된 계정으로 로그인 시도 (비활성 계정)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value("비활성화된 계정입니다"));
    }
    
    @Test
    @DisplayName("시나리오 7: ACADEMY 계정 승인 후 프로필 조회")
    void scenario_AcademyAccountApprovalAndProfile_Success() throws Exception {
        // 1. ACADEMY 계정 회원가입
        SignupRequest academySignup = new SignupRequest(
            "academy@example.com",
            "Password123!",
            "학원계정",
            "010-5555-4444",
            "대전시 유성구",
            "소프트웨어학원",
            "원장",
            AccountType.ACADEMY,
            1L  // academyId는 ACADEMY 타입에 필수
        );
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(academySignup)))
            .andExpect(status().isCreated());
        
        // 2. 관리자가 ACADEMY 계정 승인 (수동)
        Account academyAccount = accountRepository.findByEmail("academy@example.com")
            .orElseThrow();
        academyAccount.setAccountApproved(ApprovalStatus.APPROVED);
        accountRepository.save(academyAccount);
        
        // 3. 승인된 ACADEMY 계정 로그인
        LoginRequest academyLogin = new LoginRequest("academy@example.com", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(academyLogin)))
            .andExpect(status().isOk())
            .andReturn();
        
        String academyToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
            .get("accessToken").asText();
        
        // 4. 프로필 조회
        mockMvc.perform(get("/api/mypage/profile")
                .header("Authorization", "Bearer " + academyToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("academy@example.com"))
            .andExpect(jsonPath("$.userName").value("학원계정"))
            .andExpect(jsonPath("$.accountType").value("ACADEMY"))
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
            .andExpect(jsonPath("$.affiliation").value("소프트웨어학원"))
            .andExpect(jsonPath("$.position").value("원장"));
    }
    
    @Test
    @DisplayName("시나리오 8: Refresh Token 갱신 후 프로필 수정")
    void scenario_RefreshTokenAndUpdateProfile_Success() throws Exception {
        // 1. 회원가입 → 로그인
        signupAndLogin();
        
        // 2. Refresh Token으로 Access Token 갱신
        String refreshRequestBody = String.format("{\"email\": \"%s\", \"refreshToken\": \"%s\"}", 
            userSignupRequest.email(), refreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer " + accessToken)  // 기존 Access Token 필요
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshRequestBody))
            .andExpect(status().isOk())
            .andReturn();
        
        String newAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
            .get("accessToken").asText();
        
        // 3. 새 Access Token으로 프로필 수정
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
            .userName("갱신후수정")
            .position("프론트엔드 개발자")
            .build();
        
        mockMvc.perform(patch("/api/mypage/profile")
                .header("Authorization", "Bearer " + newAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("갱신후수정"))
            .andExpect(jsonPath("$.position").value("프론트엔드 개발자"));
    }
}
