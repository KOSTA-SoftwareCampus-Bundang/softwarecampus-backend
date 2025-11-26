package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseReviewRequest;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import com.softwarecampus.backend.service.course.CourseReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{type}/course/{courseId}/reviews")
public class CourseReviewController {

    private final CourseReviewService reviewService;

    @GetMapping("/{reviewId}")
    public ResponseEntity<CourseReviewResponse> getReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(reviewService.getReview(type, courseId, reviewId));
    }

    @PostMapping
    public ResponseEntity<CourseReviewResponse> createReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @RequestParam Long accountId,
            @RequestBody CourseReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.createReview(type, courseId, accountId, request));
    }

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
}
