/**
 * 공용 파일 관리 컨트롤러
 * 모든 도메인에서 사용 가능한 파일 업로드/서빙/삭제 기능을 제공합니다.
 * S3Service를 통해 Cloudflare R2 버킷과 연동합니다.
 *
 * 파일 접근 방식:
 * - 공개 파일: GET /api/files/public?key= (인증 불필요, 백엔드 프록시)
 * - 민감 파일: AcademyFileService.getFileUrl() Presigned URL (인증 필요)
 */
package com.softwarecampus.backend.controller.common;

import com.softwarecampus.backend.dto.common.FileDeleteResponse;
import com.softwarecampus.backend.dto.common.FileUploadResponse;
import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final S3Service s3Service;

    /**
     * 파일 업로드
     * 인증된 사용자만 파일을 업로드할 수 있습니다.
     *
     * @param fileType 파일 타입 (PROFILE, BOARD_ATTACH, COURSE_IMAGE 등)
     * @return 업로드된 파일의 key (예: "profile/uuid.jpg")
     *
     *         사용 예시:
     *         - 게시판 첨부파일: POST /api/files/upload?folder=board&fileType=BOARD_ATTACH
     *         - 프로필 이미지: POST /api/files/upload?folder=profile&fileType=PROFILE
     *         - 과정 이미지: POST /api/files/upload?folder=course&fileType=COURSE_IMAGE
     */
    @PostMapping("/files/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam(value = "file", required = true) MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "") String folder,
            @RequestParam(value = "fileType", required = true) FileType.FileTypeEnum fileType) {

        // 파일 검증
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다. 파일을 선택해주세요.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }

        // Path Traversal 검증 (폴더명만)
        if (folder.contains("..")) {
            throw new IllegalArgumentException("폴더명에 상위 디렉토리 경로(..)를 포함할 수 없습니다.");
        }

        // 위험한 문자 검증 (파일명)
        if (originalFilename.matches(".*[<>:\"/\\\\|?*].*")) {
            throw new IllegalArgumentException("파일명에 허용되지 않는 특수문자가 포함되어 있습니다.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";

        log.info("File upload request by {} - folder: {}, fileType: {}", username, folder, fileType);

        String key = s3Service.uploadFile(file, folder, fileType);
        log.info("File uploaded successfully: {}", key);

        return ResponseEntity.ok(FileUploadResponse.success(key));
    }

    /**
     * 공개 파일 서빙 (인증 불필요)
     * R2 버킷은 비공개이므로 백엔드가 프록시 역할을 합니다.
     * SecurityConfig에서 GET /api/files/public 을 permitAll 처리합니다.
     *
     * @param key S3 키 (예: "profile/uuid.jpg")
     * @return 파일 바이트와 적절한 Content-Type 헤더 (30일 캐시)
     */
    @GetMapping("/files/public")
    public ResponseEntity<byte[]> getPublicFile(@RequestParam("key") String key) {
        byte[] bytes = s3Service.downloadFile(key);
        String contentType = s3Service.getContentType(key);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL,
                        CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().getHeaderValue())
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    /**
     * 파일 삭제 (관리자 전용)
     *
     * ⚠️ 주의: 이 엔드포인트는 관리자 대시보드 또는 긴급 상황에서만 사용됩니다.
     * 일반적인 파일 삭제 플로우:
     * 1. 사용자가 게시글/댓글 삭제 시 DB에서 soft-delete (is_deleted = true)
     * 2. 배치 작업으로 주기적으로 soft-deleted 파일들을 스토리지에서 실제 삭제
     * 3. 이를 통해 실수로 인한 데이터 손실 방지 및 복구 가능성 확보
     *
     * @param key 삭제할 파일의 S3 key (예: "profile/uuid.jpg")
     * @return 삭제 결과 메시지
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/files/delete")
    public ResponseEntity<FileDeleteResponse> deleteFile(
            @RequestParam(value = "key", required = true) String key) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "admin";

        log.info("ADMIN file delete request - user: {}, key: {}", username, key);

        s3Service.deleteFile(key);

        log.warn("File PERMANENTLY deleted by admin {}: {}", username, key);

        return ResponseEntity.ok(FileDeleteResponse.success());
    }
}
