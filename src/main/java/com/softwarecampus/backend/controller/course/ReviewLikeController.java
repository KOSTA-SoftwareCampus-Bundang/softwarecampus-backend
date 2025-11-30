package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.dto.course.ReviewLikeRequest;
import com.softwarecampus.backend.dto.course.ReviewLikeResponse;
import com.softwarecampus.backend.security.CustomUserDetails;
import com.softwarecampus.backend.service.course.ReviewLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}/reviews")
@RequiredArgsConstructor
public class ReviewLikeController {

        private final ReviewLikeService reviewLikeService;

        /**
         * 좋아요/싫어요 토글 (인증 필요)
         * POST /api/courses/{courseId}/reviews/{reviewId}/likes
         */
        @PostMapping("/{reviewId}/likes")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ReviewLikeResponse> toggleLike(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId,
                        @RequestBody ReviewLikeRequest request,
                        @AuthenticationPrincipal CustomUserDetails userDetails) {
                // reviewId가 courseId에 속하는지 검증
                reviewLikeService.validateReviewBelongsToCourse(courseId, reviewId);

                Long accountId = userDetails.getId();
                var result = reviewLikeService.toggleLike(reviewId, accountId, request.getType());

                return ResponseEntity.ok(
                                new ReviewLikeResponse(
                                                result.isActive() ? result.getType().name() : "NONE",
                                                reviewLikeService.getLikeCount(reviewId),
                                                reviewLikeService.getDislikeCount(reviewId)));
        }

        /**
         * 좋아요/싫어요 개수 조회
         * GET /api/courses/{courseId}/reviews/{reviewId}/likes
         */
        @GetMapping("/{reviewId}/likes")
        public ResponseEntity<ReviewLikeResponse> getCounts(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId) {
                // reviewId가 courseId에 속하는지 검증
                reviewLikeService.validateReviewBelongsToCourse(courseId, reviewId);

                long likeCount = reviewLikeService.getLikeCount(reviewId);
                long dislikeCount = reviewLikeService.getDislikeCount(reviewId);

                return ResponseEntity.ok(
                                new ReviewLikeResponse("NONE", likeCount, dislikeCount));
        }
}
