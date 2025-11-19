package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.*;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.*;
import com.softwarecampus.backend.repository.course.*;
import com.softwarecampus.backend.repository.user.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseReviewServiceImpl implements CourseReviewService {

    private final CourseReviewRepository courseReviewRepository;
    private final CourseReviewRecommendRepository recommendRepository;
    private final AccountRepository accountRepository;
    private final com.softwarecampus.backend.repository.course.CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(Long courseId) {

        // 1) 리뷰 + 섹션 + 작성자 fetch join
        List<CourseReview> reviews = courseReviewRepository
                .findByCourseIdWithSections(courseId, ApprovalStatus.APPROVED);

        if (reviews.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 리뷰 ID 추출
        List<Long> reviewIds = reviews.stream()
                .map(CourseReview::getId)
                .toList();

        // 3) 좋아요/싫어요 일괄 조회
        Map<Long, Long> likeMap = recommendRepository.countLikes(reviewIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        Map<Long, Long> dislikeMap = recommendRepository.countDislikes(reviewIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        // 4) map을 이용해 toResponse 호출 (N+1 방지)
        return reviews.stream()
                .map(r -> toResponse(r, likeMap, dislikeMap))
                .toList();
    }



    @Override
    public Long createReview(Long courseId, ReviewCreateRequest request, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("과정이 존재하지 않습니다."));

        CourseReview review = CourseReview.builder()
                .title(request.getTitle())
                .course(course)
                .account(account)
                .reviewApproved(ApprovalStatus.APPROVED)
                .build();

        for (ReviewSectionRequest s : request.getSections()) {
            ReviewSection section = ReviewSection.builder()
                    .sectionType(s.getSectionType())
                    .point(s.getSectionType().isHasRating() ? s.getPoint() : null)
                    .text(s.getText())
                    .review(review)
                    .build();
            review.addSection(section);
        }

        courseReviewRepository.save(review);
        return review.getId();
    }

    @Override
    public void updateReview(Long reviewId, ReviewUpdateRequest request, Long accountId) {
        CourseReview review = courseReviewRepository.findByIdAndAccount_Id(reviewId, accountId)
                .orElseThrow(() -> new EntityNotFoundException("본인 리뷰만 수정할 수 있습니다."));

        review.setTitle(request.getTitle());
        review.getSections().clear();

        for (ReviewSectionRequest s : request.getSections()) {
            ReviewSection section = ReviewSection.builder()
                    .sectionType(s.getSectionType())
                    .point(s.getSectionType().isHasRating() ? s.getPoint() : null)
                    .text(s.getText())
                    .review(review)
                    .build();
            review.addSection(section);
        }
    }

    @Override
    public void deleteReview(Long reviewId, Long accountId) {
        CourseReview review = courseReviewRepository.findByIdAndAccount_Id(reviewId, accountId)
                .orElseThrow(() -> new EntityNotFoundException("삭제 권한이 없습니다."));
        courseReviewRepository.delete(review);
    }

    @Override
    public void requestDeleteReview(Long reviewId, Long accountId) {
        CourseReview review = courseReviewRepository.findByIdAndAccount_Id(reviewId, accountId)
                .orElseThrow(() -> new EntityNotFoundException("삭제 요청할 수 없습니다."));
        review.setReviewApproved(ApprovalStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewDetail(Long reviewId) {
        CourseReview review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));

        // 단일 리뷰 ID를 리스트로 만들어 일괄 조회 메서드 사용
        List<Long> reviewIds = List.of(reviewId);

        Map<Long, Long> likeMap = recommendRepository.countLikes(reviewIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        Map<Long, Long> dislikeMap = recommendRepository.countDislikes(reviewIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        return toResponse(review, likeMap, dislikeMap);
    }



    @Override
    public void recommendReview(Long reviewId, Long accountId, boolean liked) {
        CourseReview review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));

        Optional<CourseReviewRecommend> existing =
                recommendRepository.findByReview_IdAndAccount_Id(reviewId, accountId);

        if (existing.isPresent()) {
            CourseReviewRecommend r = existing.get();
            r.setLiked(liked);
        } else {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));
            CourseReviewRecommend newRec = CourseReviewRecommend.builder()
                    .review(review)
                    .account(account)
                    .liked(liked)
                    .build();
            recommendRepository.save(newRec);
        }
    }

    // ====================
    // 내부 변환 로직
    // ====================
    private ReviewResponse toResponse(CourseReview review,
                                      Map<Long, Long> likeMap,
                                      Map<Long, Long> dislikeMap) {

        List<ReviewSectionResponse> sections = review.getSections().stream()
                .map(s -> ReviewSectionResponse.builder()
                        .sectionType(s.getSectionType())
                        .point(s.getPoint())
                        .text(s.getText())
                        .build())
                .toList();

        double avg = review.getSections().stream()
                .filter(s -> s.getSectionType().isHasRating() && s.getPoint() != null)
                .mapToInt(ReviewSection::getPoint)
                .average()
                .orElse(0.0);

        long likeCount = likeMap.getOrDefault(review.getId(), 0L);
        long dislikeCount = dislikeMap.getOrDefault(review.getId(), 0L);

        return ReviewResponse.builder()
                .id(review.getId())
                .title(review.getTitle())
                .authorName(review.getAccount() != null ? review.getAccount().getUserName() : "익명")
                .sections(sections)
                .averageScore(avg)
                .likeCount((int) likeCount)
                .dislikeCount((int) dislikeCount)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

}
