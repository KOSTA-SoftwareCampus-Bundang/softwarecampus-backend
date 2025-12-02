/**
 * 관리자 컨트롤러
 * 작성자: GitHub Copilot
 * 작성일: 2025-11-28
 * 수정일: 2025-12-02 - 레이어 규칙 준수를 위해 Repository 직접 호출 제거
 */
package com.softwarecampus.backend.controller.admin;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.dto.admin.DashboardStatsResponse;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.academy.AcademyRejectRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.service.admin.AccountAdminService;
import com.softwarecampus.backend.service.admin.AdminDashboardService;
import com.softwarecampus.backend.service.academy.AcademyService;
import com.softwarecampus.backend.service.academy.AcademyFileService;
import com.softwarecampus.backend.service.course.CourseService;
import com.softwarecampus.backend.service.course.CourseReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 관리자 전용 API
 * - 회원 승인/거절
 * - 기관 승인/거절 (AcademyController에 구현됨)
 * - 대시보드 통계 조회
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AccountAdminService accountAdminService;
    private final AdminDashboardService adminDashboardService;
    private final AcademyService academyService;
    private final AcademyFileService academyFileService;
    private final CourseService courseService;
    private final CourseReviewService reviewService;
    private final com.softwarecampus.backend.scheduler.FileCleanupScheduler fileCleanupScheduler;

    /**
     * 회원 승인
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     * 수정일: 2025-11-29 - 비즈니스 로직을 서비스 계층으로 이동
     */
    @PatchMapping("/accounts/{accountId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> approveAccount(@PathVariable Long accountId) {
        AccountResponse response = accountAdminService.approveAccount(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * 훈련기관 등록 승인
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @PatchMapping("/academies/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademyResponse> approveAcademy(@PathVariable Long id) {
        AcademyResponse academyResponse = academyService.approveAcademy(id);
        return ResponseEntity.ok(academyResponse);
    }

    /**
     * 훈련기관 등록 거절
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @PatchMapping("/academies/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademyResponse> rejectAcademy(
            @PathVariable Long id,
            @Valid @RequestBody AcademyRejectRequest request) {
        AcademyResponse academyResponse = academyService.rejectAcademy(id, request.getReason());
        return ResponseEntity.ok(academyResponse);
    }

    /**
     * 기관 첨부파일 다운로드
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     * 수정일: 2025-11-29 - academyId 검증 추가 (보안)
     */
    @GetMapping("/academies/{academyId}/files/{fileId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> downloadAcademyFile(
            @PathVariable Long academyId,
            @PathVariable Long fileId) {
        String presignedUrl = academyFileService.getFileUrl(academyId, fileId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .build();
    }

    /**
     * 파일 정리 스케줄러 수동 실행 (테스트용)
     * 작성일: 2025-11-30
     */
    @PostMapping("/files/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> triggerFileCleanup() {
        fileCleanupScheduler.cleanupDeletedFiles();
        return ResponseEntity.ok("파일 정리 스케줄러가 수동으로 실행되었습니다.");
    }

    /**
     * 대시보드 통계 조회
     * 작성일: 2025-12-01
     * 수정일: 2025-12-02 - 서비스 계층으로 로직 이동
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }

    /**
     * 기관 목록 조회 (관리자용)
     * 작성일: 2025-12-02
     */
    @GetMapping("/academies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AcademyResponse>> getAdminAcademies(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(academyService.getAdminAcademies(status, keyword, pageable));
    }

    /**
     * 과정 목록 조회 (관리자용)
     */
    @GetMapping("/courses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CourseResponseDTO>> getAdminCourses(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(courseService.getAdminCourses(status, keyword, pageable));
    }

    /**
     * 리뷰 목록 조회 (관리자용)
     */
    @GetMapping("/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CourseReviewResponse>> getAdminReviews(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAdminReviews(status, keyword, pageable));
    }
}
