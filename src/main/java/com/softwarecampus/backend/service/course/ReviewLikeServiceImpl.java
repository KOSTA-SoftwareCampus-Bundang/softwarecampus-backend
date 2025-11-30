package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.ReviewLike;
import com.softwarecampus.backend.domain.course.ReviewLike.LikeType;
import com.softwarecampus.backend.dto.course.ReviewLikeResponse;
import com.softwarecampus.backend.exception.course.BadRequestException;
import com.softwarecampus.backend.exception.course.NotFoundException;
import com.softwarecampus.backend.repository.course.CourseReviewRepository;
import com.softwarecampus.backend.repository.course.ReviewLikeRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewLikeServiceImpl implements ReviewLikeService {

    private final ReviewLikeRepository reviewLikeRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final AccountRepository accountRepository;

    /**
     * reviewId가 courseId에 속하는지 검증
     * 
     * @throws NotFoundException   reviewId가 존재하지 않거나 course가 null인 경우
     * @throws BadRequestException reviewId가 courseId에 속하지 않는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public void validateReviewBelongsToCourse(Long courseId, Long reviewId) {
        courseReviewRepository.findByIdAndCourseIdAndIsDeletedFalse(reviewId, courseId)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다: " + reviewId));
    }

    @Override
    @Transactional
    public ReviewLikeResponse toggleLike(Long reviewId, Long accountId, LikeType type) {

        // 1) 리뷰와 계정 존재 확인
        var review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다: " + reviewId));

        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("계정을 찾을 수 없습니다: " + accountId));

        // 2) soft-delete 포함 기존 좋아요/싫어요 조회
        var existing = reviewLikeRepository
                .findByReviewIdAndAccountId(reviewId, accountId); // soft-deleted 포함

        ReviewLike resultLike;

        if (existing.isPresent()) {
            var like = existing.get();

            if (!like.isActive()) {
                // soft-deleted 상태면 재활성화 + 타입 변경
                like.restore();
                like.setType(type);
                resultLike = like;
            } else if (like.getType() == type) {
                // 같은 타입이면 취소 (soft delete)
                like.markDeleted();
                resultLike = like;
            } else {
                // 다른 타입이면 타입 변경
                like.setType(type);
                resultLike = like;
            }
        } else {
            // 3) 기존 레코드 없으면 새로 생성
            ReviewLike newLike = ReviewLike.builder()
                    .review(review)
                    .account(account)
                    .type(type)
                    .build();
            resultLike = reviewLikeRepository.save(newLike);
        }

        // 4) 카운트 조회 및 DTO 반환
        long likeCount = reviewLikeRepository.countByReviewIdAndTypeAndIsDeletedFalse(reviewId, LikeType.LIKE);
        long dislikeCount = reviewLikeRepository.countByReviewIdAndTypeAndIsDeletedFalse(reviewId, LikeType.DISLIKE);

        return new ReviewLikeResponse(
                resultLike.isActive() ? resultLike.getType().name() : "NONE",
                likeCount,
                dislikeCount);
    }

    @Override
    public long getLikeCount(Long reviewId) {
        return reviewLikeRepository.countByReviewIdAndTypeAndIsDeletedFalse(reviewId, LikeType.LIKE);
    }

    @Override
    public long getDislikeCount(Long reviewId) {
        return reviewLikeRepository.countByReviewIdAndTypeAndIsDeletedFalse(reviewId, LikeType.DISLIKE);
    }
}
