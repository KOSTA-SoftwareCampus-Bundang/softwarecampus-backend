/**
 * 파일 타입별 허용 확장자 및 MIME 타입 정의
 * application.properties에서 설정을 주입받아 관리합니다.
 * 데이터베이스 테이블 단위로 구분합니다.
 *
 * 사용 예시:
 * <pre>
 * // 프로필 이미지 업로드 시
 * s3Service.uploadFile(file, S3Folder.PROFILE, FileType.FileTypeEnum.PROFILE);
 * 
 * // 게시판 첨부파일 업로드 시
 * s3Service.uploadFile(file, S3Folder.BOARD, FileType.FileTypeEnum.BOARD_ATTACH);
 * </pre>
 */
package com.softwarecampus.backend.service.common;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileType {

    // 프로필 이미지 설정 (account 테이블)
    @Value("${file.upload.profile.extensions}")
    private String profileExtensionsStr;
    @Value("${file.upload.profile.content-types}")
    private String profileContentTypesStr;
    @Value("${file.upload.profile.max-size}")
    private long profileMaxSize;

    // 게시판 첨부파일 설정 (board_attach 테이블)
    @Value("${file.upload.board-attach.extensions}")
    private String boardAttachExtensionsStr;
    @Value("${file.upload.board-attach.content-types}")
    private String boardAttachContentTypesStr;
    @Value("${file.upload.board-attach.max-size}")
    private long boardAttachMaxSize;
    @Value("${file.upload.board-attach.max-count}")
    private int boardAttachMaxCount;

    // 과정 이미지 설정 (course_image 테이블)
    @Value("${file.upload.course-image.extensions}")
    private String courseImageExtensionsStr;
    @Value("${file.upload.course-image.content-types}")
    private String courseImageContentTypesStr;
    @Value("${file.upload.course-image.max-size}")
    private long courseImageMaxSize;

    // 기관 첨부파일 설정 (academy_files 테이블)
    @Value("${file.upload.academy-file.extensions}")
    private String academyFileExtensionsStr;
    @Value("${file.upload.academy-file.content-types}")
    private String academyFileContentTypesStr;
    @Value("${file.upload.academy-file.max-size}")
    private long academyFileMaxSize;

    // 후기 첨부파일 설정 (course_review_file 테이블)
    @Value("${file.upload.review-file.extensions}")
    private String reviewFileExtensionsStr;
    @Value("${file.upload.review-file.content-types}")
    private String reviewFileContentTypesStr;
    @Value("${file.upload.review-file.max-size}")
    private long reviewFileMaxSize;

    // 파싱된 설정을 저장하는 Map
    private final Map<FileTypeEnum, FileTypeConfig> configs = new HashMap<>();

    @PostConstruct
    private void init() {
        // 각 파일 타입별 설정 파싱 및 검증
        Set<String> profileExts = parseSet(profileExtensionsStr);
        Set<String> profileTypes = parseSet(profileContentTypesStr);
        validateFileTypeConfig("PROFILE", profileExts, profileTypes, profileMaxSize);
        configs.put(FileTypeEnum.PROFILE, new FileTypeConfig(profileExts, profileTypes, profileMaxSize));

        Set<String> boardExts = parseSet(boardAttachExtensionsStr);
        Set<String> boardTypes = parseSet(boardAttachContentTypesStr);
        validateFileTypeConfig("BOARD_ATTACH", boardExts, boardTypes, boardAttachMaxSize);
        configs.put(FileTypeEnum.BOARD_ATTACH,
                new FileTypeConfig(boardExts, boardTypes, boardAttachMaxSize, boardAttachMaxCount));

        Set<String> courseExts = parseSet(courseImageExtensionsStr);
        Set<String> courseTypes = parseSet(courseImageContentTypesStr);
        validateFileTypeConfig("COURSE_IMAGE", courseExts, courseTypes, courseImageMaxSize);
        configs.put(FileTypeEnum.COURSE_IMAGE, new FileTypeConfig(courseExts, courseTypes, courseImageMaxSize));

        Set<String> academyExts = parseSet(academyFileExtensionsStr);
        Set<String> academyTypes = parseSet(academyFileContentTypesStr);
        validateFileTypeConfig("ACADEMY_FILE", academyExts, academyTypes, academyFileMaxSize);
        configs.put(FileTypeEnum.ACADEMY_FILE, new FileTypeConfig(academyExts, academyTypes, academyFileMaxSize));

        Set<String> reviewExts = parseSet(reviewFileExtensionsStr);
        Set<String> reviewTypes = parseSet(reviewFileContentTypesStr);
        validateFileTypeConfig("REVIEW_FILE", reviewExts, reviewTypes, reviewFileMaxSize);
        configs.put(FileTypeEnum.REVIEW_FILE, new FileTypeConfig(reviewExts, reviewTypes, reviewFileMaxSize));

        log.info("FileType configurations initialized: {}", configs.keySet());
    }

    private void validateFileTypeConfig(String typeName, Set<String> extensions, Set<String> contentTypes,
            long maxSize) {
        if (extensions == null || extensions.isEmpty()) {
            throw new IllegalStateException(
                    String.format("File type %s: allowedExtensions is null or empty. " +
                            "Please check 'file.upload.%s.extensions' in application.properties.",
                            typeName, typeName.toLowerCase().replace("_", "-")));
        }
        if (contentTypes == null || contentTypes.isEmpty()) {
            throw new IllegalStateException(
                    String.format("File type %s: allowedContentTypes is null or empty. " +
                            "Please check 'file.upload.%s.content-types' in application.properties.",
                            typeName, typeName.toLowerCase().replace("_", "-")));
        }
        if (maxSize <= 0) {
            throw new IllegalStateException(
                    String.format("File type %s: maxFileSize must be greater than 0. " +
                            "Please check 'file.upload.%s.max-size' in application.properties.",
                            typeName, typeName.toLowerCase().replace("_", "-")));
        }
    }

    private Set<String> parseSet(String str) {
        if (str == null || str.trim().isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public FileTypeConfig getConfig(FileTypeEnum type) {
        return configs.get(type);
    }

    /**
     * 파일 타입 열거형 (데이터베이스 테이블 단위)
     */
    public enum FileTypeEnum {
        /** 프로필 이미지 (account 테이블) */
        PROFILE,
        /** 게시판 첨부파일 (board_attach 테이블) */
        BOARD_ATTACH,
        /** 과정 이미지 (course_image 테이블) */
        COURSE_IMAGE,
        /** 기관 첨부파일 (academy_files 테이블) */
        ACADEMY_FILE,
        /** 후기 첨부파일 (course_review_file 테이블) - 수료증 */
        REVIEW_FILE
    }

    /**
     * 파일 타입 설정 클래스
     */
    @Getter
    public static class FileTypeConfig {
        private final Set<String> allowedExtensions;
        private final Set<String> allowedContentTypes;
        private final long maxFileSize;
        private final int maxCount;

        public FileTypeConfig(Set<String> allowedExtensions, Set<String> allowedContentTypes, long maxFileSize) {
            this(allowedExtensions, allowedContentTypes, maxFileSize, 0); // 0 = 무제한
        }

        public FileTypeConfig(Set<String> allowedExtensions, Set<String> allowedContentTypes, long maxFileSize,
                int maxCount) {
            // 방어적 복사 및 불변성 보장 (검증은 @PostConstruct에서 수행)
            this.allowedExtensions = Collections.unmodifiableSet(new HashSet<>(allowedExtensions));
            this.allowedContentTypes = Collections.unmodifiableSet(new HashSet<>(allowedContentTypes));
            this.maxFileSize = maxFileSize;
            this.maxCount = maxCount;
        }

        public boolean isExtensionAllowed(String extension) {
            if (extension == null || extension.trim().isEmpty()) {
                return false;
            }
            return allowedExtensions.contains(extension.trim().toLowerCase(Locale.ROOT));
        }

        public boolean isContentTypeAllowed(String contentType) {
            if (contentType == null || contentType.trim().isEmpty()) {
                return false;
            }
            return allowedContentTypes.contains(contentType.trim().toLowerCase(Locale.ROOT));
        }

        public boolean isFileSizeValid(long fileSize) {
            return fileSize > 0 && fileSize <= maxFileSize;
        }

        public boolean isFileCountValid(int fileCount) {
            return maxCount <= 0 || fileCount <= maxCount; // maxCount가 0이면 무제한
        }

        public long getMaxFileSizeMB() {
            return maxFileSize / (1024 * 1024);
        }
    }
}
