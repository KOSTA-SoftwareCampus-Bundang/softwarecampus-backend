package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseReviewRequest;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import com.softwarecampus.backend.dto.course.ReviewFileResponse;
import com.softwarecampus.backend.service.course.CourseReviewFileService;
import com.softwarecampus.backend.service.course.CourseReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{type}/course/{courseId}/reviews")
public class CourseReviewController {

    private final CourseReviewService reviewService;
    private final CourseReviewFileService reviewFileService;

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

    @PostMapping("/{reviewId}/file")
    public ResponseEntity<ReviewFileResponse> uploadReviewFile(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") Long userId
    ) {
        return ResponseEntity.ok(reviewFileService.uploadReviewFile(type, courseId, reviewId, userId, file));
    }

    @DeleteMapping("/{reviewId}/file/{fileId}")
    public ResponseEntity<Void> deleteReviewFile(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @PathVariable Long fileId,
            @RequestAttribute("userId") Long userId
    ) {
        reviewFileService.deleteReviewFile(type, courseId, reviewId, fileId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reviewId}/file/{fileId}/restore")
    public ResponseEntity<Void> restoreReviewFile(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @PathVariable Long fileId,
            @RequestAttribute("userId") Long adminId
    ) {
        reviewFileService.restoreReviewFile(type, courseId, reviewId, fileId, adminId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{reviewId}/file/{fileId}/hard")
    public ResponseEntity<Void> hardDeleteReviewFile(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @PathVariable Long fileId,
            @RequestAttribute("userId") Long adminId
    ) {
        reviewFileService.hardDeleteReviewFile(type, courseId, reviewId, fileId, adminId);
        return ResponseEntity.noContent().build();
    }
}
