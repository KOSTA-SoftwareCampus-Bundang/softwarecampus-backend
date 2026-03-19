/**
 * 객체 스토리지(S3 호환) 파일 업로드/다운로드/삭제 서비스
 * Cloudflare R2를 대상으로 파일 관리 기능을 제공합니다.
 *
 * 파일 URL 정책:
 * - DB에는 절대 URL 대신 key(경로)를 저장합니다. (예: "profile/550e8400-e29b-41d4-a716-446655440000.jpg")
 * - 파일 접근은 GET /api/files/public?key= 백엔드 프록시를 통해 이루어집니다.
 * - 민감한 파일(기관 등록 서류 등)은 generatePresignedUrl()로 임시 링크를 발급합니다.
 */
package com.softwarecampus.backend.service.common;

import com.softwarecampus.backend.exception.S3UploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileType fileType;

    @Value("${storage.object.bucket}")
    private String bucketName;

    // 허용된 폴더 목록 (S3Folder enum 기반)
    private static final Set<String> ALLOWED_FOLDERS = Arrays.stream(S3Folder.values())
            .map(S3Folder::getPath)
            .collect(Collectors.toSet());

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, FileType fileType) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.fileType = fileType;
    }

    /**
     * 애플리케이션 시작 시 필수 설정값 검증
     */
    @PostConstruct
    private void validateConfiguration() {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException(
                "스토리지 버킷 이름이 설정되지 않았습니다. 'storage.object.bucket' 프로퍼티를 확인하세요.");
        }
        log.info("S3Service initialized with bucket: {}", bucketName);
    }

    /**
     * 파일을 업로드하고 S3 key를 반환합니다.
     * DB에는 절대 URL이 아닌 key(예: "profile/uuid.jpg")를 저장해야 합니다.
     *
     * @param file         업로드할 파일
     * @param folder       S3 폴더 경로 (S3Folder enum 기반)
     * @param fileTypeEnum 파일 타입 (FileType 설정 조회용)
     * @return S3 key (예: "profile/550e8400-e29b-41d4-a716-446655440000.jpg")
     */
    public String uploadFile(MultipartFile file, String folder, FileType.FileTypeEnum fileTypeEnum) {
        // 1. 폴더 경로 순회 공격 방지 및 허용 목록 검증
        validateFolderSecurity(folder);

        // 2. FileType 설정 조회 및 파일 검증
        FileType.FileTypeConfig config = fileType.getConfig(fileTypeEnum);
        validateFile(file, config);

        try {
            String originalFilename = file.getOriginalFilename();
            String fileName = generateFileName(originalFilename);
            String key = folder.isEmpty() ? fileName : folder + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            // 스트리밍 방식으로 파일 업로드 (메모리 효율적)
            // file.getBytes()는 전체 파일을 메모리에 로드하여 대용량 파일 시 OutOfMemoryError 발생 가능
            try (var inputStream = file.getInputStream()) {
                s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize())
                );
            }

            log.info("File uploaded successfully: {}", key);
            return key;
        } catch (IOException e) {
            log.error("Failed to read file for upload: {}", file.getOriginalFilename(), e);
            throw new S3UploadException("파일 읽기에 실패했습니다.", e);
        } catch (S3Exception e) {
            log.error("Upload failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw new S3UploadException("S3 업로드에 실패했습니다.", e);
        }
    }

    /**
     * S3 key로 파일을 삭제합니다.
     *
     * @param key S3 키 (예: "profile/uuid.jpg")
     */
    public void deleteFile(String key) {
        validateS3Key(key);

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", key);
        } catch (S3Exception e) {
            log.error("Delete failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw new S3UploadException("S3 파일 삭제에 실패했습니다.", e);
        }
    }

    /**
     * S3 key로 파일 바이트를 다운로드합니다.
     * GET /api/files/public 백엔드 프록시에서 사용합니다.
     *
     * @param key S3 키 (예: "profile/uuid.jpg")
     * @return 파일 바이트 배열
     */
    public byte[] downloadFile(String key) {
        validateS3Key(key);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
        } catch (S3Exception e) {
            log.error("Download failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw new S3UploadException("S3 파일 다운로드에 실패했습니다.", e);
        }
    }

    /**
     * S3 파일의 Content-Type을 조회합니다.
     * GET /api/files/public 엔드포인트에서 올바른 Content-Type 헤더를 반환하기 위해 사용합니다.
     *
     * @param key S3 키
     * @return Content-Type 문자열 (조회 실패 시 "application/octet-stream" 반환)
     */
    public String getContentType(String key) {
        try {
            validateS3Key(key);
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            return s3Client.headObject(headObjectRequest).contentType();
        } catch (Exception e) {
            log.warn("Failed to get content type for key: {}, defaulting to octet-stream", key);
            return "application/octet-stream";
        }
    }

    /**
     * S3 파일의 메타데이터(크기)를 조회합니다.
     *
     * @param key S3 키 (예: "profile/uuid.jpg")
     * @return 파일 크기 (bytes), 조회 실패 시 0 반환
     */
    public long getFileSize(String key) {
        try {
            validateS3Key(key);
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            long fileSize = response.contentLength();
            log.debug("File size for {}: {} bytes", key, fileSize);
            return fileSize;
        } catch (S3Exception e) {
            log.warn("Failed to get file size: {}", e.awsErrorDetails().errorMessage());
            return 0L;
        } catch (Exception e) {
            log.warn("Failed to get file size: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * S3 파일에 대한 Presigned URL을 생성합니다.
     * 지정된 시간 동안만 유효한 임시 다운로드 링크입니다.
     * 민감한 파일(기관 등록 서류 등) 접근 시 사용합니다.
     *
     * @param s3Key    S3 객체 키 (예: "academy/123/uuid-file.pdf")
     * @param duration URL 유효 기간
     * @return Presigned URL
     */
    public String generatePresignedUrl(String s3Key, Duration duration) {
        validateS3Key(s3Key);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.info("Presigned URL generated for key: {}, duration: {}", s3Key, duration);
            return presignedUrl;
        } catch (S3Exception e) {
            log.error("Failed to generate presigned URL for key: {}", s3Key, e);
            throw new S3UploadException("Presigned URL 생성에 실패했습니다.", e);
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    /**
     * 폴더 경로의 보안 검증
     * S3Folder enum 기반 화이트리스트 검증으로 경로 순회 공격 원천 차단
     * 서브폴더도 허용 (예: academy/100, course/images/123)
     */
    private void validateFolderSecurity(String folder) {
        if (folder.contains("..") || folder.contains("//") || folder.contains("\\")) {
            log.warn("Path traversal attempt detected in folder: {}", folder);
            throw new IllegalArgumentException("허용되지 않은 폴더입니다: " + folder);
        }

        String rootFolder = folder.contains("/") ? folder.split("/")[0] : folder;
        if (!ALLOWED_FOLDERS.contains(folder) && !ALLOWED_FOLDERS.contains(rootFolder)) {
            log.warn("Invalid folder name attempted: {}", folder);
            throw new IllegalArgumentException("허용되지 않은 폴더입니다: " + folder);
        }
    }

    /**
     * S3 key의 경로 순회 공격 검증
     * URL 인코딩 우회를 방지하기 위해 key를 직접 검증합니다.
     *
     * @param key 검증할 S3 키
     */
    private void validateS3Key(String key) {
        if (key == null || key.isBlank()) {
            throw new S3UploadException("S3 키가 비어있습니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }

        if (key.contains("..") || key.contains("//") || key.contains("\\")) {
            log.warn("Path traversal attempt detected in S3 key: {}", key);
            throw new S3UploadException("잘못된 S3 키 형식입니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }

        if (key.startsWith("/") || key.startsWith("\\")) {
            log.warn("S3 key starts with path separator: {}", key);
            throw new S3UploadException("잘못된 S3 키 형식입니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }
    }

    private void validateFile(MultipartFile file, FileType.FileTypeConfig config) {
        if (file == null || file.isEmpty()) {
            throw new S3UploadException("파일이 비어있습니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }

        if (!config.isFileSizeValid(file.getSize())) {
            log.warn("File size {} exceeds maximum allowed size {}", file.getSize(), config.getMaxFileSize());
            throw new S3UploadException(
                    String.format("파일 크기가 제한을 초과합니다. 최대 %dMB까지 업로드 가능합니다.",
                            config.getMaxFileSizeMB()),
                    S3UploadException.FailureReason.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !config.isContentTypeAllowed(contentType)) {
            log.warn("Invalid content type attempted: {}", contentType);
            throw new S3UploadException(
                    "허용되지 않은 파일 형식입니다: " + contentType,
                    S3UploadException.FailureReason.INVALID_FILE_TYPE);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new S3UploadException(
                    "파일 확장자가 없습니다.",
                    S3UploadException.FailureReason.VALIDATION_ERROR);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!config.isExtensionAllowed(extension)) {
            log.warn("Invalid file extension attempted: {}", extension);
            throw new S3UploadException(
                    String.format("허용되지 않은 파일 확장자입니다: %s (허용: %s)",
                            extension, config.getAllowedExtensions()),
                    S3UploadException.FailureReason.INVALID_FILE_TYPE);
        }
    }
}
