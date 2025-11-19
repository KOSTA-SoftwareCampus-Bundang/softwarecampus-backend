package com.softwarecampus.backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.service.user.signup.SignupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트
 * 
 * 테스트 대상:
 * - POST /api/v1/auth/signup: 회원가입
 * - GET /api/v1/auth/check-email: 이메일 중복 확인
 * 
 * 테스트 도구:
 * - @WebMvcTest: Controller Layer만 로드
 * - MockMvc: HTTP 요청/응답 모킹
 * - @MockBean: Service Layer 모킹
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private SignupService signupService;
    
    @Test
    @DisplayName("POST /signup - 회원가입 성공 (USER)")
    void signup_성공_USER() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            "서울시 강남구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        AccountResponse response = new AccountResponse(
            1L,
            "user@example.com",
            "홍길동",
            "010-1234-5678",
            AccountType.USER,
            ApprovalStatus.APPROVED,
            "서울시 강남구",
            null,
            null
        );
        
        when(signupService.signup(any(SignupRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/accounts/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.userName").value("홍길동"))
            .andExpect(jsonPath("$.accountType").value("USER"))
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - 회원가입 성공 (ACADEMY)")
    void signup_성공_ACADEMY() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "teacher@example.com",
            "password123!",
            "김선생",
            "010-9876-5432",
            "서울시 서초구",
            "ABC학원",
            "수학 강사",
            AccountType.ACADEMY,
            100L
        );
        
        AccountResponse response = new AccountResponse(
            2L,
            "teacher@example.com",
            "김선생",
            "010-9876-5432",
            AccountType.ACADEMY,
            ApprovalStatus.PENDING,
            "서울시 서초구",
            "ABC학원",
            "수학 강사"
        );
        
        when(signupService.signup(any(SignupRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/accounts/2"))
            .andExpect(jsonPath("$.accountType").value("ACADEMY"))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING"));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - Bean Validation 실패 (이메일 누락)")
    void signup_BeanValidation실패_이메일누락() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            null,  // email 누락
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
        
        // Service 호출되지 않음
        verify(signupService, never()).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - 이메일 형식 오류 (RFC 5322 위반)")
    void signup_이메일형식오류() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "invalid-email",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        // When & Then (Bean Validation이 먼저 잡음)
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("요청 본문에 유효하지 않은 필드가 있습니다."));
        
        // Bean Validation 실패로 Service 호출 안 됨
        verify(signupService, never()).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - 이메일 중복 (409 Conflict)")
    void signup_이메일중복() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        when(signupService.signup(any(SignupRequest.class)))
            .thenThrow(new DuplicateEmailException("이메일이 이미 등록되었습니다."));
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("이메일이 이미 등록되었습니다."));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - 전화번호 중복 (400 Bad Request)")
    void signup_전화번호중복() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        when(signupService.signup(any(SignupRequest.class)))
            .thenThrow(new InvalidInputException("이미 사용 중인 전화번호입니다."));
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("이미 사용 중인 전화번호입니다."));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - ADMIN 계정 차단")
    void signup_ADMIN_차단() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "admin@example.com",
            "password123!",
            "관리자",
            "010-0000-0000",
            null, null, null,
            AccountType.ADMIN,
            null
        );
        
        when(signupService.signup(any(SignupRequest.class)))
            .thenThrow(new InvalidInputException("관리자 계정은 회원가입으로 생성할 수 없습니다."));
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("관리자 계정은 회원가입으로 생성할 수 없습니다."));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - ACADEMY academyId 누락")
    void signup_ACADEMY_academyId없음() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "teacher@example.com",
            "password123!",
            "김선생",
            "010-9876-5432",
            null,
            "ABC학원",
            "강사",
            AccountType.ACADEMY,
            null  // academyId 누락
        );
        
        // When & Then (Bean Validation이 먼저 잡음)
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("요청 본문에 유효하지 않은 필드가 있습니다."));
        
        // @ValidAccountType이 먼저 검증하므로 Service 호출 안 됨
        verify(signupService, never()).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("GET /check-email - 사용 가능 (200)")
    void checkEmail_사용가능() throws Exception {
        // Given
        when(signupService.isEmailAvailable("newuser@example.com")).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/v1/auth/check-email")
                .param("email", "newuser@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("사용 가능한 이메일입니다."));
        
        verify(signupService).isEmailAvailable("newuser@example.com");
    }
    
    @Test
    @DisplayName("GET /check-email - 사용 불가 (200)")
    void checkEmail_사용불가() throws Exception {
        // Given
        when(signupService.isEmailAvailable("user@example.com")).thenReturn(false);
        
        // When & Then
        mockMvc.perform(get("/api/v1/auth/check-email")
                .param("email", "user@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        
        verify(signupService).isEmailAvailable("user@example.com");
    }
    
    @Test
    @DisplayName("GET /check-email - 이메일 형식 오류 (400)")
    void checkEmail_이메일형식오류() throws Exception {
        // Given - Bean Validation 실패 시 컨트롤러 메서드 호출 안 됨
        
        // When & Then
        mockMvc.perform(get("/api/v1/auth/check-email")
                .param("email", "invalid-email"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("올바른 이메일 형식이 아닙니다."));
        
        // Bean Validation 실패로 서비스 호출 안 됨
        verifyNoInteractions(signupService);
    }
    
    @Test
    @DisplayName("POST /signup - Location 헤더 검증")
    void signup_Location헤더검증() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        AccountResponse response = new AccountResponse(
            123L,  // accountId
            "user@example.com",
            "홍길동",
            "010-1234-5678",
            AccountType.USER,
            ApprovalStatus.APPROVED,
            null, null, null
        );
        
        when(signupService.signup(any(SignupRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/accounts/123"));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
}
