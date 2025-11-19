/**
 * 공용 파일 관리 컨트롤러
 * 모든 도메인에서 사용 가능한 파일 업로드/삭제 기능을 제공합니다.
 * S3Service를 통해 AWS S3 버킷과 연동합니다.
 */
package com.softwarecampus.backend.controller.common;

import com.softwarecampus.backend.dto.common.FileDeleteResponse;
import com.softwarecampus.backend.dto.common.FileUploadResponse;
import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "") String folder,
            @RequestParam(value = "fileType", required = true) FileType.FileTypeEnum fileType) {

        // 현재 사용자 정보 (로깅용)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";
        
        log.info("File upload request by {} - folder: {}, fileType: {}",
                username, folder, fileType);

        // S3 업로드 실행 (모든 검증 및 예외는 Service와 GlobalExceptionHandler에서 처리)
        String fileUrl = s3Service.uploadFile(file, folder, fileType);

        log.info("File uploaded successfully: {}", fileUrl);

        return ResponseEntity.ok(FileUploadResponse.success(fileUrl));
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
    public ResponseEntity<FileDeleteResponse> deleteFile(
            @RequestParam(value = "fileUrl", required = false) String fileUrl) {

        // 현재 사용자 정보 (로깅용)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "admin";
        
        log.info("ADMIN file delete request - user: {}, fileUrl: {}", username, fileUrl);

        // S3 파일 삭제 실행 (모든 검증 및 예외는 Service와 GlobalExceptionHandler에서 처리)
        s3Service.deleteFile(fileUrl);

        log.warn("File PERMANENTLY deleted by admin {}: {}", username, fileUrl);

        return ResponseEntity.ok(FileDeleteResponse.success());
    }
}

