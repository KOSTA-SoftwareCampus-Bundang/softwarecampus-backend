package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.dto.course.CourseReviewRequest;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseReviewService {

    Page<CourseReviewResponse> getReviews(Long courseId, Pageable pageable);

    CourseReviewResponse getReviewDetail(Long courseId, Long reviewId);

    CourseReviewResponse createReview(Long courseId, Long accountId, CourseReviewRequest request);

    CourseReviewResponse updateReview(Long courseId, Long reviewId, Long accountId, CourseReviewRequest request);

    void deleteReview(Long courseId, Long reviewId, Long accountId);

    void requestDeleteReview(Long courseId, Long reviewId, Long accountId);
}
