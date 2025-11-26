package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseReviewRequest;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import com.softwarecampus.backend.service.course.CourseReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{type}/course/{courseId}/reviews")
public class CourseReviewController {

    private final CourseReviewService reviewService;

    /**
     * 1. 리뷰 리스트 조회
     * GET /api/{type}/course/{courseId}/reviews
     */
    @GetMapping
    public ResponseEntity<List<CourseReviewResponse>> getReviews(
            @PathVariable CategoryType type,
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(reviewService.getReviews(type, courseId));
    }

    /**
     * 2. 리뷰 상세 조회
     * GET /api/{type}/course/{courseId}/reviews/{reviewId}
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<CourseReviewResponse> getReviewDetail(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(reviewService.getReviewDetail(type, courseId, reviewId));
    }

    /**
     * 3. 리뷰 등록
     * POST /api/{type}/course/{courseId}/reviews?accountId=
     */
    @PostMapping
    public ResponseEntity<CourseReviewResponse> createReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @RequestParam Long accountId,
            @Valid @RequestBody CourseReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.createReview(type, courseId, accountId, request));
    }

    /**
     * 4. 리뷰 수정
     * PUT /api/{type}/course/{courseId}/reviews/{reviewId}?accountId=
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<CourseReviewResponse> updateReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @RequestParam Long accountId,
            @Valid @RequestBody CourseReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateReview(type, courseId, reviewId, accountId, request));
    }

    /**
     * 5. 리뷰 삭제
     * DELETE /api/{type}/course/{courseId}/reviews/{reviewId}?accountId=
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @RequestParam Long accountId
    ) {
        reviewService.deleteReview(type, courseId, reviewId, accountId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 6. 리뷰 삭제 요청
     * POST /api/{type}/course/{courseId}/reviews/{reviewId}/delete-request?accountId=
     */
    @PostMapping("/{reviewId}/delete-request")
    public ResponseEntity<Void> requestDeleteReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @RequestParam Long accountId
    ) {
        reviewService.requestDeleteReview(type, courseId, reviewId, accountId);
        return ResponseEntity.ok().build();
    }
}
