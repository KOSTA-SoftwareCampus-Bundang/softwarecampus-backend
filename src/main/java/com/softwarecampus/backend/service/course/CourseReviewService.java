package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.CourseReviewListResponse;
import com.softwarecampus.backend.dto.course.CourseReviewRequest;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;

import java.util.List;

public interface CourseReviewService {

    List<CourseReviewResponse> getReviews(CategoryType type, Long courseId);

    CourseReviewResponse getReviewDetail(CategoryType type, Long courseId, Long reviewId);

    CourseReviewResponse createReview(CategoryType type, Long courseId, Long accountId, CourseReviewRequest request);

    CourseReviewResponse updateReview(CategoryType type, Long courseId, Long reviewId, Long accountId, CourseReviewRequest request);

    void deleteReview(CategoryType type, Long courseId, Long reviewId, Long accountId);

    void requestDeleteReview(CategoryType type, Long courseId, Long reviewId, Long accountId);
}
