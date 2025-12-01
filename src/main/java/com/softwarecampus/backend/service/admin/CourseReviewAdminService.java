package com.softwarecampus.backend.service.admin;

import com.softwarecampus.backend.dto.course.CourseReviewResponse;

/**
 * 관리자 후기 관리 서비스
 * 작성일: 2025-12-01
 */
public interface CourseReviewAdminService {

    /**
     * 후기 승인
     * 
     * @param reviewId 후기 ID
     * @return 승인된 후기 정보
     */
    CourseReviewResponse approveReview(Long reviewId);

    /**
     * 후기 거부
     * 
     * @param reviewId 후기 ID
     * @param reason   거부 사유
     * @return 거부된 후기 정보
     */
    CourseReviewResponse rejectReview(Long reviewId, String reason);
}
