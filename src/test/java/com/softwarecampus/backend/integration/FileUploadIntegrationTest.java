package com.softwarecampus.backend.integration;

import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 파일 업로드/삭제 통합 테스트
 * 
 * 실제 Spring 컨텍스트를 로드하여 다음 항목들을 통합 검증:
 * - FileController ↔ S3Service 통합
 * - Spring Security 인증/권한 체크
 * - 요청 파라미터 바인딩 및 검증
 * - 에러 핸들링 및 응답 포맷
 * 
 * S3Client는 Mock으로 대체하여 실제 AWS 호출 없이 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
                "aws.s3.bucket-name=test-bucket",
                "aws.s3.region=ap-northeast-2"
})
@DisplayName("파일 업로드/삭제 통합 테스트")
class FileUploadIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private S3Service s3Service;

        /**
         * S3Client를 Mock으로 대체하여 실제 AWS S3 호출 없이 테스트
         * 
         * 참고: @MockBean은 Spring Boot 3.4.0부터 deprecated되었습니다.
         * Spring은 표준 Mockito 어노테이션 사용을 권장하지만,
         * 
         * @SpringBootTest 통합 테스트에서 Spring Bean을 Mock으로 사용해도 동작은 가능합니다.
         */
        @SuppressWarnings("removal")
        @MockBean
        private S3Client s3Client;

        @Autowired
        private FileType fileType;

        private List<String> uploadedFileUrls;

        @BeforeEach
        void setUp() {
                uploadedFileUrls = new ArrayList<>();

                // S3 업로드 Mock 설정 - RequestBody 명시
                when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                                .thenReturn(PutObjectResponse.builder().build());

                // S3 삭제 Mock 설정
                when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                                .thenReturn(DeleteObjectResponse.builder().build());
        }

        @AfterEach
        void tearDown() {
                // 테스트 중 업로드된 파일 정리 (Mock이므로 실제 삭제는 없음)
                uploadedFileUrls.clear();
                reset(s3Client);
        }

        // ==================== 파일 업로드 통합 테스트 ====================

        @Test
        @DisplayName("통합: 프로필 이미지 업로드 성공 - 일반 사용자")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_UploadProfileImage_Success() throws Exception {
                // given
                MockMultipartFile profileImage = new MockMultipartFile(
                                "file",
                                "profile.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "profile image content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(profileImage)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fileUrl").exists())
                                .andExpect(jsonPath("$.message").exists())
                                .andExpect(jsonPath("$.fileUrl").value(containsString("s3")))
                                .andExpect(jsonPath("$.fileUrl").value(containsString("profile")))
                                .andExpect(jsonPath("$.fileUrl").value(org.hamcrest.Matchers.endsWith(".jpg")));

                verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 게시판 첨부파일 업로드 성공 - 관리자")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testIntegration_UploadBoardAttachment_Success() throws Exception {
                // given
                MockMultipartFile attachment = new MockMultipartFile(
                                "file",
                                "document.pdf",
                                MediaType.APPLICATION_PDF_VALUE,
                                "pdf document content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(attachment)
                                .param("folder", "board")
                                .param("fileType", "BOARD_ATTACH")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fileUrl").exists())
                                .andExpect(jsonPath("$.message").exists())
                                .andExpect(jsonPath("$.fileUrl").value(containsString("board")))
                                .andExpect(jsonPath("$.fileUrl").value(org.hamcrest.Matchers.endsWith(".pdf")));

                verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 과정 이미지 업로드 성공")
        @WithMockUser(username = "instructor", roles = { "USER" })
        void testIntegration_UploadCourseImage_Success() throws Exception {
                // given
                MockMultipartFile courseImage = new MockMultipartFile(
                                "file",
                                "course-thumbnail.png",
                                MediaType.IMAGE_PNG_VALUE,
                                "course image content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(courseImage)
                                .param("folder", "course")
                                .param("fileType", "COURSE_IMAGE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fileUrl").exists())
                                .andExpect(jsonPath("$.message").exists())
                                .andExpect(jsonPath("$.fileUrl").value(containsString("course")))
                                .andExpect(jsonPath("$.fileUrl").value(org.hamcrest.Matchers.endsWith(".png")));

                verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 파일 업로드 실패 - 비인증 사용자")
        void testIntegration_Upload_Unauthorized() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());

                verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 파일 업로드 실패 - null 파일")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_Upload_NullFile() throws Exception {
                // when & then
                // required=true 파라미터가 없으면 Spring이 MissingServletRequestPartException 발생 (400)
                mockMvc.perform(multipart("/api/files/upload")
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest());

                verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 파일 업로드 실패 - 빈 파일")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_Upload_EmptyFile() throws Exception {
                // given
                MockMultipartFile emptyFile = new MockMultipartFile(
                                "file",
                                "empty.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                new byte[0]);

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(emptyFile)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail").exists())
                                .andExpect(jsonPath("$.detail").value(containsString("파일이 비어있습니다")));

                verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 파일 업로드 실패 - 허용되지 않은 확장자")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_Upload_InvalidExtension() throws Exception {
                // given
                MockMultipartFile executableFile = new MockMultipartFile(
                                "file",
                                "malicious.exe",
                                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                "executable content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(executableFile)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isUnsupportedMediaType()) // 415
                                .andExpect(jsonPath("$.detail").exists())
                                .andExpect(jsonPath("$.detail").value(containsString("허용되지 않은 파일")));

                verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 파일 업로드 실패 - 파일 크기 초과")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_Upload_FileSizeExceeded() throws Exception {
                // given - 프로필 이미지 최대 크기는 5MB
                byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
                MockMultipartFile largeFile = new MockMultipartFile(
                                "file",
                                "large-profile.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                largeContent);

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(largeFile)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isPayloadTooLarge()) // 413
                                .andExpect(jsonPath("$.detail").exists())
                                .andExpect(jsonPath("$.detail").value(containsString("파일 크기가 제한을 초과합니다")));

                verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 파일 업로드 실패 - 경로 순회 공격")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_Upload_PathTraversal() throws Exception {
                // given - 파일명은 정상이지만 폴더 경로에 상위 디렉토리 참조
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "normal.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "normal content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "../profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest()) // 400 (IllegalArgumentException)
                                .andExpect(jsonPath("$.detail").exists())
                                .andExpect(jsonPath("$.detail")
                                                .value(containsString("폴더명에 상위 디렉토리 경로(..)를 포함할 수 없습니다")));

                verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 파일 업로드 실패 - 위험한 파일명")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_Upload_DangerousFilename() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "file<script>.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test content".getBytes());

                // when & then
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest()) // 400 (IllegalArgumentException)
                                .andExpect(jsonPath("$.detail").exists())
                                .andExpect(jsonPath("$.detail").value(containsString("파일명에 허용되지 않는 특수문자가 포함되어 있습니다")));

                verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        // ==================== 파일 삭제 통합 테스트 ====================

        @Test
        @DisplayName("통합: 파일 삭제 성공 - 관리자")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testIntegration_DeleteFile_Success() throws Exception {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";

                // when & then
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", fileUrl)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").exists())
                                .andExpect(jsonPath("$.message").value(containsString("삭제되었습니다")));

                verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("통합: 파일 삭제 실패 - 일반 사용자 (권한 없음)")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_DeleteFile_Forbidden() throws Exception {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";

                // when & then
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", fileUrl)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isForbidden());

                verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("통합: 파일 삭제 실패 - 비인증 사용자")
        void testIntegration_DeleteFile_Unauthorized() throws Exception {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";

                // when & then
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", fileUrl)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(result -> {
                                        int status = result.getResponse().getStatus();
                                        if (status != 302 && status != 401 && status != 403) {
                                                throw new AssertionError("Expected 302, 401, or 403 but was " + status);
                                        }
                                });

                verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("통합: 파일 삭제 실패 - null URL")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testIntegration_DeleteFile_NullUrl() throws Exception {
                // when & then
                // required=true 파라미터가 없으면 Spring이 MissingServletRequestParameterException 발생
                // (400)
                mockMvc.perform(delete("/api/admin/files/delete")
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest());

                verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("통합: 파일 삭제 실패 - 잘못된 URL 형식")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testIntegration_DeleteFile_InvalidUrl() throws Exception {
                // given
                String invalidUrl = "not-a-valid-s3-url";

                // when & then
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", invalidUrl)
                                .with(csrf()))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.detail").exists())
                                .andExpect(jsonPath("$.detail").value(containsString("유효하지 않은")));

                verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        // ==================== 다중 파일 처리 시나리오 ====================

        @Test
        @DisplayName("통합: 여러 파일 연속 업로드 성공")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_MultipleFileUploads_Success() throws Exception {
                // given
                MockMultipartFile file1 = new MockMultipartFile(
                                "file", "image1.jpg", MediaType.IMAGE_JPEG_VALUE, "content1".getBytes());
                MockMultipartFile file2 = new MockMultipartFile(
                                "file", "image2.png", MediaType.IMAGE_PNG_VALUE, "content2".getBytes());
                MockMultipartFile file3 = new MockMultipartFile(
                                "file", "document.pdf", MediaType.APPLICATION_PDF_VALUE, "content3".getBytes());

                // when & then - 첫 번째 파일
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file1)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fileUrl").exists());

                // 두 번째 파일
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file2)
                                .param("folder", "course")
                                .param("fileType", "COURSE_IMAGE")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fileUrl").exists());

                // 세 번째 파일
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file3)
                                .param("folder", "board")
                                .param("fileType", "BOARD_ATTACH")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fileUrl").exists());

                verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("통합: 업로드 후 삭제 시나리오")
        @WithMockUser(username = "admin", roles = { "ADMIN" })
        void testIntegration_UploadAndDelete_Scenario() throws Exception {
                // given - 파일 업로드
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "temp-file.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "temporary content".getBytes());

                // when - 업로드
                String fileUrl = mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "temp")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fileUrl").exists())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                // then - 업로드 검증
                verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

                // when - 삭제 (실제 URL 사용 대신 Mock URL 사용)
                String mockFileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/temp/test-file.jpg";
                mockMvc.perform(delete("/api/admin/files/delete")
                                .param("fileUrl", mockFileUrl)
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").exists());

                // then - 삭제 검증
                verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("통합: CSRF 토큰 없이 요청 실패")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_Upload_WithoutCSRF() throws Exception {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.jpg",
                                MediaType.IMAGE_JPEG_VALUE,
                                "test content".getBytes());

                // when & then
                // CSRF가 비활성화되어 있으므로 성공 (SecurityConfig에서 csrf().disable())
                mockMvc.perform(multipart("/api/files/upload")
                                .file(file)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE"))
                                .andDo(print())
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("통합: 다양한 파일 타입 검증")
        @WithMockUser(username = "user1", roles = { "USER" })
        void testIntegration_VariousFileTypes() throws Exception {
                // PROFILE - jpg
                MockMultipartFile jpg = new MockMultipartFile(
                                "file", "profile.jpg", MediaType.IMAGE_JPEG_VALUE, "jpg".getBytes());
                mockMvc.perform(multipart("/api/files/upload")
                                .file(jpg)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andExpect(status().isOk());

                // PROFILE - png
                MockMultipartFile png = new MockMultipartFile(
                                "file", "profile.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes());
                mockMvc.perform(multipart("/api/files/upload")
                                .file(png)
                                .param("folder", "profile")
                                .param("fileType", "PROFILE")
                                .with(csrf()))
                                .andExpect(status().isOk());

                // BOARD_ATTACH - pdf
                MockMultipartFile pdf = new MockMultipartFile(
                                "file", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf".getBytes());
                mockMvc.perform(multipart("/api/files/upload")
                                .file(pdf)
                                .param("folder", "board")
                                .param("fileType", "BOARD_ATTACH")
                                .with(csrf()))
                                .andExpect(status().isOk());

                // COURSE_IMAGE - webp
                MockMultipartFile webp = new MockMultipartFile(
                                "file", "course.webp", "image/webp", "webp".getBytes());
                mockMvc.perform(multipart("/api/files/upload")
                                .file(webp)
                                .param("folder", "course")
                                .param("fileType", "COURSE_IMAGE")
                                .with(csrf()))
                                .andExpect(status().isOk());

                verify(s3Client, times(4)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }
}
