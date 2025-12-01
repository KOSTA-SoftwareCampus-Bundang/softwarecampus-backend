package com.softwarecampus.backend.controller.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseRequestDTO;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;
import com.softwarecampus.backend.service.course.CourseService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CourseService courseService;

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
        @DisplayName("과정 승인 - 성공")
        @WithMockUser(roles = "ADMIN")
        void approveCourse_success() throws Exception {
                Long courseId = 1L;
                CourseResponseDTO responseDTO = new CourseResponseDTO();
                given(courseService.approveCourse(courseId)).willReturn(responseDTO);

                mockMvc.perform(post("/api/courses/{courseId}/approve", courseId)
                                .with(csrf()))
                                .andExpect(status().isOk());

                verify(courseService).approveCourse(courseId);
        }

        @Test
        @DisplayName("과정 수정 - 성공")
        @WithMockUser(roles = "ADMIN")
        void updateCourse_success() throws Exception {
                Long courseId = 1L;
                CourseRequestDTO requestDTO = CourseRequestDTO.builder()
                                .academyId(1L)
                                .categoryType(CategoryType.JOB_SEEKER)
                                .categoryName("IT")
                                .name("Updated Course")
                                .build();
                CourseResponseDTO responseDTO = new CourseResponseDTO();

                given(courseService.updateCourse(eq(courseId), any(CourseRequestDTO.class))).willReturn(responseDTO);

                mockMvc.perform(put("/api/courses/{courseId}", courseId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk());

                verify(courseService).updateCourse(eq(courseId), any(CourseRequestDTO.class));
        }

        @Test
        @DisplayName("과정 삭제 - 성공")
        @WithMockUser(roles = "ADMIN")
        void deleteCourse_success() throws Exception {
                Long courseId = 1L;

                mockMvc.perform(delete("/api/courses/{courseId}", courseId)
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(courseService).deleteCourse(courseId);
        }

        @Test
        @DisplayName("과정 등록 요청 - 성공")
        @WithMockUser(roles = "ACADEMY")
        void requestCourse_success() throws Exception {
                CourseRequestDTO requestDTO = CourseRequestDTO.builder()
                                .academyId(1L)
                                .categoryType(CategoryType.JOB_SEEKER)
                                .categoryName("IT")
                                .name("New Course")
                                .build();
                CourseResponseDTO responseDTO = new CourseResponseDTO();

                given(courseService.requestCourseRegistration(any(CourseRequestDTO.class), any(Long.class))).willReturn(responseDTO);

                mockMvc.perform(post("/api/courses/request")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk());

                verify(courseService).requestCourseRegistration(any(CourseRequestDTO.class), any(Long.class));
        }

        @Test
        @DisplayName("과정 상세 조회 - 성공")
        @WithMockUser
        void getCourseDetail_success() throws Exception {
                Long courseId = 1L;
                com.softwarecampus.backend.dto.course.CourseDetailResponseDTO responseDTO = com.softwarecampus.backend.dto.course.CourseDetailResponseDTO
                                .builder()
                                .id(courseId)
                                .name("Course Detail")
                                .build();

                given(courseService.getCourseDetail(courseId)).willReturn(responseDTO);

                mockMvc.perform(get("/api/courses/{courseId}", courseId)
                                .with(csrf()))
                                .andExpect(status().isOk());

                verify(courseService).getCourseDetail(courseId);
        }

        @Test
        @DisplayName("과정 상세 조회 - 실패 (존재하지 않거나 삭제됨)")
        @WithMockUser
        void getCourseDetail_notFound() throws Exception {
                Long courseId = 999L;
                given(courseService.getCourseDetail(courseId))
                                .willThrow(new jakarta.persistence.EntityNotFoundException("해당 과정이 존재하지 않습니다."));

                mockMvc.perform(get("/api/courses/{courseId}", courseId)
                                .with(csrf()))
                                .andExpect(status().isNotFound());

                verify(courseService).getCourseDetail(courseId);
        }
}
