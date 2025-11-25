package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.*;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.*;
import com.softwarecampus.backend.exception.course.BadRequestException;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.repository.course.CourseRepository;
import com.softwarecampus.backend.repository.course.CourseReviewRepository;
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

    @Override
    public CourseReviewResponse getReview(CategoryType type, Long courseId, Long reviewId) {

        Course course = courseRepository.findByIdAndCategory(courseId, type)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        CourseReview review = reviewRepository.findById(reviewId)
                .filter(r -> !r.getIsDeleted() && r.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        return toDto(review);
    }

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

        // 응답 DTO 변환
        return toDto(saved);
    }

    private CourseReviewResponse toDto(CourseReview review) {
        return CourseReviewResponse.builder()
                .reviewId(review.getId())
                .writerId(review.getWriter().getId())
                .courseId(review.getCourse().getId())
                .comment(review.getComment())
                .approvalStatus(review.getApprovalStatus().name())
                .averageScore(review.calculateAverageScore())
                .sections(
                        review.getSections().stream()
                                .map(sec -> ReviewSectionResponse.builder()
                                        .sectionType(sec.getSectionType().name())
                                        .score(sec.getScore())
                                        .build()
                                ).toList()
                )
                .attachments(List.of()) // 아직 구현 안 된 첨부파일
                .likeCount(0)
                .dislikeCount(0)
                .build();
    }

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

        review.setComment(request.getComment());

        review.getSections().clear();

        for (ReviewSectionRequest sec : request.getSections()) {

            ReviewSectionType sectionType = ReviewSectionType.from(sec.getSectionType());

            ReviewSection newSection = ReviewSection.builder()
                    .review(review)
                    .sectionType(sectionType)
                    .score(sec.getScore())
                    .build();

            review.getSections().add(newSection);
        }

        reviewRepository.save(review);

        return CourseReviewResponse.builder()
                .reviewId(review.getId())
                .writerId(review.getWriter().getId())
                .courseId(review.getCourse().getId())
                .comment(review.getComment())
                .approvalStatus(review.getApprovalStatus().name())
                .averageScore(review.calculateAverageScore())
                .sections(review.getSections().stream()
                        .map(ReviewSectionResponse::fromEntity)
                        .toList())
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

        review.markDeleted();
        reviewRepository.save(review);
    }



}
