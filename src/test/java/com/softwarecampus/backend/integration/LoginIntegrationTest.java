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
 * 6. Access Token 없이 보호된 엔드포인트 접근 실패
 * 7. 잘못된 Access Token으로 접근 실패
 * 8. Refresh Token으로 Access Token 갱신 후 API 호출
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
            .andExpect(jsonPath("$.expiresIn").value(180))  // 3분 = 180초
            .andExpect(jsonPath("$.account.email").value("integrationuser@example.com"))
            .andExpect(jsonPath("$.account.userName").value("통합테스트"))
            .andExpect(jsonPath("$.account.accountType").value("USER"))
            .andExpect(jsonPath("$.account.approvalStatus").value("APPROVED"));
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
            .andExpect(jsonPath("$.approvalStatus").value("PENDING"));
        
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
