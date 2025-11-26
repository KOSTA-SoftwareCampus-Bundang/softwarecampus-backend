package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.*;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.*;
import com.softwarecampus.backend.exception.course.BadRequestException;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.repository.course.CourseRepository;
import com.softwarecampus.backend.repository.course.CourseReviewRepository;
import com.softwarecampus.backend.repository.course.ReviewSectionRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewServiceImpl implements CourseReviewService {

    private final CourseReviewRepository reviewRepository;
    private final AccountRepository accountRepository;
    private final CourseRepository courseRepository;
    private final ReviewSectionRepository reviewSectionRepository;

    /**
     * 1. 리뷰 리스트 조회
     */
    @Override
    public List<CourseReviewResponse> getReviews(CategoryType type, Long courseId) {

        Course course = courseRepository.findByIdAndCategory(courseId, type)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        List<CourseReview> reviews = reviewRepository.findAllByCourse_IdAndIsDeletedFalse(courseId);

        return reviews.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 2. 리뷰 상세 조회
     */
    @Override
    public CourseReviewResponse getReviewDetail(CategoryType type, Long courseId, Long reviewId) {

        Course course = courseRepository.findByIdAndCategory(courseId, type)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        CourseReview review = reviewRepository.findById(reviewId)
                .filter(r -> !r.getIsDeleted() && r.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        return toDto(review);
    }

    /**
     * 3. 리뷰 등록
     */
    @Override
    @Transactional
    public CourseReviewResponse createReview(CategoryType type, Long courseId, Long accountId, CourseReviewRequest request) {

        Course course = courseRepository.findByIdAndCategory(courseId, type)
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
        return toDto(saved);
    }

    /**
     * DTO 변환
     */
    private CourseReviewResponse toDto(CourseReview review) {
        return CourseReviewResponse.builder()
                .reviewId(review.getId())
                .writerId(review.getWriter().getId())
                .courseId(review.getCourse().getId())
                .comment(review.getComment())
                .approvalStatus(review.getApprovalStatus().name())
                .averageScore(review.calculateAverageScore())
                .sections(review.getSections().stream()
                        .map(sec -> ReviewSectionResponse.builder()
                                .sectionType(sec.getSectionType().name())
                                .score(sec.getScore())
                                .build()
                        ).toList()
                )
                .attachments(review.getAttachments().stream()
                        .map(ReviewAttachmentResponse::fromEntity)
                        .toList())
                .likeCount((int) review.getLikes().stream()
                        .filter(l -> !l.getIsDeleted() && l.getType() == ReviewLike.LikeType.LIKE)
                        .count())
                .dislikeCount((int) review.getLikes().stream()
                        .filter(l -> !l.getIsDeleted() && l.getType() == ReviewLike.LikeType.DISLIKE)
                        .count())
                .build();
    }

    /**
     * 4. 리뷰 수정
     */
    @Override
    @Transactional
    public CourseReviewResponse updateReview(CategoryType type, Long courseId, Long reviewId, Long accountId, CourseReviewRequest request) {

        Course course = courseRepository.findByIdAndCategory(courseId, type)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

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

        return toDto(review);
    }

    /**
     * 5. 리뷰 삭제 (소프트 삭제)
     */
    @Override
    @Transactional
    public void deleteReview(CategoryType type, Long courseId, Long reviewId, Long accountId) {

        Course course = courseRepository.findByIdAndCategory(courseId, type)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

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
    public void requestDeleteReview(CategoryType type, Long courseId, Long reviewId, Long accountId) {

        Course course = courseRepository.findByIdAndCategory(courseId, type)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        CourseReview review = reviewRepository.findById(reviewId)
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
}

