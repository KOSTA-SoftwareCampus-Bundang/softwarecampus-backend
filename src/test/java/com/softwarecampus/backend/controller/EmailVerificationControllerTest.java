package com.softwarecampus.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.model.dto.email.EmailVerificationCodeRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationRequest;
import com.softwarecampus.backend.model.dto.email.EmailVerificationResponse;
import com.softwarecampus.backend.service.email.EmailVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EmailVerificationController 통합 테스트
 * 
 * @apiNote @SpringBootTest를 사용하여 전체 애플리케이션 컨텍스트를 로드하는 통합 테스트입니다.
 *          Security 필터는 테스트 간소화를 위해 비활성화되었습니다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
@ActiveProfiles("test")
@DisplayName("이메일 인증 Controller 테스트")
class EmailVerificationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private EmailVerificationService verificationService;
    
    @Test
    @DisplayName("POST /api/auth/email/send-verification - 성공")
    void sendSignupVerification_ShouldReturn200() throws Exception {
        // given
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .email("test@example.com")
                .build();
        
        EmailVerificationResponse response = EmailVerificationResponse.withExpiry(
                "인증 코드가 발송되었습니다",
                180
        );
        
        when(verificationService.sendVerificationCode(any())).thenReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/auth/email/send-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다"))
                .andExpect(jsonPath("$.expiresIn").value(180));
    }
    
    @Test
    @DisplayName("POST /api/auth/email/send-verification - 이메일 형식 오류")
    void sendSignupVerification_WithInvalidEmail_ShouldReturn400() throws Exception {
        // given
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .email("invalid-email") // 잘못된 형식
                .build();
        
        // when & then
        mockMvc.perform(post("/api/auth/email/send-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("POST /api/auth/email/verify - 성공")
    void verifySignupCode_ShouldReturn200() throws Exception {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email("test@example.com")
                .code("123456")
                .build();
        
        EmailVerificationResponse response = EmailVerificationResponse.success(
                "이메일 인증이 완료되었습니다"
        );
        
        when(verificationService.verifyCode(any())).thenReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다"));
    }
    
    @Test
    @DisplayName("POST /api/auth/email/verify - 코드 형식 오류")
    void verifySignupCode_WithInvalidCodeFormat_ShouldReturn400() throws Exception {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email("test@example.com")
                .code("12345") // 5자리 (잘못된 형식)
                .build();
        
        // when & then
        mockMvc.perform(post("/api/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("POST /api/auth/email/send-reset-code - 성공")
    void sendPasswordResetCode_ShouldReturn200() throws Exception {
        // given
        EmailVerificationRequest request = EmailVerificationRequest.builder()
                .email("test@example.com")
                .build();
        
        EmailVerificationResponse response = EmailVerificationResponse.withExpiry(
                "인증 코드가 발송되었습니다",
                180
        );
        
        when(verificationService.sendVerificationCode(any())).thenReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/auth/email/send-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.expiresIn").value(180));
    }
    
    @Test
    @DisplayName("POST /api/auth/email/verify-reset - 성공")
    void verifyPasswordResetCode_ShouldReturn200() throws Exception {
        // given
        EmailVerificationCodeRequest request = EmailVerificationCodeRequest.builder()
                .email("test@example.com")
                .code("654321")
                .build();
        
        EmailVerificationResponse response = EmailVerificationResponse.success(
                "인증이 완료되었습니다. 새 비밀번호를 설정하세요"
        );
        
        when(verificationService.verifyResetCode(any())).thenReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/auth/email/verify-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
