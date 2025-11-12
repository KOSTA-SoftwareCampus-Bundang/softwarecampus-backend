package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.dto.course.ReviewCreateRequest;
import com.softwarecampus.backend.dto.course.ReviewResponse;
import com.softwarecampus.backend.dto.course.ReviewUpdateRequest;

import java.util.List;

public interface CourseReviewService {

    /** 특정 과정의 승인된 리뷰 리스트 조회 */
    List<ReviewResponse> getReviews(Long courseId);

    /** 리뷰 등록 */
    Long createReview(Long courseId, ReviewCreateRequest request, Long accountId);

    /** 리뷰 수정 (본인 작성한 리뷰만 가능) */
    void updateReview(Long reviewId, ReviewUpdateRequest request, Long accountId);

    /** 리뷰 삭제 (관리자) */
    void deleteReview(Long reviewId, Long accountId);

    /** 리뷰 삭제 요청 (관리자에게 승인 요청) */
    void requestDeleteReview(Long reviewId, Long accountId);

    /** 리뷰 상세보기 */
    ReviewResponse getReviewDetail(Long reviewId);

    /** 리뷰 추천/비추천 */
    void recommendReview(Long reviewId, Long accountId, boolean liked);
}
