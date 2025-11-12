package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.dto.course.ReviewCreateRequest;
import com.softwarecampus.backend.dto.course.ReviewResponse;
import com.softwarecampus.backend.dto.course.ReviewUpdateRequest;
import com.softwarecampus.backend.service.course.CourseReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/employee/course")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService courseReviewService;

    /**
     * ✅ 리뷰 목록 조회
     * GET /api/employee/course/{courseId}/reviews
     */
    @GetMapping("/{courseId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long courseId) {
        List<ReviewResponse> reviews = courseReviewService.getReviews(courseId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * ✅ 리뷰 등록
     * POST /api/employee/course/reviews/{courseId}
     */
    @PostMapping("/reviews/{courseId}")
    public ResponseEntity<Void> createReview(
            @PathVariable Long courseId,
            @RequestBody ReviewCreateRequest request,
            @RequestAttribute("accountId") Long accountId // 인증정보에서 가져온다고 가정
    ) {
        Long id = courseReviewService.createReview(courseId, request, accountId);
        return ResponseEntity.created(URI.create("/api/employee/course/reviews/" + id)).build();
    }

    /**
     * ✅ 리뷰 수정 (본인 작성한 것)
     * PUT /api/employee/course/reviews/{reviewId}
     */
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequest request,
            @RequestAttribute("accountId") Long accountId
    ) {
        courseReviewService.updateReview(reviewId, request, accountId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ✅ 리뷰 삭제 (본인 작성한 것)
     * DELETE /api/employee/course/reviews/{reviewId}
     */
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @RequestAttribute("accountId") Long accountId
    ) {
        courseReviewService.deleteReview(reviewId, accountId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ✅ 리뷰 삭제 요청 (본인 작성한 것)
     * POST /api/employee/course/reviews/{reviewId}/delete-request
     */
    @PostMapping("/reviews/{reviewId}/delete-request")
    public ResponseEntity<Void> requestDeleteReview(
            @PathVariable Long reviewId,
            @RequestAttribute("accountId") Long accountId
    ) {
        courseReviewService.requestDeleteReview(reviewId, accountId);
        return ResponseEntity.accepted().build();
    }

    /**
     * ✅ 리뷰 상세보기
     * GET /api/employee/course/reviews/{reviewId}
     */
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> getReviewDetail(@PathVariable Long reviewId) {
        ReviewResponse response = courseReviewService.getReviewDetail(reviewId);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ 리뷰 추천/비추천
     * POST /api/employee/course/reviews/{reviewId}/recommend
     */
    @PostMapping("/reviews/{reviewId}/recommend")
    public ResponseEntity<Void> recommendReview(
            @PathVariable Long reviewId,
            @RequestParam boolean liked,
            @RequestAttribute("accountId") Long accountId
    ) {
        courseReviewService.recommendReview(reviewId, accountId, liked);
        return ResponseEntity.ok().build();
    }
}
