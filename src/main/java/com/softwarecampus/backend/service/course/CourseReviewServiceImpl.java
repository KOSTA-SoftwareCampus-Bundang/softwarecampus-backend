package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.*;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.*;
import com.softwarecampus.backend.exception.course.BadRequestException;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.course.CourseRepository;
import com.softwarecampus.backend.repository.course.CourseReviewRepository;
import com.softwarecampus.backend.repository.course.ReviewSectionRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewServiceImpl implements CourseReviewService {

        private final CourseReviewRepository reviewRepository;
        private final AccountRepository accountRepository;
        private final CourseRepository courseRepository;
        private final ReviewSectionRepository reviewSectionRepository;
        private final AcademyRepository academyRepository;

        /**
         * 1. 리뷰 리스트 조회 (Pageable)
         * - 승인된 후기(APPROVED)만 조회
         * - 작성자 본인은 자신의 대기 중(PENDING) 후기도 조회 가능
         */
        @Override
        public Page<CourseReviewResponse> getReviews(Long courseId, Pageable pageable, Long accountId) {

                if (courseId == null) {
                        throw new IllegalArgumentException("Course ID cannot be null");
                }
                if (!courseRepository.existsById(courseId)) {
                        throw new EntityNotFoundException("Course not found");
                }

                // 승인된 후기만 조회
                Page<CourseReview> reviewPage = reviewRepository
                                .findByCourseIdAndApprovalStatusAndIsDeletedFalse(
                                                courseId, ApprovalStatus.APPROVED, pageable);

                return reviewPage.map(review -> toDto(review, accountId));
        }

        /**
         * 2. 리뷰 상세 조회
         */
        @Override
        public CourseReviewResponse getReviewDetail(@NonNull Long courseId, @NonNull Long reviewId, Long accountId) {

                if (!courseRepository.existsById(courseId)) {
                        throw new EntityNotFoundException("Course not found");
                }

                CourseReview review = reviewRepository.findWithDetailsByIdAndIsDeletedFalse(reviewId)
                                .filter(r -> r.getCourse().getId().equals(courseId))
                                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

                return toDto(review, accountId);
        }

        @Override
        @Transactional
        public CourseReviewResponse createReview(@NonNull Long courseId, @NonNull Long accountId,
                        CourseReviewRequest request) {

                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

                Account writer = accountRepository.findById(accountId)
                                .orElseThrow(() -> new EntityNotFoundException("User not found"));

                CourseReview review = CourseReview.builder()
                                .course(course)
                                .writer(writer)
                                .comment(request.getComment())
                                .approvalStatus(ApprovalStatus.PENDING)
                                .build();

                // 섹션 추가
                if (request.getSections() != null) {
                        for (ReviewSectionRequest secReq : request.getSections()) {

                                ReviewSection section = ReviewSection.builder()
                                                .sectionType(ReviewSectionType.from(secReq.getSectionType()))
                                                .score(secReq.getScore())
                                                .review(review)
                                                .build();

                                review.getSections().add(section);
                        }
                }

                CourseReview saved = reviewRepository.save(review);
                return toDto(saved, accountId);
        }

        /**
         * DTO 변환
         */
        private CourseReviewResponse toDto(@NonNull CourseReview review, Long accountId) {
                String myLikeType = "NONE";
                if (accountId != null) {
                        myLikeType = review.getLikes().stream()
                                        .filter(l -> l.getAccount().getId().equals(accountId))
                                        .findFirst()
                                        .map(l -> l.getType().name())
                                        .orElse("NONE");
                }

                return CourseReviewResponse.builder()
                                .reviewId(review.getId())
                                .writerId(review.getWriter().getId())
                                .writerName(review.getWriter().getUserName()) // 추가
                                .courseId(review.getCourse().getId())
                                .courseName(review.getCourse().getName()) // 추가: 과정 이름
                                .comment(review.getComment())
                                .approvalStatus(review.getApprovalStatus().name())
                                .averageScore(review.calculateAverageScore())
                                .sections(review.getSections().stream()
                                                .map(sec -> ReviewSectionResponse.builder()
                                                                .sectionType(sec.getSectionType().name())
                                                                .score(sec.getScore())
                                                                .build())
                                                .toList())
                                .attachments(review.getAttachments().stream()
                                                .map(ReviewAttachmentResponse::fromEntity)
                                                .toList())
                                .likeCount((int) review.getLikes().stream()
                                                .filter(l -> l.getType() == ReviewLike.LikeType.LIKE)
                                                .count())
                                .dislikeCount((int) review.getLikes().stream()
                                                .filter(l -> l.getType() == ReviewLike.LikeType.DISLIKE)
                                                .count())
                                .myLikeType(myLikeType)
                                .createdAt(review.getCreatedAt()) // 추가
                                .build();
        }

        /**
         * 4. 리뷰 수정
         */
        @Override
        @Transactional
        public CourseReviewResponse updateReview(@NonNull Long courseId, @NonNull Long reviewId,
                        @NonNull Long accountId,
                        CourseReviewRequest request) {

                if (!courseRepository.existsById(courseId)) {
                        throw new EntityNotFoundException("Course not found");
                }

                CourseReview review = reviewRepository.findById(reviewId)
                                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

                if (!review.getCourse().getId().equals(courseId)) {
                        throw new BadRequestException("이 리뷰는 해당 코스에 속하지 않습니다.");
                }

                if (!review.getWriter().getId().equals(accountId)) {
                        throw new ForbiddenException("본인이 작성한 리뷰만 수정할 수 있습니다.");
                }

                if (review.getIsDeleted()) {
                        throw new BadRequestException("이미 삭제된 리뷰는 수정할 수 없습니다.");
                }

                review.setComment(request.getComment());

                // 기존 섹션 삭제 후 다시 생성
                review.getSections().clear();

                if (request.getSections() != null) {
                        for (ReviewSectionRequest sec : request.getSections()) {
                                ReviewSectionType sectionType = ReviewSectionType.from(sec.getSectionType());

                                ReviewSection newSection = ReviewSection.builder()
                                                .review(review)
                                                .sectionType(sectionType)
                                                .score(sec.getScore())
                                                .build();

                                review.getSections().add(newSection);
                        }
                }

                return toDto(review, accountId);
        }

        /**
         * 5. 리뷰 삭제 (소프트 삭제)
         */
        @Override
        @Transactional
        public void deleteReview(@NonNull Long courseId, @NonNull Long reviewId, @NonNull Long accountId) {

                if (courseId == null) {
                        throw new IllegalArgumentException("Course ID cannot be null");
                }
                if (!courseRepository.existsById(courseId)) {
                        throw new EntityNotFoundException("Course not found");
                }

                CourseReview review = reviewRepository.findById(reviewId)
                                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

                if (!review.getCourse().getId().equals(courseId)) {
                        throw new BadRequestException("이 리뷰는 해당 과정의 리뷰가 아닙니다.");
                }

                if (!review.getWriter().getId().equals(accountId)) {
                        throw new ForbiddenException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
                }

                review.markDeleted(); // soft delete
                reviewSectionRepository.softDeleteByReviewId(reviewId);
        }

        /**
         * 6. 리뷰 삭제 요청
         */
        @Override
        @Transactional
        public void requestDeleteReview(@NonNull Long courseId, @NonNull Long reviewId, @NonNull Long accountId) {

                if (!courseRepository.existsById(courseId)) {
                        throw new EntityNotFoundException("Course not found");
                }

                CourseReview review = reviewRepository.findWithDetailsByIdAndIsDeletedFalse(reviewId)
                                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

                if (!review.getCourse().getId().equals(courseId)) {
                        throw new BadRequestException("이 리뷰는 해당 과정의 리뷰가 아닙니다.");
                }

                // 작성자만 삭제 요청 가능하도록
                if (!review.getWriter().getId().equals(accountId)) {
                        throw new ForbiddenException("본인이 작성한 리뷰만 삭제 요청할 수 있습니다.");
                }

                review.requestDelete(); // 삭제 요청 상태로 변경 (도메인에서 구현 필요)
        }

        @Override
        public Page<CourseReviewResponse> getAdminReviews(ApprovalStatus status, String keyword, Pageable pageable) {
                Page<CourseReview> reviewPage = reviewRepository.searchAdminReviews(status, keyword, pageable);
                return reviewPage.map(review -> toDto(review, null));
        }

        /**
         * 리뷰 승인 (관리자용)
         * 수정일: 2025-12-02 - Soft Delete 준수, 중복 승인 검증 추가
         */
        @Override
        @Transactional
        public CourseReviewResponse approveReview(@NonNull Long reviewId) {
                // Soft Delete 준수: findByIdAndIsDeletedFalse 사용
                CourseReview review = reviewRepository.findByIdAndIsDeletedFalse(reviewId)
                                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

                // 이미 승인된 리뷰인지 검증
                if (review.getApprovalStatus() == ApprovalStatus.APPROVED) {
                        // 이미 승인된 경우 현재 상태 그대로 반환
                        return toDto(review, null);
                }

                review.setApprovalStatus(ApprovalStatus.APPROVED);
                review.setRejectionReason(null); // 승인 시 거부 사유 초기화
                return toDto(review, null);
        }

        /**
         * 리뷰 거부 (관리자용)
         * 수정일: 2025-12-02 - Soft Delete 준수, 거부 사유 저장
         */
        @Override
        @Transactional
        public CourseReviewResponse rejectReview(@NonNull Long reviewId, String reason) {
                // Soft Delete 준수: findByIdAndIsDeletedFalse 사용
                CourseReview review = reviewRepository.findByIdAndIsDeletedFalse(reviewId)
                                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

                review.setApprovalStatus(ApprovalStatus.REJECTED);
                review.setRejectionReason(reason); // 거부 사유 저장
                return toDto(review, null);
        }

        @Override
        @Transactional
        public Page<CourseReviewResponse> getInstitutionReviews(@NonNull Long academyId, ApprovalStatus status, String keyword,
                        Pageable pageable) {
                if (!academyRepository.existsById(academyId)) {
                        throw new EntityNotFoundException("기관을 찾을 수 없습니다");
                }
                Page<CourseReview> reviewPage = reviewRepository.searchInstitutionReviews(academyId, status, keyword,
                                pageable);
                return reviewPage.map(review -> toDto(review, null));
        }
}
