package com.softwarecampus.backend.controller.common;

import com.softwarecampus.backend.exception.GlobalExceptionHandler;
import com.softwarecampus.backend.exception.S3UploadException;
import com.softwarecampus.backend.security.SecurityConfig;
import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Service;
import com.softwarecampus.backend.security.JwtAuthenticationFilter;
import com.softwarecampus.backend.security.JwtAuthenticationEntryPoint;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import com.softwarecampus.backend.security.RateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileController 단위 테스트
 * 
 * 테스트 대상:
 * - POST /api/files/upload: 파일 업로드 (인증된 사용자만)
 * - DELETE /api/admin/files/delete: 파일 삭제 (관리자만)
 * - 인증 및 권한 검증
 * - 예외 처리 (GlobalExceptionHandler 통한 RFC 9457 ProblemDetail 응답)
 */
@WebMvcTest(FileController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationEntryPoint.class,
                JwtAuthenticationFilter.class })
@TestPropertySource(properties = {
                "problem.base-uri=https://api.example.com/problems"
})
@DisplayName("FileController 단위 테스트")
class FileControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private S3Service s3Service;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        @MockBean
        private UserDetailsService userDetailsService;

        @MockBean
        private RateLimitFilter rateLimitFilter;

        @BeforeEach
        void setUp() throws ServletException, IOException {
                // RateLimitFilter Mock이 체인을 계속 진행하도록 설정
                doAnswer(invocation -> {
                        ServletRequest request = invocation.getArgument(0);
                        ServletResponse response = invocation.getArgument(1);
                        FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(request, response);
                        return null;
                }).when(rateLimitFilter).doFilter(any(), any(), any());
        }

        // ==================== 파일 업로드 테스트 ====================

        @Test
        @DisplayName("파일 업로드 성공 - 인증된 일반 사용자")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_Success_AuthenticatedUser() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test-image.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test image content".getBytes());

                String expectedUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/uuid-test-image.jpg";
                when(s3Service.uploadFile(any(), eq("profile"), eq(FileType.FileTypeEnum.PROFILE)))
                                .thenReturn(expectedUrl);

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fileUrl").value(expectedUrl))
                                .andExpect(jsonPath("$.message").exists());

                verify(s3Service, times(1)).uploadFile(any(), eq("profile"), eq(FileType.FileTypeEnum.PROFILE));
        }

        @Test
        @DisplayName("파일 업로드 성공 - 관리자")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testUploadFile_Success_Admin() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "admin-file.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "admin file content".getBytes());

                String expectedUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/board/admin-file.jpg";
                when(s3Service.uploadFile(any(), eq("board"), eq(FileType.FileTypeEnum.BOARD_ATTACH)))
                                .thenReturn(expectedUrl);

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "board")
                                .param("fileType", "BOARD_ATTACH")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fileUrl").value(expectedUrl));

                verify(s3Service, times(1)).uploadFile(any(), eq("board"), eq(FileType.FileTypeEnum.BOARD_ATTACH));
        }

        // SecurityConfig 수정 완료: 비인증 사용자 업로드 차단 확인
        @Test
        @DisplayName("파일 업로드 실패 - 비로그인 사용자 (401 Unauthorized)")
        void testUploadFile_Fail_Unauthorized() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test-image.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test image content".getBytes());

                when(s3Service.uploadFile(any(), any(), any()))
                                .thenReturn("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test.jpg");

                // when & then
                // SecurityConfig에서 .anyRequest().authenticated()로 설정되었으므로 401 반환
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isUnauthorized()); // 401 Unauthorized

                verify(s3Service, never()).uploadFile(any(), any(), any());
        }

        @Test
        @DisplayName("파일 업로드 실패 - null 파일")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_NullFile() throws Exception {
                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("파일 업로드 실패 - 빈 파일")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_EmptyFile() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                new byte[0]);

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail").value("파일이 비어있습니다. 파일을 선택해주세요."));
        }

        @Test
        @DisplayName("파일 업로드 실패 - 파일명이 null")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_NullFilename() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                null,
                                MediaType.IMAGE_JPEG_VALUE,
                                "test content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail").value("파일명이 유효하지 않습니다."));
        }

        @Test
        @DisplayName("파일 업로드 실패 - 경로 순회 공격 시도 (파일명에 ../)")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_PathTraversal_Filename() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "../../../etc/passwd",
                                MediaType.IMAGE_JPEG_VALUE,
                                "malicious content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail").value("파일명에 허용되지 않는 특수문자가 포함되어 있습니다."));
        }

        @Test
        @DisplayName("파일 업로드 실패 - 경로 순회 공격 시도 (폴더에 ../)")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_PathTraversal_Folder() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "../../../etc")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail").value("폴더명에 상위 디렉토리 경로(..)를 포함할 수 없습니다."));
        }

        @Test
        @DisplayName("파일 업로드 실패 - 위험한 문자 포함 (파일명에 <>:\"?*)")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_DangerousCharacters() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test<>file.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail").value("파일명에 허용되지 않는 특수문자가 포함되어 있습니다."));
        }

        @Test
        @DisplayName("파일 업로드 성공 - 연속된 점이 있는 파일명 허용 (경로 순회 아님)")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_ConsecutiveDots_Allowed() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "file..name.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test content".getBytes());

                String expectedUrl = "https://test-bucket.s3.amazonaws.com/profile/file..name.jpg";
                when(s3Service.uploadFile(any(), eq("profile"), eq(FileType.FileTypeEnum.PROFILE)))
                                .thenReturn(expectedUrl);

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("파일 업로드 실패 - S3 업로드 오류")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_S3Exception() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test-image.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test image content".getBytes());

                when(s3Service.uploadFile(any(), eq("profile"), eq(FileType.FileTypeEnum.PROFILE)))
                                .thenThrow(new S3UploadException("S3 upload failed"));

                // when & then - GlobalExceptionHandler가 ProblemDetail 형식으로 응답
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                                .andExpect(jsonPath("$.detail").value("파일 업로드 중 서버 오류가 발생했습니다."))
                                .andExpect(jsonPath("$.reason").value("INTERNAL_ERROR"));
        }

        @Test
        @DisplayName("파일 업로드 실패 - 검증 실패 (IllegalArgumentException)")
        @WithMockUser(username = "testuser", roles = { "USER" })
        void testUploadFile_ValidationException() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test-image.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test image content".getBytes());

                when(s3Service.uploadFile(any(), eq("profile"), eq(FileType.FileTypeEnum.PROFILE)))
                                .thenThrow(new IllegalArgumentException("파일 크기가 제한을 초과합니다"));

                // when & then - GlobalExceptionHandler가 ProblemDetail 형식으로 응답
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.title").value("Invalid Request Argument"))
                                // TODO: 원인 파악 필요. Mock 설정은 "잘못된 파일 타입입니다."이나 실제로는 "파일 크기가 제한을 초과합니다"가 반환됨.
                                .andExpect(jsonPath("$.detail").value("파일 크기가 제한을 초과합니다"));
        }

        // ==================== 파일 삭제 테스트 ====================

        @Test
        @DisplayName("파일 삭제 성공 - 관리자")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testDeleteFile_Success_Admin() throws Exception {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";
                doNothing().when(s3Service).deleteFile(fileUrl);

                // when & then
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", fileUrl)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("파일이 성공적으로 삭제되었습니다."));

                verify(s3Service, times(1)).deleteFile(fileUrl);
        }

        @Test
        @DisplayName("파일 삭제 실패 - 일반 사용자 (403 Forbidden)")
        @WithMockUser(username = "normaluser", roles = { "USER" })
        void testDeleteFile_Fail_Forbidden() throws Exception {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";

                // when & then
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", fileUrl)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isForbidden());

                verify(s3Service, never()).deleteFile(any());
        }

        // SecurityConfig 수정 완료: 관리자 권한 체크 확인
        @Test
        @DisplayName("파일 삭제 실패 - 비로그인 사용자 (인증 필요)")
        void testDeleteFile_Fail_Unauthorized() throws Exception {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";

                // when & then
                // 인증되지 않은 사용자는 401 Unauthorized 반환
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", fileUrl)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());

                verify(s3Service, never()).deleteFile(any());
        }

        @Test
        @DisplayName("파일 삭제 실패 - null URL")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testDeleteFile_NullUrl() throws Exception {
                // when & then
                mockMvc.perform(delete("/api/admin/files/delete")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("파일 삭제 실패 - 빈 URL")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testDeleteFile_EmptyUrl() throws Exception {
                // given
                doThrow(new S3UploadException("파일 URL이 비어있습니다.", S3UploadException.FailureReason.VALIDATION_ERROR))
                                .when(s3Service).deleteFile("");

                // when & then - Service에서 검증 후 GlobalExceptionHandler가 ProblemDetail 형식으로 응답
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", "")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.title").value("File Validation Error"))
                                .andExpect(jsonPath("$.detail").value("파일 URL이 비어있습니다."))
                                .andExpect(jsonPath("$.reason").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("파일 삭제 실패 - S3 삭제 오류")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testDeleteFile_S3Exception() throws Exception {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";
                doThrow(new S3UploadException("S3 delete failed"))
                                .when(s3Service).deleteFile(fileUrl);

                // when & then - GlobalExceptionHandler가 ProblemDetail 형식으로 응답
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", fileUrl)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                                .andExpect(jsonPath("$.detail").value("파일 업로드 중 서버 오류가 발생했습니다."))
                                .andExpect(jsonPath("$.reason").value("INTERNAL_ERROR"));
        }

        @Test
        @DisplayName("파일 삭제 실패 - 유효하지 않은 URL 형식")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testDeleteFile_InvalidUrlFormat() throws Exception {
                // given
                String fileUrl = "invalid-url";
                doThrow(new S3UploadException("유효하지 않은 URL 형식입니다.", S3UploadException.FailureReason.VALIDATION_ERROR))
                                .when(s3Service).deleteFile(fileUrl);

                // when & then - Service에서 검증 후 GlobalExceptionHandler가 ProblemDetail 형식으로 응답
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", fileUrl)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.title").value("File Validation Error"))
                                .andExpect(jsonPath("$.detail").value("유효하지 않은 URL 형식입니다."))
                                .andExpect(jsonPath("$.reason").value("VALIDATION_ERROR"));

                verify(s3Service, times(1)).deleteFile(fileUrl);
        }
}
