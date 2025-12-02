package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import com.softwarecampus.backend.dto.course.ReviewRejectRequest;
import com.softwarecampus.backend.service.admin.CourseReviewAdminService;
import com.softwarecampus.backend.service.course.CourseReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 리뷰 관리 API
 * 수정일: 2025-12-02 - CourseReviewAdminService로 승인/거부 로직 분리
 */
@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class CourseReviewAdminController {

    private final CourseReviewService reviewService;
    private final CourseReviewAdminService reviewAdminService;

    /**
     * 리뷰 승인 요청 목록 조회
     */
    @GetMapping("/approval-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CourseReviewResponse>> getApprovalRequests(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAdminReviews(status, keyword, pageable));
    }

    /**
     * 리뷰 승인
     * - CourseReviewAdminService 사용 (Soft Delete 준수, 상태 검증 포함)
     */
    @PostMapping("/{reviewId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseReviewResponse> approveReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewAdminService.approveReview(reviewId));
    }

    /**
     * 리뷰 거부
     * - CourseReviewAdminService 사용 (Soft Delete 준수, 감사 로깅 포함)
     */
    @PostMapping("/{reviewId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseReviewResponse> rejectReview(
            @PathVariable Long reviewId,
            @RequestBody @Valid ReviewRejectRequest request) {
        return ResponseEntity.ok(reviewAdminService.rejectReview(reviewId, request.getReason()));
    }
}
