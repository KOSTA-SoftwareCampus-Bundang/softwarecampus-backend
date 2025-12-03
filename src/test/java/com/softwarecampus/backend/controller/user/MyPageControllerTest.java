package com.softwarecampus.backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.PhoneNumberAlreadyExistsException;
import com.softwarecampus.backend.security.RateLimitFilter;
import com.softwarecampus.backend.security.jwt.JwtProperties;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import com.softwarecampus.backend.service.user.profile.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MyPageController 슬라이스 테스트
 * 
 * 테스트 대상:
 * - GET /api/mypage/profile: 프로필 조회
 * - PATCH /api/mypage/profile: 프로필 수정
 * - DELETE /api/mypage/account: 계정 삭제
 * 
 * 테스트 도구:
 * - @WebMvcTest: Controller Layer만 로드
 * - @WithMockUser: Security 인증 모킹
 * - MockMvc: HTTP 요청/응답 모킹
 * - @MockBean: Service Layer 모킹
 */
@WebMvcTest(MyPageController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MyPageController 슬라이스 테스트")
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    // Security 관련 Mock
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    // ========================================
    // 프로필 조회 테스트 (3개)
    // ========================================

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /profile - 프로필 조회 성공")
    void getProfile_Success() throws Exception {
        // Given
        AccountResponse response = new AccountResponse(
                1L,
                "user@test.com",
                "홍길동",
                "010-1234-5678",
                AccountType.USER,
                ApprovalStatus.APPROVED,
                "서울시 강남구",
                "소프트캔퍼스",
                "개발자",
                null,
                null,
                null,
                0,
                0);

        when(profileService.getAccountByEmail("user@test.com")).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/mypage/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.userName").value("홍길동"))
                .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
                .andExpect(jsonPath("$.accountType").value("USER"))
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        verify(profileService, times(1)).getAccountByEmail("user@test.com");
    }

    @Test
    @DisplayName("GET /profile - 인증 없음 (401) - @WebMvcTest에서는 Skip")
    @org.junit.jupiter.api.Disabled("@WebMvcTest는 Security 필터 비활성화되어 테스트 불가, Integration 테스트에서 검증")
    void getProfile_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/mypage/profile"))
                .andExpect(status().isUnauthorized());

        verify(profileService, never()).getAccountByEmail(anyString());
    }

    @Test
    @WithMockUser(username = "unknown@test.com")
    @DisplayName("GET /profile - 계정 없음 (404)")
    void getProfile_AccountNotFound() throws Exception {
        // Given
        when(profileService.getAccountByEmail("unknown@test.com"))
                .thenThrow(new AccountNotFoundException("계정을 찾을 수 없습니다."));

        // When & Then
        mockMvc.perform(get("/api/mypage/profile"))
                .andExpect(status().isNotFound());

        verify(profileService, times(1)).getAccountByEmail("unknown@test.com");
    }

    // ========================================
    // 프로필 수정 테스트 (5개)
    // ========================================

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PATCH /profile - 프로필 수정 성공 (모든 필드)")
    void updateProfile_Success_AllFields() throws Exception {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .userName("김철수")
                .phoneNumber("010-9999-8888")
                .address("부산시 해운대구")
                .affiliation("부산대학교")
                .position("교수")
                .build();

        AccountResponse response = new AccountResponse(
                1L,
                "user@test.com",
                "김철수",
                "010-9999-8888",
                AccountType.USER,
                ApprovalStatus.APPROVED,
                "부산시 해운대구",
                "부산대학교",
                "교수",
                null,
                null,
                null,
                0,
                0);

        when(profileService.updateProfile(eq("user@test.com"), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("김철수"))
                .andExpect(jsonPath("$.phoneNumber").value("010-9999-8888"))
                .andExpect(jsonPath("$.address").value("부산시 해운대구"))
                .andExpect(jsonPath("$.affiliation").value("부산대학교"))
                .andExpect(jsonPath("$.position").value("교수"));

        verify(profileService, times(1)).updateProfile(eq("user@test.com"), any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PATCH /profile - 부분 수정 (이름만)")
    void updateProfile_Success_PartialUpdate() throws Exception {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .userName("이영희")
                .build();

        AccountResponse response = new AccountResponse(
                1L,
                "user@test.com",
                "이영희",
                "010-1234-5678",
                AccountType.USER,
                ApprovalStatus.APPROVED,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0);

        when(profileService.updateProfile(eq("user@test.com"), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("이영희"));

        verify(profileService, times(1)).updateProfile(eq("user@test.com"), any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PATCH /profile - Bean Validation 실패: 이름 길이 초과 (400)")
    void updateProfile_ValidationFail_UserNameTooLong() throws Exception {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .userName("가".repeat(51)) // 51자 (최대 50자)
                .build();

        // When & Then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).updateProfile(anyString(), any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PATCH /profile - Bean Validation 실패: 전화번호 형식 오류 (400)")
    void updateProfile_ValidationFail_InvalidPhoneNumber() throws Exception {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phoneNumber("02-1234-5678") // 지역번호 불가
                .build();

        // When & Then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).updateProfile(anyString(), any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PATCH /profile - 전화번호 중복 (409)")
    void updateProfile_PhoneNumberDuplicate() throws Exception {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .phoneNumber("010-9999-8888")
                .build();

        when(profileService.updateProfile(eq("user@test.com"), any(UpdateProfileRequest.class)))
                .thenThrow(new PhoneNumberAlreadyExistsException("010-9999-8888"));

        // When & Then
        mockMvc.perform(patch("/api/mypage/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(profileService, times(1)).updateProfile(eq("user@test.com"), any(UpdateProfileRequest.class));
    }

    // ========================================
    // 계정 삭제 테스트 (2개)
    // ========================================

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /account - 계정 삭제 성공 (204)")
    void deleteAccount_Success() throws Exception {
        // Given
        doNothing().when(profileService).deleteAccount("user@test.com");

        // When & Then
        mockMvc.perform(delete("/api/mypage/account"))
                .andExpect(status().isNoContent());

        verify(profileService, times(1)).deleteAccount("user@test.com");
    }

    @Test
    @DisplayName("DELETE /account - 인증 없음 (401) - @WebMvcTest에서는 Skip")
    @org.junit.jupiter.api.Disabled("@WebMvcTest는 Security 필터 비활성화되어 테스트 불가, Integration 테스트에서 검증")
    void deleteAccount_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/mypage/account"))
                .andExpect(status().isUnauthorized());

        verify(profileService, never()).deleteAccount(anyString());
    }
}
