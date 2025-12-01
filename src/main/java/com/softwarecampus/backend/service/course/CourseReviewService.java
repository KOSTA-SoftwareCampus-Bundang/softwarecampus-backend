package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.dto.course.CourseReviewRequest;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseReviewService {

    Page<CourseReviewResponse> getReviews(Long courseId, Pageable pageable, Long accountId);

    CourseReviewResponse getReviewDetail(Long courseId, Long reviewId, Long accountId);

    CourseReviewResponse createReview(Long courseId, Long accountId, CourseReviewRequest request);

    CourseReviewResponse updateReview(Long courseId, Long reviewId, Long accountId, CourseReviewRequest request);

    void deleteReview(Long courseId, Long reviewId, Long accountId);

    void requestDeleteReview(Long courseId, Long reviewId, Long accountId);

    /** 관리자 - 리뷰 목록 조회 (승인 상태별) */
    Page<CourseReviewResponse> getAdminReviews(com.softwarecampus.backend.domain.common.ApprovalStatus status,
            String keyword, Pageable pageable);

    /** 관리자 - 리뷰 승인 */
    CourseReviewResponse approveReview(Long reviewId);

    /** 관리자 - 리뷰 거부 */
    CourseReviewResponse rejectReview(Long reviewId, String reason);

    /** 기관 - 리뷰 목록 조회 (상태별) */
    Page<CourseReviewResponse> getInstitutionReviews(Long academyId,
            com.softwarecampus.backend.domain.common.ApprovalStatus status,
            String keyword, Pageable pageable);
}
