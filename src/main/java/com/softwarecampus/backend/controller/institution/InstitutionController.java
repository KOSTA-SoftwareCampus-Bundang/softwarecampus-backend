package com.softwarecampus.backend.controller.institution;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.dto.admin.InstitutionDashboardStatsResponse;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import com.softwarecampus.backend.security.CustomUserDetails;
import com.softwarecampus.backend.service.admin.AdminDashboardService;
import com.softwarecampus.backend.service.course.CourseReviewService;
import com.softwarecampus.backend.service.course.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 기관 사용자용 컨트롤러
 * 작성일: 2025-12-01
 * 수정일: 2025-12-02 - 레이어 규칙 준수를 위해 Repository 직접 호출 제거
 */
@RestController
@RequestMapping("/api/institution")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ACADEMY')")
public class InstitutionController {

    private final CourseService courseService;
    private final CourseReviewService courseReviewService;
    private final AdminDashboardService adminDashboardService;

    /**
     * 현재 로그인 사용자의 기관 ID 조회
     * 레이어 규칙 준수를 위해 서비스 계층을 통해 조회
     */
    private Long getAcademyId(CustomUserDetails userDetails) {
        return adminDashboardService.getAcademyIdByAccountId(userDetails.getId());
    }

    /**
     * 기관 대시보드 통계 조회
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<InstitutionDashboardStatsResponse> getDashboardStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long academyId = getAcademyId(userDetails);
        return ResponseEntity.ok(adminDashboardService.getInstitutionDashboardStats(academyId));
    }

    /**
     * 기관 과정 목록 조회
     */
    @GetMapping("/courses")
    public ResponseEntity<Page<CourseResponseDTO>> getInstitutionCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long academyId = getAcademyId(userDetails);
        return ResponseEntity.ok(courseService.getInstitutionCourses(academyId, status, keyword, pageable));
    }

    /**
     * 기관 리뷰 목록 조회
     */
    @GetMapping("/reviews")
    public ResponseEntity<Page<CourseReviewResponse>> getInstitutionReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long academyId = getAcademyId(userDetails);
        return ResponseEntity.ok(courseReviewService.getInstitutionReviews(academyId, status, keyword, pageable));
    }
}
