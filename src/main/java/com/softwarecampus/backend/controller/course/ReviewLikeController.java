package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.dto.course.ReviewLikeRequest;
import com.softwarecampus.backend.dto.course.ReviewLikeResponse;
import com.softwarecampus.backend.service.course.ReviewLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{type}/course/reviews")
@RequiredArgsConstructor
public class ReviewLikeController {

    private final ReviewLikeService reviewLikeService;

    /**
     * 좋아요/싫어요 토글
     */
    @PostMapping("/{reviewId}/recommend")
    public ResponseEntity<ReviewLikeResponse> toggleLike(
            @PathVariable Long reviewId,
            @RequestParam Long accountId,
            @RequestBody ReviewLikeRequest request
    ) {

        // 서비스는 request.getType() 을 받도록 FIX 필요
        var result = reviewLikeService.toggleLike(reviewId, accountId, request.getType());

        // JSON 응답에 type은 String으로 변환해야 함
        return ResponseEntity.ok(
                new ReviewLikeResponse(
                        result.getType().name(),
                        reviewLikeService.getLikeCount(reviewId),
                        reviewLikeService.getDislikeCount(reviewId)
                )
        );
    }


    /**
     * 좋아요/싫어요 개수 조회
     */
    @GetMapping("/counts")
    public ResponseEntity<?> getCounts(@PathVariable Long reviewId) {

        var like = reviewLikeService.getLikeCount(reviewId);
        var dislike = reviewLikeService.getDislikeCount(reviewId);

        return ResponseEntity.ok(
                new ReviewLikeResponse("NONE", like, dislike)
        );
    }

}
