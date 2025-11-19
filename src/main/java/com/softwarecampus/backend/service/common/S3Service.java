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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
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

    // 경로 순회 공격 패턴 (../ 또는 \..\만 차단)
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*(\\.\\.[/\\\\]|[/\\\\]\\.\\.).*");

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
        // 1. URL 검증 (null, 형식, 길이, 경로 순회 공격 방지)
        validateFileUrl(fileUrl);
        
        try {
            String key = extractKeyFromUrl(fileUrl);

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
            throw new IllegalArgumentException("파일 URL이 비어있습니다.");
        }
        
        String expectedPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (!fileUrl.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("유효하지 않은 S3 URL 형식입니다: " + fileUrl);
        }
        
        // URL에서 키 부분 추출
        String encodedKey = fileUrl.substring(expectedPrefix.length());
        
        // URL 디코딩 (공백, 특수문자 복원)
        try {
            return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("Failed to decode S3 key from URL: {}", fileUrl, e);
            throw new IllegalArgumentException("S3 URL 디코딩에 실패했습니다: " + fileUrl, e);
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
     * S3 키에서 안전한 URL을 생성합니다.
     * 키의 각 경로 세그먼트를 URL 인코딩하여 공백과 특수문자가 포함된 파일명도 올바르게 처리합니다.
     */
    private String getFileUrl(String key) {
        String encodedKey = encodeS3Key(key);
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                region,
                encodedKey);
    }

    /**
     * S3 키의 각 경로 세그먼트를 RFC 3986 표준에 따라 퍼센트 인코딩합니다.
     * 폴더 구분자('/')는 보존하면서 파일명의 공백과 특수문자를 올바르게 인코딩합니다.
     * URLEncoder는 form encoding(application/x-www-form-urlencoded)을 사용하여
     * 공백을 '+'로 변환하므로 S3 경로에는 부적합합니다.
     */
    private String encodeS3Key(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        
        try {
            // split("/", -1)을 사용해 trailing empty segments 보존
            String[] segments = key.split("/", -1);
            StringBuilder encodedKey = new StringBuilder();
            
            // 선행 슬래시 감지 (첫 세그먼트가 empty면 key가 "/"로 시작)
            boolean startsWithSlash = key.startsWith("/");
            // 후행 슬래시 감지 (마지막 세그먼트가 empty면 key가 "/"로 끝남)
            boolean endsWithSlash = key.endsWith("/");
            
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                
                // empty segment 처리: 선행/후행/연속 슬래시를 의미
                if (segment.isEmpty()) {
                    // 첫 번째 empty segment (선행 슬래시)
                    if (i == 0 && startsWithSlash) {
                        encodedKey.append("/");
                    }
                    // 중간 empty segment (연속 슬래시 "//")
                    else if (i > 0 && i < segments.length - 1) {
                        encodedKey.append("/");
                    }
                    // 마지막 empty segment (후행 슬래시)는 루프 후 처리
                } else {
                    // non-empty segment만 URI로 인코딩
                    if (i > 0 && (i > 1 || !startsWithSlash)) {
                        encodedKey.append("/");
                    }
                    // RFC 3986 표준 퍼센트 인코딩 사용
                    // URI 생성자는 각 세그먼트를 올바르게 인코딩 (공백 -> %20)
                    String encoded = new URI(null, null, segment, null).getRawPath();
                    encodedKey.append(encoded);
                }
            }
            
            // 후행 슬래시 추가
            if (endsWithSlash && !key.equals("/")) {
                encodedKey.append("/");
            }
            
            return encodedKey.toString();
        } catch (URISyntaxException e) {
            log.error("Failed to encode S3 key: {}", key, e);
            // 인코딩 실패 시 원본 키 반환 (S3Service에서 추가 검증 필요)
            throw new IllegalArgumentException("S3 키 인코딩에 실패했습니다: " + key, e);
        }
    }

    /**
     * 폴더 경로의 보안 검증 (경로 순회 공격 방지 + 허용 목록 검증)
     * URL 인코딩, 이중 인코딩, 유니코드 인코딩된 경로 순회 벡터 탐지
     * 
     * @param folder S3 내 폴더 경로
     * @throws IllegalArgumentException 경로 순회 공격 시도 또는 허용되지 않은 폴더인 경우
     */
    private void validateFolderSecurity(String folder) {
        // 1. 허용 목록 검증
        if (!ALLOWED_FOLDERS.contains(folder)) {
            log.warn("Invalid folder name attempted: {}", folder);
            throw new IllegalArgumentException("허용되지 않은 폴더입니다: " + folder);
        }
        
        // 2. 경로 순회 공격 방지 (URL 인코딩/이중 인코딩 포함)
        if (!isPathSafe(folder, "uploads")) {
            log.warn("Path traversal attempt detected in folder: {}", folder);
            throw new IllegalArgumentException("유효하지 않은 폴더 경로입니다.");
        }
    }

    /**
     * URL 디코딩 및 정규화를 통한 경로 순회 공격 방지
     * URL 인코딩, 이중 인코딩, 유니코드 인코딩된 경로 순회 벡터 탐지
     * 
     * @param input 검증할 입력 문자열 (폴더 경로)
     * @param baseFolder 기본 폴더 (예: "uploads")
     * @return 정규화된 경로가 안전하면 true, 경로 순회 시도가 감지되면 false
     */
    private boolean isPathSafe(String input, String baseFolder) {
        if (input == null || input.isBlank()) {
            return true; // 빈 문자열은 다른 검증에서 처리
        }

        try {
            // 1. URL 디코딩 (한 번만 수행하여 무한 디코딩 방지)
            String decoded = URLDecoder.decode(input, StandardCharsets.UTF_8);
            
            // 2. 이중 인코딩 방지를 위한 추가 디코딩 검사
            // 디코딩 후에도 % 문자가 남아있으면 이중 인코딩 시도로 간주
            if (decoded.contains("%")) {
                log.warn("Potential double-encoding detected in path: {}", input);
                return false;
            }
            
            // 3. 기본 디렉토리 설정 (S3 버킷 루트)
            Path basePath = Paths.get("/" + baseFolder).normalize().toAbsolutePath();
            
            // 4. 입력 경로를 기본 디렉토리에 해결 및 정규화
            Path resolvedPath = basePath.resolve(decoded).normalize().toAbsolutePath();
            
            // 5. 정규화된 경로가 기본 디렉토리로 시작하는지 확인
            if (!resolvedPath.startsWith(basePath)) {
                log.warn("Path traversal attempt detected - input: {}, decoded: {}, resolved: {}, base: {}",
                        input, decoded, resolvedPath, basePath);
                return false;
            }
            
            return true;
            
        } catch (IllegalArgumentException e) {
            // URL 디코딩 실패 또는 잘못된 경로
            log.warn("Invalid path format: {}", input, e);
            return false;
        } catch (Exception e) {
            // 기타 예외 발생 시 안전하게 거부
            log.error("Unexpected error during path validation: {}", input, e);
            return false;
        }
    }

    /**
     * 파일 URL의 보안 검증 (null, 형식, 길이, 경로 순회 공격 방지)
     * 
     * @param fileUrl 검증할 파일 URL
     * @throws IllegalArgumentException URL이 유효하지 않거나 보안 위협이 감지된 경우
     */
    private void validateFileUrl(String fileUrl) {
        // 1. null 및 빈 문자열 검증
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("파일 URL이 비어있습니다.");
        }

        // 2. URL 형식 검증 (기본 패턴 체크)
        if (!fileUrl.startsWith("https://") && !fileUrl.startsWith("http://")) {
            log.warn("File delete failed: invalid URL format: {}", fileUrl);
            throw new IllegalArgumentException("유효하지 않은 URL 형식입니다.");
        }

        // 3. S3 URL 패턴 검증 (amazonaws.com 포함 여부)
        if (!fileUrl.contains(".s3.") || !fileUrl.contains(".amazonaws.com/")) {
            log.warn("File delete failed: not a valid S3 URL: {}", fileUrl);
            throw new IllegalArgumentException("유효하지 않은 S3 URL입니다.");
        }

        // 4. 경로 순회 공격 방지
        if (PATH_TRAVERSAL_PATTERN.matcher(fileUrl).matches()) {
            log.warn("File delete failed: path traversal attempt detected in URL: {}", fileUrl);
            throw new IllegalArgumentException("유효하지 않은 URL입니다.");
        }

        // 5. URL 길이 검증 (비정상적으로 긴 URL 차단)
        if (fileUrl.length() > 2048) {
            log.warn("File delete failed: URL too long: {} characters", fileUrl.length());
            throw new IllegalArgumentException("URL이 너무 깁니다.");
        }
    }

    private void validateFile(MultipartFile file, FileType.FileTypeConfig config) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 파일 크기 검증
        if (!config.isFileSizeValid(file.getSize())) {
            log.warn("File size {} exceeds maximum allowed size {}", 
                    file.getSize(), config.getMaxFileSize());
            throw new IllegalArgumentException(
                    String.format("파일 크기가 제한을 초과합니다. 최대 %dMB까지 업로드 가능합니다.",
                            config.getMaxFileSizeMB()));
        }

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !config.isContentTypeAllowed(contentType)) {
            log.warn("Invalid content type attempted: {}", contentType);
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다: " + contentType);
        }

        // 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!config.isExtensionAllowed(extension)) {
            log.warn("Invalid file extension attempted: {}", extension);
            throw new IllegalArgumentException(
                    String.format("허용되지 않은 파일 확장자입니다: %s (허용: %s)", 
                            extension, config.getAllowedExtensions()));
        }
    }
}

