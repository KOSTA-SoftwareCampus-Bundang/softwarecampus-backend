package com.softwarecampus.backend.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * FileType 단위 테스트
 * 
 * 테스트 대상:
 * - FileTypeConfig: 파일 타입 설정 검증
 * - isExtensionAllowed: 확장자 허용 검증
 * - isContentTypeAllowed: Content-Type 허용 검증
 * - isFileSizeValid: 파일 크기 검증
 */
@DisplayName("FileType 단위 테스트")
class FileTypeTest {

    private FileType fileType;

    @BeforeEach
    void setUp() {
        fileType = new FileType();
        
        // @Value 필드 주입 시뮬레이션
        ReflectionTestUtils.setField(fileType, "profileExtensionsStr", "jpg,jpeg,png,gif,webp");
        ReflectionTestUtils.setField(fileType, "profileContentTypesStr", "image/jpeg,image/png,image/gif,image/webp");
        ReflectionTestUtils.setField(fileType, "profileMaxSize", 5242880L); // 5MB
        
        ReflectionTestUtils.setField(fileType, "boardAttachExtensionsStr", "jpg,jpeg,png,pdf,doc,docx,zip");
        ReflectionTestUtils.setField(fileType, "boardAttachContentTypesStr", "image/jpeg,image/png,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/zip");
        ReflectionTestUtils.setField(fileType, "boardAttachMaxSize", 52428800L); // 50MB
        
        ReflectionTestUtils.setField(fileType, "courseImageExtensionsStr", "jpg,jpeg,png,webp");
        ReflectionTestUtils.setField(fileType, "courseImageContentTypesStr", "image/jpeg,image/png,image/webp");
        ReflectionTestUtils.setField(fileType, "courseImageMaxSize", 10485760L); // 10MB
        
        // @PostConstruct 메서드 수동 호출
        ReflectionTestUtils.invokeMethod(fileType, "init");
    }

    @Test
    @DisplayName("FileTypeConfig 생성 성공")
    void testFileTypeConfigCreation() {
        // given
        Set<String> extensions = Set.of("jpg", "png");
        Set<String> contentTypes = Set.of("image/jpeg", "image/png");
        long maxSize = 5242880L;

        // when
        FileType.FileTypeConfig config = new FileType.FileTypeConfig(extensions, contentTypes, maxSize);

        // then
        assertThat(config.getAllowedExtensions()).containsExactlyInAnyOrder("jpg", "png");
        assertThat(config.getAllowedContentTypes()).containsExactlyInAnyOrder("image/jpeg", "image/png");
        assertThat(config.getMaxFileSize()).isEqualTo(5242880L);
        assertThat(config.getMaxFileSizeMB()).isEqualTo(5L);
    }

    @Test
    @DisplayName("허용된 확장자 검증 - 성공")
    void testIsExtensionAllowed_Success() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThat(config.isExtensionAllowed("jpg")).isTrue();
        assertThat(config.isExtensionAllowed("JPG")).isTrue(); // 대소문자 무관
        assertThat(config.isExtensionAllowed("jpeg")).isTrue();
        assertThat(config.isExtensionAllowed("png")).isTrue();
        assertThat(config.isExtensionAllowed("  gif  ")).isTrue(); // trim 처리
    }

    @Test
    @DisplayName("허용되지 않은 확장자 검증 - 실패")
    void testIsExtensionAllowed_Fail() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThat(config.isExtensionAllowed("pdf")).isFalse();
        assertThat(config.isExtensionAllowed("doc")).isFalse();
        assertThat(config.isExtensionAllowed("exe")).isFalse();
    }

    @Test
    @DisplayName("확장자 null/빈 문자열 검증 - 실패")
    void testIsExtensionAllowed_NullOrEmpty() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThat(config.isExtensionAllowed(null)).isFalse();
        assertThat(config.isExtensionAllowed("")).isFalse();
        assertThat(config.isExtensionAllowed("   ")).isFalse();
    }

    @Test
    @DisplayName("허용된 Content-Type 검증 - 성공")
    void testIsContentTypeAllowed_Success() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThat(config.isContentTypeAllowed("image/jpeg")).isTrue();
        assertThat(config.isContentTypeAllowed("IMAGE/JPEG")).isTrue(); // 대소문자 무관
        assertThat(config.isContentTypeAllowed("Image/Png")).isTrue();
        assertThat(config.isContentTypeAllowed("  image/webp  ")).isTrue(); // trim 처리
    }

    @Test
    @DisplayName("허용되지 않은 Content-Type 검증 - 실패")
    void testIsContentTypeAllowed_Fail() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThat(config.isContentTypeAllowed("application/pdf")).isFalse();
        assertThat(config.isContentTypeAllowed("video/mp4")).isFalse();
    }

    @Test
    @DisplayName("Content-Type null/빈 문자열 검증 - 실패")
    void testIsContentTypeAllowed_NullOrEmpty() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThat(config.isContentTypeAllowed(null)).isFalse();
        assertThat(config.isContentTypeAllowed("")).isFalse();
        assertThat(config.isContentTypeAllowed("   ")).isFalse();
    }

    @Test
    @DisplayName("파일 크기 검증 - 성공")
    void testIsFileSizeValid_Success() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThat(config.isFileSizeValid(1024L)).isTrue(); // 1KB
        assertThat(config.isFileSizeValid(1048576L)).isTrue(); // 1MB
        assertThat(config.isFileSizeValid(5242880L)).isTrue(); // 5MB (최대)
    }

    @Test
    @DisplayName("파일 크기 검증 - 실패 (초과)")
    void testIsFileSizeValid_Exceed() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThat(config.isFileSizeValid(5242881L)).isFalse(); // 5MB + 1byte
        assertThat(config.isFileSizeValid(10485760L)).isFalse(); // 10MB
    }

    @Test
    @DisplayName("파일 크기 검증 - 실패 (0 또는 음수)")
    void testIsFileSizeValid_ZeroOrNegative() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThat(config.isFileSizeValid(0L)).isFalse();
        assertThat(config.isFileSizeValid(-1L)).isFalse();
    }

    @Test
    @DisplayName("파일 타입별 설정 검증 - PROFILE")
    void testGetConfig_Profile() {
        // when
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // then
        assertThat(config).isNotNull();
        assertThat(config.getAllowedExtensions()).contains("jpg", "jpeg", "png", "gif", "webp");
        assertThat(config.getMaxFileSize()).isEqualTo(5242880L);
    }

    @Test
    @DisplayName("파일 타입별 설정 검증 - BOARD_ATTACH")
    void testGetConfig_BoardAttach() {
        // when
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.BOARD_ATTACH);

        // then
        assertThat(config).isNotNull();
        assertThat(config.getAllowedExtensions()).contains("jpg", "pdf", "doc", "zip");
        assertThat(config.getMaxFileSize()).isEqualTo(52428800L);
    }

    @Test
    @DisplayName("파일 타입별 설정 검증 - COURSE_IMAGE")
    void testGetConfig_CourseImage() {
        // when
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.COURSE_IMAGE);

        // then
        assertThat(config).isNotNull();
        assertThat(config.getAllowedExtensions()).contains("jpg", "jpeg", "png", "webp");
        assertThat(config.getMaxFileSize()).isEqualTo(10485760L);
    }

    @Test
    @DisplayName("설정 불변성 검증")
    void testConfigImmutability() {
        // given
        FileType.FileTypeConfig config = fileType.getConfig(FileType.FileTypeEnum.PROFILE);

        // when & then
        assertThatThrownBy(() -> config.getAllowedExtensions().add("exe"))
                .isInstanceOf(UnsupportedOperationException.class);
        
        assertThatThrownBy(() -> config.getAllowedContentTypes().add("application/exe"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("parseSet - null 입력 시 빈 Set 반환")
    void testParseSet_NullInput() throws Exception {
        // when
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) ReflectionTestUtils.invokeMethod(fileType, "parseSet", (String) null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parseSet - 빈 문자열 입력 시 빈 Set 반환")
    void testParseSet_EmptyInput() throws Exception {
        // when
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) ReflectionTestUtils.invokeMethod(fileType, "parseSet", "");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parseSet - 공백만 있는 입력 시 빈 Set 반환")
    void testParseSet_BlankInput() throws Exception {
        // when
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) ReflectionTestUtils.invokeMethod(fileType, "parseSet", "   ");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("parseSet - 정상 입력 파싱 및 소문자 변환")
    void testParseSet_ValidInput() throws Exception {
        // when
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) ReflectionTestUtils.invokeMethod(fileType, "parseSet", "JPG,PNG,  GIF  ,WebP");

        // then
        assertThat(result).containsExactlyInAnyOrder("jpg", "png", "gif", "webp");
    }

    @Test
    @DisplayName("parseSet - 빈 토큰 필터링")
    void testParseSet_FilterEmptyTokens() throws Exception {
        // when
        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) ReflectionTestUtils.invokeMethod(fileType, "parseSet", "jpg,,png,  ,gif");

        // then
        assertThat(result).containsExactlyInAnyOrder("jpg", "png", "gif");
    }
}
