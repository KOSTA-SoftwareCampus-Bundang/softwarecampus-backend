package com.softwarecampus.backend.controller.academy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.service.academy.AcademyFileService;
import com.softwarecampus.backend.service.academy.AcademyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcademyController.class)
class AcademyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AcademyService academyService;

    @MockBean
    private AcademyFileService academyFileService;

    @MockBean
    private com.softwarecampus.backend.security.jwt.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.softwarecampus.backend.security.CustomUserDetailsService customUserDetailsService;

    @MockBean
    private com.softwarecampus.backend.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private com.softwarecampus.backend.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.softwarecampus.backend.security.RateLimitFilter rateLimitFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(rateLimitFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("기관 상세 조회 - 성공")
    @WithMockUser
    void getAcademyDetails_success() throws Exception {
        Long academyId = 1L;
        AcademyResponse response = AcademyResponse.builder()
                .id(academyId)
                .name("Test Academy")
                .build();

        given(academyService.getAcademyDetails(academyId)).willReturn(response);

        mockMvc.perform(get("/academies/{academyId}", academyId)
                .with(csrf()))
                .andExpect(status().isOk());

        verify(academyService).getAcademyDetails(academyId);
    }

    @Test
    @DisplayName("기관 상세 조회 - 실패 (존재하지 않거나 삭제됨)")
    @WithMockUser
    void getAcademyDetails_notFound() throws Exception {
        Long academyId = 999L;
        given(academyService.getAcademyDetails(academyId))
                .willThrow(new com.softwarecampus.backend.exception.academy.AcademyException(
                        com.softwarecampus.backend.exception.academy.AcademyErrorCode.ACADEMY_NOT_FOUND));

        mockMvc.perform(get("/academies/{academyId}", academyId)
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(academyService).getAcademyDetails(academyId);
    }

    @Test
    @DisplayName("기관 삭제 - 성공")
    @WithMockUser(roles = "ADMIN")
    void deleteAcademy_success() throws Exception {
        Long academyId = 1L;

        mockMvc.perform(delete("/academies/{id}", academyId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(academyService).deleteAcademy(academyId);
    }
}
