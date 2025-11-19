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
     * TODO: [별도 브랜치] SecurityConfig에 @EnableMethodSecurity 추가 및 .requestMatchers("/api/files/upload").authenticated() 설정
     *       현재는 SecurityConfig의 .anyRequest().permitAll()로 인해 @PreAuthorize가 작동하지 않음
     *       비인증 사용자도 파일 업로드가 가능한 상태이므로 보안 수정 필요
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
    @PreAuthorize("isAuthenticated()")  // TODO: SecurityConfig 수정 후 활성화됨
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

            log.info("File upload request by {} - folder: {}, fileType: {}",
                    username, folder, fileType);

            // 2. S3 업로드 실행 (Service에서 모든 검증 수행)
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
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/files/delete")
    public ResponseEntity<Map<String, String>> deleteFile(
            @RequestParam(value = "fileUrl", required = false) String fileUrl) {

        try {
            // 1. 인증 정보 확인 및 관리자 권한 검증
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // 1-1. 인증 객체 null 체크
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("File delete failed: User is not authenticated");
                return createErrorResponse(
                        HttpStatus.UNAUTHORIZED,
                        "인증이 필요합니다."
                );
            }
            
            // 1-2. ADMIN 권한 확인 (getAuthorities() null 체크 포함)
            boolean isAdmin = authentication.getAuthorities() != null && 
                    authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> 
                        grantedAuthority != null && 
                        "ROLE_ADMIN".equals(grantedAuthority.getAuthority())
                    );
            
            if (!isAdmin) {
                String userName = authentication.getName() != null ? authentication.getName() : "authenticated_user";
                log.warn("File delete failed: User {} does not have ADMIN role", userName);
                return createErrorResponse(
                        HttpStatus.FORBIDDEN,
                        "관리자 권한이 필요합니다."
                );
            }
            
            String username = authentication.getName() != null ? authentication.getName() : "authenticated_admin";
            log.info("ADMIN file delete request - user: {}, fileUrl: {}", username, fileUrl);

            // 2. S3 파일 삭제 실행 (Service에서 모든 검증 수행)
            s3Service.deleteFile(fileUrl);

            log.warn("File PERMANENTLY deleted by admin {}: {}", username, fileUrl);

            Map<String, String> response = new HashMap<>();
            response.put("message", "파일이 성공적으로 삭제되었습니다.");

            return ResponseEntity.ok(response);

        } catch (S3UploadException e) {
            log.error("S3 delete failed for URL: {}", fileUrl, e);
            // S3UploadException의 FailureReason을 확인하여 적절한 HTTP 상태 코드 반환
            if (e.getReason() == S3UploadException.FailureReason.RESOURCE_NOT_FOUND) {
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

