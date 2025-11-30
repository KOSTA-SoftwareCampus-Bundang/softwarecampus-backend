package com.softwarecampus.backend.service.common;

import com.softwarecampus.backend.exception.S3UploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * S3Service 단위 테스트
 * 
 * 테스트 대상:
 * - uploadFile: S3 파일 업로드
 * - deleteFile: S3 파일 삭제
 * - validateFile: 파일 검증
 * - validateS3Key: S3 키 보안 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3Service 단위 테스트")
class S3ServiceTest {

        @Mock
        private S3Client s3Client;

        @Mock
        private FileType fileType;

        @Mock
        private S3Presigner s3Presigner;

        private S3Service s3Service;

        private FileType.FileTypeConfig profileConfig;

        @BeforeEach
        void setUp() {
                s3Service = new S3Service(s3Client, s3Presigner, fileType);

                // @Value 필드 주입
                ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
                ReflectionTestUtils.setField(s3Service, "region", "ap-northeast-2");

                // @PostConstruct 메서드 호출
                ReflectionTestUtils.invokeMethod(s3Service, "validateConfiguration");

                // FileTypeConfig mock 설정
                profileConfig = new FileType.FileTypeConfig(
                                Set.of("jpg", "jpeg", "png", "gif", "webp"),
                                Set.of("image/jpeg", "image/png", "image/gif", "image/webp"),
                                5242880L // 5MB
                );

                // lenient 설정으로 불필요한 stubbing 경고 무시
                lenient().when(fileType.getConfig(FileType.FileTypeEnum.PROFILE)).thenReturn(profileConfig);
        }

        @Test
        @DisplayName("파일 업로드 성공")
        void testUploadFile_Success() throws IOException {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test-image.jpg",
                                "image/jpeg",
                                "test content".getBytes());

                PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
                when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                                .thenReturn(putObjectResponse);

                // when
                String fileUrl = s3Service.uploadFile(file, "profile", FileType.FileTypeEnum.PROFILE);

                // then
                assertThat(fileUrl).isNotNull();
                assertThat(fileUrl).startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/");
                assertThat(fileUrl).endsWith(".jpg");

                verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("파일 업로드 - 파일명에 공백 포함 (UUID로 대체)")
        void testUploadFile_WithSpaceInFilename() throws IOException {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test image.jpg",
                                "image/jpeg",
                                "test content".getBytes());

                PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
                when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                                .thenReturn(putObjectResponse);

                // when
                String fileUrl = s3Service.uploadFile(file, "profile", FileType.FileTypeEnum.PROFILE);

                // then
                assertThat(fileUrl).contains(".jpg"); // UUID로 파일명이 변경되지만 확장자는 유지됨
                assertThat(fileUrl).doesNotContain("test image"); // 원본 파일명은 포함되지 않음
                verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("파일 업로드 실패 - null 파일")
        void testUploadFile_NullFile() {
                // when & then
                assertThatThrownBy(() -> s3Service.uploadFile(null, "profile", FileType.FileTypeEnum.PROFILE))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("파일이 비어있습니다");
        }

        @Test
        @DisplayName("파일 업로드 실패 - 빈 파일")
        void testUploadFile_EmptyFile() {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.jpg",
                                "image/jpeg",
                                new byte[0]);

                // when & then
                assertThatThrownBy(() -> s3Service.uploadFile(file, "profile", FileType.FileTypeEnum.PROFILE))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("파일이 비어있습니다");
        }

        @Test
        @DisplayName("파일 업로드 실패 - 파일 크기 초과")
        void testUploadFile_FileSizeExceeded() {
                // given
                byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB (limit: 5MB)
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "large-image.jpg",
                                "image/jpeg",
                                largeContent);

                // when & then
                assertThatThrownBy(() -> s3Service.uploadFile(file, "profile", FileType.FileTypeEnum.PROFILE))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("파일 크기가 제한을 초과합니다");
        }

        @Test
        @DisplayName("파일 업로드 실패 - 허용되지 않은 Content-Type")
        void testUploadFile_InvalidContentType() {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.pdf",
                                "application/pdf",
                                "test content".getBytes());

                // when & then
                assertThatThrownBy(() -> s3Service.uploadFile(file, "profile", FileType.FileTypeEnum.PROFILE))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("허용되지 않은 파일 형식입니다");
        }

        @Test
        @DisplayName("파일 업로드 실패 - 허용되지 않은 확장자")
        void testUploadFile_InvalidExtension() {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.exe",
                                "image/jpeg", // Content-Type은 맞지만 확장자가 틀림
                                "test content".getBytes());

                // when & then
                assertThatThrownBy(() -> s3Service.uploadFile(file, "profile", FileType.FileTypeEnum.PROFILE))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("허용되지 않은 파일 확장자입니다");
        }

        @Test
        @DisplayName("파일 업로드 실패 - 확장자 없음")
        void testUploadFile_NoExtension() {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test",
                                "image/jpeg",
                                "test content".getBytes());

                // when & then
                assertThatThrownBy(() -> s3Service.uploadFile(file, "profile", FileType.FileTypeEnum.PROFILE))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("파일 확장자가 없습니다");
        }

        @Test
        @DisplayName("파일 업로드 실패 - 허용되지 않은 폴더")
        void testUploadFile_InvalidFolder() {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.jpg",
                                "image/jpeg",
                                "test content".getBytes());

                // when & then
                assertThatThrownBy(() -> s3Service.uploadFile(file, "invalid-folder", FileType.FileTypeEnum.PROFILE))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("허용되지 않은 폴더입니다");
        }

        @Test
        @DisplayName("파일 업로드 실패 - S3 업로드 오류")
        void testUploadFile_S3Exception() {
                // given
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.jpg",
                                "image/jpeg",
                                "test content".getBytes());

                S3Exception s3Exception = (S3Exception) S3Exception.builder()
                                .message("S3 error")
                                .awsErrorDetails(AwsErrorDetails.builder()
                                                .errorCode("InternalError")
                                                .errorMessage("S3 internal error")
                                                .build())
                                .build();

                when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                                .thenThrow(s3Exception);

                // when & then
                assertThatThrownBy(() -> s3Service.uploadFile(file, "profile", FileType.FileTypeEnum.PROFILE))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("S3 업로드에 실패했습니다");
        }

        @Test
        @DisplayName("파일 삭제 성공")
        void testDeleteFile_Success() {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";

                DeleteObjectResponse deleteObjectResponse = DeleteObjectResponse.builder().build();
                when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                                .thenReturn(deleteObjectResponse);

                // when
                assertThatCode(() -> s3Service.deleteFile(fileUrl))
                                .doesNotThrowAnyException();

                // then
                ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
                verify(s3Client, times(1)).deleteObject(captor.capture());

                DeleteObjectRequest request = captor.getValue();
                assertThat(request.bucket()).isEqualTo("test-bucket");
                assertThat(request.key()).isEqualTo("profile/test-file.jpg");
        }

        @Test
        @DisplayName("파일 삭제 성공 - URL 인코딩된 키")
        void testDeleteFile_EncodedKey() {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test%20file.jpg";

                DeleteObjectResponse deleteObjectResponse = DeleteObjectResponse.builder().build();
                when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                                .thenReturn(deleteObjectResponse);

                // when
                assertThatCode(() -> s3Service.deleteFile(fileUrl))
                                .doesNotThrowAnyException();

                // then
                ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
                verify(s3Client, times(1)).deleteObject(captor.capture());

                DeleteObjectRequest request = captor.getValue();
                assertThat(request.key()).isEqualTo("profile/test file.jpg"); // 디코딩됨
        }

        @Test
        @DisplayName("파일 삭제 실패 - null URL")
        void testDeleteFile_NullUrl() {
                // when & then
                assertThatThrownBy(() -> s3Service.deleteFile(null))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("파일 URL이 비어있습니다");
        }

        @Test
        @DisplayName("파일 삭제 실패 - 빈 URL")
        void testDeleteFile_EmptyUrl() {
                // when & then
                assertThatThrownBy(() -> s3Service.deleteFile(""))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("파일 URL이 비어있습니다");
        }

        @Test
        @DisplayName("파일 삭제 실패 - 잘못된 URL 형식")
        void testDeleteFile_InvalidUrl() {
                // when & then
                assertThatThrownBy(() -> s3Service.deleteFile("invalid-url"))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("유효하지 않은 URL 형식입니다");
        }

        @Test
        @DisplayName("파일 삭제 실패 - S3 삭제 오류")
        void testDeleteFile_S3Exception() {
                // given
                String fileUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";

                S3Exception s3Exception = (S3Exception) S3Exception.builder()
                                .message("S3 delete error")
                                .awsErrorDetails(AwsErrorDetails.builder()
                                                .errorCode("InternalError")
                                                .errorMessage("S3 internal error")
                                                .build())
                                .build();

                lenient().when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                                .thenThrow(s3Exception);

                // when & then
                assertThatThrownBy(() -> s3Service.deleteFile(fileUrl))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("S3 파일 삭제에 실패했습니다");
        }

        @Test
        @DisplayName("S3 키 검증 - 정상 키")
        void testValidateS3Key_Valid() throws Exception {
                // given
                String validKey = "board/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg";

                // when & then - 예외가 발생하지 않아야 함
                assertThatNoException().isThrownBy(
                                () -> ReflectionTestUtils.invokeMethod(s3Service, "validateS3Key", validKey));
        }

        @Test
        @DisplayName("S3 키 검증 - 경로 순회 공격 방지 (..)")
        void testValidateS3Key_PathTraversal_DoubleDot() throws Exception {
                // given
                String attackKey = "board/../profile/malicious.jpg";

                // when & then
                assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(s3Service, "validateS3Key", attackKey))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("잘못된 S3 키 형식입니다");
        }

        @Test
        @DisplayName("S3 키 검증 - 경로 순회 공격 방지 (//)")
        void testValidateS3Key_PathTraversal_DoubleSlash() throws Exception {
                // given
                String attackKey = "board//malicious.jpg";

                // when & then
                assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(s3Service, "validateS3Key", attackKey))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("잘못된 S3 키 형식입니다");
        }

        @Test
        @DisplayName("S3 키 검증 - 경로 순회 공격 방지 (\\\\)")
        void testValidateS3Key_PathTraversal_Backslash() throws Exception {
                // given
                String attackKey = "board\\\\malicious.jpg";

                // when & then
                assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(s3Service, "validateS3Key", attackKey))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("잘못된 S3 키 형식입니다");
        }

        @Test
        @DisplayName("S3 키 검증 - 선행 슬래시 방지")
        void testValidateS3Key_LeadingSlash() throws Exception {
                // given
                String attackKey = "/board/malicious.jpg";

                // when & then
                assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(s3Service, "validateS3Key", attackKey))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("잘못된 S3 키 형식입니다");
        }

        @Test
        @DisplayName("S3 키 검증 - null 입력")
        void testValidateS3Key_Null() throws Exception {
                // when & then
                assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(s3Service, "validateS3Key", (String) null))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("S3 키가 비어있습니다");
        }

        @Test
        @DisplayName("S3 키 검증 - 빈 문자열")
        void testValidateS3Key_Empty() throws Exception {
                // when & then
                assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(s3Service, "validateS3Key", ""))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("S3 키가 비어있습니다");
        }

        @Test
        @DisplayName("S3 키 검증 - 공백 문자열")
        void testValidateS3Key_Whitespace() throws Exception {
                // when & then
                assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(s3Service, "validateS3Key", "   "))
                                .isInstanceOf(S3UploadException.class)
                                .hasMessageContaining("S3 키가 비어있습니다");
        }

        @Test
        @DisplayName("URL에서 키 추출 - 정상")
        void testExtractKeyFromUrl() throws Exception {
                // given
                String url = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test-file.jpg";

                // when
                String key = (String) ReflectionTestUtils.invokeMethod(s3Service, "extractKeyFromUrl", url);

                // then
                assertThat(key).isEqualTo("profile/test-file.jpg");
        }

        @Test
        @DisplayName("URL에서 키 추출 - URL 인코딩된 키")
        void testExtractKeyFromUrl_Encoded() throws Exception {
                // given
                String url = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/profile/test%20file.jpg";

                // when
                String key = (String) ReflectionTestUtils.invokeMethod(s3Service, "extractKeyFromUrl", url);

                // then
                assertThat(key).isEqualTo("profile/test file.jpg"); // 디코딩됨
        }
}
