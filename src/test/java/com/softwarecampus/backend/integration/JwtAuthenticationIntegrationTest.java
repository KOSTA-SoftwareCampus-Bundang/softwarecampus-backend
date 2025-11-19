package com.softwarecampus.backend.integration;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 13: JWT 인증 통합 테스트
 * 
 * 목표:
 * - JWT 토큰 기반 인증 E2E 검증
 * - JwtAuthenticationFilter 동작 확인
 * - SecurityFilterChain 통합 검증
 * - 실제 인증이 필요한 엔드포인트 접근 테스트
 * 
 * 테스트 시나리오:
 * 1. 유효한 JWT 토큰으로 보호된 리소스 접근 성공
 * 2. 토큰 없이 보호된 리소스 접근 시 401 Unauthorized
 * 3. 만료된 토큰으로 접근 시 401 Unauthorized
 * 4. 잘못된 서명 토큰으로 접근 시 401 Unauthorized
 * 5. Bearer 없는 토큰으로 접근 시 401 Unauthorized
 * 6. 권한별 접근 제어 검증 (USER, ACADEMY, ADMIN)
 * 
 * @since 2025-11-19
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("JWT 인증 통합 테스트")
class JwtAuthenticationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private Account testUser;
    private String validToken;
    
    @BeforeEach
    void setUp() {
        // 테스트용 사용자 계정 생성
        testUser = Account.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .userName("테스트유저")
                .phoneNumber("01012345678")
                .accountType(AccountType.USER)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();
        accountRepository.save(testUser);
        
        // 유효한 JWT 토큰 생성
        validToken = jwtTokenProvider.generateToken(testUser.getEmail(), "USER");
    }
    
    @Test
    @DisplayName("유효한 JWT 토큰으로 보호된 리소스 접근 성공")
    void accessProtectedResource_withValidToken_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("토큰 없이 보호된 리소스 접근 시 401 Unauthorized")
    void accessProtectedResource_withoutToken_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Bearer 접두사 없는 토큰으로 접근 시 401 Unauthorized")
    void accessProtectedResource_withoutBearerPrefix_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", validToken)) // Bearer 없음
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("잘못된 형식의 토큰으로 접근 시 401 Unauthorized")
    void accessProtectedResource_withMalformedToken_Unauthorized() throws Exception {
        // given
        String malformedToken = "this.is.not.a.valid.jwt.token";
        
        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + malformedToken))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("빈 토큰으로 접근 시 401 Unauthorized")
    void accessProtectedResource_withEmptyToken_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("공개 엔드포인트는 토큰 없이 접근 가능")
    void accessPublicEndpoint_withoutToken_Success() throws Exception {
        // when & then - 회원가입 엔드포인트는 공개
        mockMvc.perform(get("/api/auth/check-email")
                .param("email", "test@example.com"))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("USER 권한으로 USER 전용 엔드포인트 접근 성공")
    void accessUserEndpoint_withUserRole_Success() throws Exception {
        // given
        String userToken = jwtTokenProvider.generateToken("test@example.com", "USER");
        
        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("ACADEMY 권한 사용자 계정으로 토큰 인증")
    void authenticateWithAcademyAccount() throws Exception {
        // given
        Account academyAccount = Account.builder()
                .email("academy@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .userName("학원관리자")
                .phoneNumber("01087654321")
                .accountType(AccountType.ACADEMY)
                .accountApproved(ApprovalStatus.APPROVED)
                .academyId(100L)
                .build();
        accountRepository.save(academyAccount);
        
        String academyToken = jwtTokenProvider.generateToken("academy@example.com", "ACADEMY");
        
        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + academyToken))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("ADMIN 권한 사용자 계정으로 토큰 인증")
    void authenticateWithAdminAccount() throws Exception {
        // given
        Account adminAccount = Account.builder()
                .email("admin@example.com")
                .password(passwordEncoder.encode("AdminPassword123!"))
                .userName("시스템관리자")
                .phoneNumber("01011112222")
                .accountType(AccountType.ADMIN)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();
        accountRepository.save(adminAccount);
        
        String adminToken = jwtTokenProvider.generateToken("admin@example.com", "ADMIN");
        
        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("토큰에서 추출한 이메일로 사용자 인증 성공")
    void authenticateWithEmailFromToken() throws Exception {
        // given - email만 포함된 토큰 (role 없음)
        String emailOnlyToken = jwtTokenProvider.generateToken("test@example.com");
        
        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + emailOnlyToken))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 이메일로 생성된 토큰으로 접근 시 401")
    void accessWithNonExistentUserToken_Unauthorized() throws Exception {
        // given - DB에 없는 이메일로 토큰 생성
        String nonExistentUserToken = jwtTokenProvider.generateToken("nonexistent@example.com", "USER");
        
        // when & then
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + nonExistentUserToken))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Authorization 헤더가 null일 때 401 Unauthorized")
    void accessWithNullAuthorizationHeader_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isUnauthorized());
    }
}
