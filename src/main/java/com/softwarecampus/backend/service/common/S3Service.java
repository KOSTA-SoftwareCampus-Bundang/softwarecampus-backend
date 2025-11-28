/**
 * AWS S3 파일 업로드/다운로드/삭제 서비스
 * S3 버킷에 대한 파일 관리 기능을 제공합니다.
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

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class S3Service {

    private final S3Client s3Client;
    private final FileType fileType;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    // 허용된 폴더 목록 (S3Folder enum 기반)
    private static final Set<String> ALLOWED_FOLDERS = Arrays.stream(S3Folder.values())
            .map(S3Folder::getPath)
            .collect(Collectors.toSet());

    public S3Service(S3Client s3Client, FileType fileType) {
        this.s3Client = s3Client;
        this.fileType = fileType;
    }

    /**
     * 애플리케이션 시작 시 필수 설정값들을 검증합니다.
     * bucketName과 region이 제대로 주입되었는지 확인하고,
     * 누락된 경우 명확한 오류 메시지와 함께 애플리케이션을 빠르게 실패시킵니다.
     */
    @PostConstruct
    private void validateConfiguration() {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException(
                "AWS S3 bucket name is not configured. Please set 'aws.s3.bucket-name' property.");
        }
        
        if (!StringUtils.hasText(region)) {
            throw new IllegalStateException(
                "AWS S3 region is not configured. Please set 'aws.s3.region' property.");
        }
        
        log.info("S3Service initialized with bucket: {} in region: {}", bucketName, region);
    }

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
            // InputStream을 사용하여 스트리밍 방식으로 업로드
            try (var inputStream = file.getInputStream()) {
                s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize())
                );
            }

            String fileUrl = getFileUrl(key);
            log.info("File uploaded successfully to S3: {}", key);
            return fileUrl;
        } catch (IOException e) {
            log.error("Failed to read file for S3 upload: {}", file.getOriginalFilename(), e);
            throw new S3UploadException("파일 읽기에 실패했습니다.", e);
        } catch (S3Exception e) {
            log.error("S3 upload failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw new S3UploadException("S3 업로드에 실패했습니다.", e);
        }
    }

    public void deleteFile(String fileUrl) {
        // 1. URL 기본 검증 (null, 형식, 길이)
        validateFileUrl(fileUrl);
        
        try {
            // 2. Key 추출 및 디코딩
            String key = extractKeyFromUrl(fileUrl);
            
            // 3. 디코딩된 key에 대한 경로 순회 검증 (URL 인코딩 우회 방지)
            validateS3Key(key);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from S3: {}", key);
        } catch (S3Exception e) {
            log.error("S3 delete failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw new S3UploadException("S3 파일 삭제에 실패했습니다.", e);
        }
    }

    /**
     * S3 URL에서 키를 추출합니다.
     * URL에 인코딩된 키를 디코딩하여 실제 S3 객체 이름과 일치시킵니다.
     * 공백이나 특수문자가 포함된 파일명의 경우 URL에서 %20 등으로 인코딩되어 있으므로
     * 디코딩하지 않으면 S3 객체 조회/삭제 시 불일치가 발생합니다.
     */
    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new S3UploadException("파일 URL이 비어있습니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }
        
        String expectedPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (!fileUrl.startsWith(expectedPrefix)) {
            throw new S3UploadException("유효하지 않은 S3 URL 형식입니다: " + fileUrl, S3UploadException.FailureReason.VALIDATION_ERROR);
        }
        
        // URL에서 키 부분 추출
        String encodedKey = fileUrl.substring(expectedPrefix.length());
        
        // URL 디코딩 (공백, 특수문자 복원)
        try {
            return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("Failed to decode S3 key from URL: {}", fileUrl, e);
            throw new S3UploadException("S3 URL 디코딩에 실패했습니다: " + fileUrl, e, S3UploadException.FailureReason.VALIDATION_ERROR);
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // validateFile()에서 이미 FileType enum 기반으로 확장자 검증 완료
            // (jpg, png, pdf 등 허용된 확장자만 통과)
        }
        return UUID.randomUUID() + extension;
    }

    /**
     * S3 키에서 URL을 생성합니다.
     * UUID 기반 파일명과 enum 기반 폴더명은 URL-safe하므로 별도 인코딩 불필요
     */
    private String getFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                region,
                key);
    }

    /**
     * 폴더 경로의 보안 검증
     * S3Folder enum 기반 화이트리스트 검증으로 경로 순회 공격 원천 차단
     * 
     * @param folder S3 내 폴더 경로
     * @throws IllegalArgumentException 허용되지 않은 폴더인 경우
     */
    private void validateFolderSecurity(String folder) {
        // S3Folder enum 기반 화이트리스트 검증
        // enum에 정의된 값("board", "academy", "course", "profile", "temp", "")만 허용
        // 긍정 리스트(positive list) 방식으로 경로 순회 공격 원천 차단
        if (!ALLOWED_FOLDERS.contains(folder)) {
            log.warn("Invalid folder name attempted: {}", folder);
            throw new IllegalArgumentException("허용되지 않은 폴더입니다: " + folder);
        }
    }

    /**
     * 파일 URL의 기본 검증 (null, 형식, 길이)
     * 경로 순회 검증은 디코딩 후 validateS3Key()에서 수행
     * 
     * @param fileUrl 검증할 파일 URL
     * @throws IllegalArgumentException URL이 유효하지 않은 경우
     */
    private void validateFileUrl(String fileUrl) {
        // 1. null 및 빈 문자열 검증
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new S3UploadException("파일 URL이 비어있습니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }

        // 2. URL 형식 검증 (기본 패턴 체크)
        if (!fileUrl.startsWith("https://") && !fileUrl.startsWith("http://")) {
            log.warn("File delete failed: invalid URL format: {}", fileUrl);
            throw new S3UploadException("유효하지 않은 URL 형식입니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }

        // 3. S3 URL 패턴 검증 (amazonaws.com 포함 여부)
        if (!fileUrl.contains(".s3.") || !fileUrl.contains(".amazonaws.com/")) {
            log.warn("File delete failed: not a valid S3 URL: {}", fileUrl);
            throw new S3UploadException("유효하지 않은 S3 URL입니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }

        // 4. URL 길이 검증 (비정상적으로 긴 URL 차단)
        if (fileUrl.length() > 2048) {
            log.warn("File delete failed: URL too long: {} characters", fileUrl.length());
            throw new S3UploadException("URL이 너무 깁니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }
    }

    /**
     * 디코딩된 S3 key의 경로 순회 공격 검증
     * URL 인코딩 우회를 방지하기 위해 디코딩 후 검증 필수
     * 
     * 예시:
     * - URL: "https://bucket.s3.region.amazonaws.com/board/%2E%2E/admin/file.pdf"
     * - 디코딩 후 key: "board/../admin/file.pdf" ← 여기서 차단
     * 
     * @param key 디코딩된 S3 키
     * @throws IllegalArgumentException 경로 순회 패턴이 감지된 경우
     */
    private void validateS3Key(String key) {
        if (key == null || key.isBlank()) {
            throw new S3UploadException("S3 키가 비어있습니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }
        
        // 경로 순회 패턴 검증 (**중요: URL 디코딩 후 검증**)
        if (key.contains("..") || key.contains("//") || key.contains("\\")) {
            log.warn("Path traversal attempt detected in decoded S3 key: {}", key);
            throw new S3UploadException("잘못된 S3 키 형식입니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }
        
        // 추가 보안: 경로 구분자로 시작하면 안됨
        if (key.startsWith("/") || key.startsWith("\\")) {
            log.warn("S3 key starts with path separator: {}", key);
            throw new S3UploadException("잘못된 S3 키 형식입니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }
    }

    private void validateFile(MultipartFile file, FileType.FileTypeConfig config) {
        if (file == null || file.isEmpty()) {
            throw new S3UploadException("파일이 비어있습니다.", S3UploadException.FailureReason.VALIDATION_ERROR);
        }

        // 파일 크기 검증
        if (!config.isFileSizeValid(file.getSize())) {
            log.warn("File size {} exceeds maximum allowed size {}", 
                    file.getSize(), config.getMaxFileSize());
            throw new S3UploadException(
                    String.format("파일 크기가 제한을 초과합니다. 최대 %dMB까지 업로드 가능합니다.",
                            config.getMaxFileSizeMB()),
                    S3UploadException.FailureReason.FILE_TOO_LARGE);
        }

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !config.isContentTypeAllowed(contentType)) {
            log.warn("Invalid content type attempted: {}", contentType);
            throw new S3UploadException(
                    "허용되지 않은 파일 형식입니다: " + contentType,
                    S3UploadException.FailureReason.INVALID_FILE_TYPE);
        }

        // 확장자 검증
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

    public byte[] downloadFile(String fileUrl) {
        // URL 검증
        validateFileUrl(fileUrl);

        // key 디코딩 및 보안 검증
        String key = extractKeyFromUrl(fileUrl);
        validateS3Key(key);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();

        } catch (S3Exception e) {
            log.error("Failed to download file from S3: {}", e.awsErrorDetails().errorMessage(), e);
            throw new S3UploadException("S3 파일 다운로드에 실패했습니다.", e);
        }
    }
}

