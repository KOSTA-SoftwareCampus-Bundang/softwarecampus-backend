/**
 * 공용 파일 관리 컨트롤러
 * 모든 도메인에서 사용 가능한 파일 업로드/삭제 기능을 제공합니다.
 * S3Service를 통해 AWS S3 버킷과 연동합니다.
 */
package com.softwarecampus.backend.controller.common;

import com.softwarecampus.backend.exception.S3UploadException;
import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final S3Service s3Service;

    // 경로 순회 공격 패턴 (../ 또는 \..\만 차단, 연속된 점은 허용)
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*(\\.\\.[/\\\\]|[/\\\\]\\.\\.).*");
    
    // 위험한 파일명 문자
    private static final Pattern DANGEROUS_CHARS_PATTERN = Pattern.compile(".*[<>:\"|?*\\x00-\\x1F].*");

    /**
     * 파일 업로드
     * 인증된 사용자만 파일을 업로드할 수 있습니다.
     * 
     * @param file 업로드할 파일
     * @param folder S3 내 폴더 경로 (선택, 기본값: 빈 문자열)
     *               예시: "board", "academy", "course", "profile" 등
     * @param fileType 파일 타입 (PROFILE, BOARD_ATTACH, COURSE_IMAGE)
     * @return 업로드된 파일의 URL
     *
     * 사용 예시:
     * - 게시판 첨부파일: POST /api/files/upload?folder=board&fileType=BOARD_ATTACH
     * - 프로필 이미지: POST /api/files/upload?folder=profile&fileType=PROFILE
     * - 과정 이미지: POST /api/files/upload?folder=course&fileType=COURSE_IMAGE
     */
    @PostMapping("/files/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "") String folder,
            @RequestParam(value = "fileType", required = true) FileType.FileTypeEnum fileType) {

        try {
            // 0. 현재 사용자 정보 (로깅용)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymous";
            
            // 1. 기본 null 및 empty 검증
            if (file == null || file.isEmpty()) {
                log.warn("File upload failed by {}: file is null or empty", username);
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "파일이 비어있습니다. 파일을 선택해주세요."
                );
            }

            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();

            log.info("File upload request by {} - filename: {}, size: {} bytes, contentType: {}, folder: {}, fileType: {}",
                    username, originalFilename, fileSize, contentType, folder, fileType);

            // 2. 파일명 검증
            if (originalFilename == null || originalFilename.isBlank()) {
                log.warn("File upload failed: filename is null or blank");
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "파일명이 유효하지 않습니다."
                );
            }

            // 3. 경로 순회 공격 방지
            if (PATH_TRAVERSAL_PATTERN.matcher(originalFilename).matches() ||
                PATH_TRAVERSAL_PATTERN.matcher(folder).matches()) {
                log.warn("File upload failed: path traversal attempt detected - filename: {}, folder: {}",
                        originalFilename, folder);
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "유효하지 않은 파일명 또는 폴더 경로입니다."
                );
            }

            // 4. 위험한 문자 검증
            if (DANGEROUS_CHARS_PATTERN.matcher(originalFilename).matches()) {
                log.warn("File upload failed: dangerous characters in filename: {}", originalFilename);
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "파일명에 허용되지 않는 문자가 포함되어 있습니다."
                );
            }

            // 5. S3 업로드 실행 (FileType에서 파일 크기, contentType, 확장자 검증)
            String fileUrl = s3Service.uploadFile(file, folder, fileType);

            log.info("File uploaded successfully: {}", fileUrl);

            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            response.put("message", "파일이 성공적으로 업로드되었습니다.");

            return ResponseEntity.ok(response);

        } catch (S3UploadException e) {
            log.error("S3 upload failed", e);
            return createErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 업로드 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            );
        } catch (IllegalArgumentException e) {
            log.error("Validation failed", e);
            return createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error during file upload", e);
            return createErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 업로드 중 예상치 못한 오류가 발생했습니다."
            );
        }
    }

    /**
     * 오류 응답 생성 헬퍼 메서드
     */
    private ResponseEntity<Map<String, String>> createErrorResponse(HttpStatus status, String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("status", String.valueOf(status.value()));
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * 파일 삭제 (관리자 전용)
     * 
     * ⚠️ 주의: 이 엔드포인트는 관리자 대시보드 또는 긴급 상황에서만 사용됩니다.
     * 일반적인 파일 삭제 플로우:
     * 1. 사용자가 게시글/댓글 삭제 시 DB에서 soft-delete (is_deleted = true)
     * 2. 배치 작업으로 주기적으로 soft-deleted 파일들을 S3에서 실제 삭제
     * 3. 이를 통해 실수로 인한 데이터 손실 방지 및 복구 가능성 확보
     * 
     * 이 API는 다음 경우에만 사용:
     * - 관리자가 악성 파일을 즉시 삭제해야 하는 경우
     * - 관리자 대시보드에서 직접 파일 관리가 필요한 경우
     * - 테스트/디버깅 용도
     * 
     * @param fileUrl 삭제할 파일의 S3 URL
     * @return 삭제 결과 메시지
     */
    @DeleteMapping("/admin/files/delete")
    public ResponseEntity<Map<String, String>> deleteFile(
            @RequestParam(value = "fileUrl", required = false) String fileUrl) {

        try {
            // 1. 인증 정보 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            log.info("ADMIN file delete request - user: {}, fileUrl: {}", username, fileUrl);

            // 2. fileUrl 기본 검증
            if (fileUrl == null || fileUrl.isBlank()) {
                log.warn("File delete failed: fileUrl is null or blank");
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "파일 URL이 비어있습니다."
                );
            }

            // 3. URL 형식 검증 (기본 패턴 체크)
            if (!fileUrl.startsWith("https://") && !fileUrl.startsWith("http://")) {
                log.warn("File delete failed: invalid URL format: {}", fileUrl);
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "유효하지 않은 URL 형식입니다."
                );
            }

            // 4. S3 URL 패턴 검증 (amazonaws.com 포함 여부)
            if (!fileUrl.contains(".s3.") || !fileUrl.contains(".amazonaws.com/")) {
                log.warn("File delete failed: not a valid S3 URL: {}", fileUrl);
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "유효하지 않은 S3 URL입니다."
                );
            }

            // 5. 경로 순회 공격 방지
            if (PATH_TRAVERSAL_PATTERN.matcher(fileUrl).matches()) {
                log.warn("File delete failed: path traversal attempt detected in URL: {}", fileUrl);
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "유효하지 않은 URL입니다."
                );
            }

            // 6. URL 길이 검증 (비정상적으로 긴 URL 차단)
            if (fileUrl.length() > 2048) {
                log.warn("File delete failed: URL too long: {} characters", fileUrl.length());
                return createErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "URL이 너무 깁니다."
                );
            }

            // 7. S3 파일 삭제 실행
            s3Service.deleteFile(fileUrl);

            log.warn("File PERMANENTLY deleted by admin {}: {}", username, fileUrl);

            Map<String, String> response = new HashMap<>();
            response.put("message", "파일이 성공적으로 삭제되었습니다.");

            return ResponseEntity.ok(response);

        } catch (S3UploadException e) {
            log.error("S3 delete failed for URL: {}", fileUrl, e);
            // S3에서 파일을 찾지 못한 경우는 404 반환
            if (e.getMessage() != null && e.getMessage().contains("NoSuchKey")) {
                return createErrorResponse(
                        HttpStatus.NOT_FOUND,
                        "파일을 찾을 수 없습니다."
                );
            }
            return createErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 삭제 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument for file deletion: {}", fileUrl, e);
            return createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error during file deletion: {}", fileUrl, e);
            return createErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 삭제 중 예상치 못한 오류가 발생했습니다."
            );
        }
    }
}

