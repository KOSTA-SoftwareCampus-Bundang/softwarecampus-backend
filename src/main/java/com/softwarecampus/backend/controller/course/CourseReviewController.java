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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/reviews")
public class CourseReviewController {

        private final CourseReviewService reviewService;
        private final CourseReviewFileService reviewFileService;
        private final CustomUserDetailsService customUserDetailsService;

        // -------------------------------
        // 1. 리뷰 리스트 조회 (Page)
        // GET /api/courses/{courseId}/reviews
        // -------------------------------
        // -------------------------------
        // 1. 리뷰 리스트 조회 (Page)
        // GET /api/courses/{courseId}/reviews
        // -------------------------------
        @GetMapping
        public ResponseEntity<Page<CourseReviewResponse>> getReviews(
                        @PathVariable Long courseId,
                        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
                        @AuthenticationPrincipal UserDetails userDetails) {
                Long accountId = null;
                if (userDetails != null) {
                        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
                        accountId = account.getId();
                }
                return ResponseEntity.ok(reviewService.getReviews(courseId, pageable, accountId));
        }

        // -------------------------------
        // 2. 리뷰 상세 조회
        // GET /api/courses/{courseId}/reviews/{reviewId}
        // -------------------------------
        @GetMapping("/{reviewId}")
        public ResponseEntity<CourseReviewResponse> getReviewDetail(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId,
                        @AuthenticationPrincipal UserDetails userDetails) {
                Long accountId = null;
                if (userDetails != null) {
                        AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
                        accountId = account.getId();
                }
                return ResponseEntity.ok(reviewService.getReviewDetail(courseId, reviewId, accountId));
        }

        // -------------------------------
        // 3. 리뷰 등록
        // POST /api/courses/{courseId}/reviews
        // -------------------------------
        @PostMapping
        public ResponseEntity<CourseReviewResponse> createReview(
                        @PathVariable Long courseId,
                        @Valid @RequestBody CourseReviewRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {
                AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());

                CourseReviewResponse response = reviewService.createReview(
                                courseId,
                                account.getId(),
                                request);

                return ResponseEntity.ok(response);
        }

        // -------------------------------
        // 4. 리뷰 수정
        // PUT /api/courses/{courseId}/reviews/{reviewId}
        // -------------------------------
        @PutMapping("/{reviewId}")
        public ResponseEntity<CourseReviewResponse> updateReview(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId,
                        @Valid @RequestBody CourseReviewRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {
                AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());

                return ResponseEntity.ok(
                                reviewService.updateReview(courseId, reviewId, account.getId(), request));
        }

        // -------------------------------
        // 5. 리뷰 삭제
        // DELETE /api/courses/{courseId}/reviews/{reviewId}
        // -------------------------------
        @DeleteMapping("/{reviewId}")
        public ResponseEntity<Void> deleteReview(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId,
                        @AuthenticationPrincipal UserDetails userDetails) {
                AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
                reviewService.deleteReview(courseId, reviewId, account.getId());
                return ResponseEntity.noContent().build();
        }

        // -------------------------------
        // 6. 리뷰 삭제 요청
        // POST /api/courses/{courseId}/reviews/{reviewId}/delete-request
        // -------------------------------
        @PostMapping("/{reviewId}/delete-request")
        public ResponseEntity<Void> requestDeleteReview(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId,
                        @AuthenticationPrincipal UserDetails userDetails) {
                AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
                reviewService.requestDeleteReview(courseId, reviewId, account.getId());
                return ResponseEntity.ok().build();
        }

        // -------------------------------
        // 7. 리뷰 파일 업로드
        // POST /api/courses/{courseId}/reviews/{reviewId}/file
        // -------------------------------
        @PostMapping("/{reviewId}/file")
        public ResponseEntity<ReviewFileResponse> uploadReviewFile(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId,
                        @RequestParam("file") MultipartFile file,
                        @AuthenticationPrincipal UserDetails userDetails) {
                AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
                return ResponseEntity.ok(
                                reviewFileService.uploadReviewFile(courseId, reviewId, account.getId(), file));
        }

        // -------------------------------
        // 8. 리뷰 파일 삭제
        // DELETE /api/courses/{courseId}/reviews/{reviewId}/file/{fileId}
        // -------------------------------
        @DeleteMapping("/{reviewId}/file/{fileId}")
        public ResponseEntity<Void> deleteReviewFile(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId,
                        @PathVariable Long fileId,
                        @AuthenticationPrincipal UserDetails userDetails) {
                AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
                reviewFileService.deleteReviewFile(courseId, reviewId, fileId, account.getId());
                return ResponseEntity.noContent().build();
        }

        // -------------------------------
        // 9. 리뷰 파일 복원 (관리자)
        // POST /api/courses/{courseId}/reviews/{reviewId}/file/{fileId}/restore
        // -------------------------------
        @PostMapping("/{reviewId}/file/{fileId}/restore")
        public ResponseEntity<Void> restoreReviewFile(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId,
                        @PathVariable Long fileId,
                        @AuthenticationPrincipal UserDetails userDetails) {
                AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
                reviewFileService.restoreReviewFile(courseId, reviewId, fileId, account.getId());
                return ResponseEntity.noContent().build();
        }

        // -------------------------------
        // 10. 리뷰 파일 영구 삭제 (관리자)
        // DELETE /api/courses/{courseId}/reviews/{reviewId}/file/{fileId}/hard
        // -------------------------------
        @DeleteMapping("/{reviewId}/file/{fileId}/hard")
        public ResponseEntity<Void> hardDeleteReviewFile(
                        @PathVariable Long courseId,
                        @PathVariable Long reviewId,
                        @PathVariable Long fileId,
                        @AuthenticationPrincipal UserDetails userDetails) {
                AccountCacheDto account = customUserDetailsService.getAccountByEmail(userDetails.getUsername());
                reviewFileService.hardDeleteReviewFile(courseId, reviewId, fileId, account.getId());
                return ResponseEntity.noContent().build();
        }

        // 참고: 리뷰 승인/거부 관리자 API는 CourseReviewAdminController (/admin/reviews)로 일원화됨
}
