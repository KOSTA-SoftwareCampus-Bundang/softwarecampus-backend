package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseReviewRequest;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import com.softwarecampus.backend.dto.course.ReviewFileResponse;
import com.softwarecampus.backend.dto.user.AccountCacheDto;
import com.softwarecampus.backend.security.CustomUserDetailsService;
import com.softwarecampus.backend.service.course.CourseReviewFileService;
import com.softwarecampus.backend.service.course.CourseReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{type}/course/{courseId}/reviews")
public class CourseReviewController {

    private final CourseReviewService reviewService;
    private final CourseReviewFileService reviewFileService;
    private final CustomUserDetailsService customUserDetailsService;

    // -------------------------------
    // 1. 리뷰 리스트 조회
    // GET /api/{type}/course/{courseId}/reviews
    // -------------------------------
    @GetMapping
    public ResponseEntity<List<CourseReviewResponse>> getReviews(
            @PathVariable CategoryType type,
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(reviewService.getReviews(type, courseId));
    }

    // -------------------------------
    // 2. 리뷰 상세 조회
    // GET /api/{type}/course/{courseId}/reviews/{reviewId}
    // -------------------------------
    @GetMapping("/{reviewId}")
    public ResponseEntity<CourseReviewResponse> getReviewDetail(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(reviewService.getReviewDetail(type, courseId, reviewId));
    }

    // -------------------------------
    // 3. 리뷰 등록
    // POST /api/{type}/course/{courseId}/reviews
    // -------------------------------
    @PostMapping
    public ResponseEntity<CourseReviewResponse> createReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());

        CourseReviewResponse response = reviewService.createReview(
                type,
                courseId,
                account.getId(),
                request
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------
    // 4. 리뷰 수정
    // PUT /api/{type}/course/{courseId}/reviews/{reviewId}
    // -------------------------------
    @PutMapping("/{reviewId}")
    public ResponseEntity<CourseReviewResponse> updateReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @Valid @RequestBody CourseReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());

        return ResponseEntity.ok(
                reviewService.updateReview(type, courseId, reviewId, account.getId(), request)
        );
    }

    // -------------------------------
    // 5. 리뷰 삭제
    // DELETE /api/{type}/course/{courseId}/reviews/{reviewId}
    // -------------------------------
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
        reviewService.deleteReview(type, courseId, reviewId, account.getId());
        return ResponseEntity.noContent().build();
    }

    // -------------------------------
    // 6. 리뷰 삭제 요청
    // POST /api/{type}/course/{courseId}/reviews/{reviewId}/delete-request
    // -------------------------------
    @PostMapping("/{reviewId}/delete-request")
    public ResponseEntity<Void> requestDeleteReview(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
        reviewService.requestDeleteReview(type, courseId, reviewId, account.getId());
        return ResponseEntity.ok().build();
    }

    // -------------------------------
    // 7. 리뷰 파일 업로드
    // POST /api/{type}/course/{courseId}/reviews/{reviewId}/file
    // -------------------------------
    @PostMapping("/{reviewId}/file")
    public ResponseEntity<ReviewFileResponse> uploadReviewFile(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
        return ResponseEntity.ok(
                reviewFileService.uploadReviewFile(type, courseId, reviewId, account.getId(), file)
        );
    }

    // -------------------------------
    // 8. 리뷰 파일 삭제
    // DELETE /api/{type}/course/{courseId}/reviews/{reviewId}/file/{fileId}
    // -------------------------------
    @DeleteMapping("/{reviewId}/file/{fileId}")
    public ResponseEntity<Void> deleteReviewFile(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
        reviewFileService.deleteReviewFile(type, courseId, reviewId, fileId, account.getId());
        return ResponseEntity.noContent().build();
    }

    // -------------------------------
    // 9. 리뷰 파일 복원 (관리자)
    // POST /api/{type}/course/{courseId}/reviews/{reviewId}/file/{fileId}/restore
    // -------------------------------
    @PostMapping("/{reviewId}/file/{fileId}/restore")
    public ResponseEntity<Void> restoreReviewFile(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
        reviewFileService.restoreReviewFile(type, courseId, reviewId, fileId, account.getId());
        return ResponseEntity.noContent().build();
    }

    // -------------------------------
    // 10. 리뷰 파일 영구 삭제 (관리자)
    // DELETE /api/{type}/course/{courseId}/reviews/{reviewId}/file/{fileId}/hard
    // -------------------------------
    @DeleteMapping("/{reviewId}/file/{fileId}/hard")
    public ResponseEntity<Void> hardDeleteReviewFile(
            @PathVariable CategoryType type,
            @PathVariable Long courseId,
            @PathVariable Long reviewId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
        reviewFileService.hardDeleteReviewFile(type, courseId, reviewId, fileId, account.getId());
        return ResponseEntity.noContent().build();
    }
}
