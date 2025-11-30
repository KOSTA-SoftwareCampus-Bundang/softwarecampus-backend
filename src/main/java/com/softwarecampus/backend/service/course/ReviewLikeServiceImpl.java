package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.ReviewLike;
import com.softwarecampus.backend.domain.course.ReviewLike.LikeType;
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
     * @throws NotFoundException reviewId가 존재하지 않거나 course가 null인 경우
     * @throws BadRequestException reviewId가 courseId에 속하지 않는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public void validateReviewBelongsToCourse(Long courseId, Long reviewId) {
        var review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다: " + reviewId));

        // Lazy loading 방지 및 NPE 방지
        if (review.getCourse() == null) {
            throw new NotFoundException("리뷰(ID: " + reviewId + ")에 연결된 과정이 없습니다.");
        }

        if (!review.getCourse().getId().equals(courseId)) {
            throw new BadRequestException(
                    String.format("리뷰(ID: %d)는 과정(ID: %d)에 속하지 않습니다.", reviewId, courseId)
            );
        }
    }

    @Override
    @Transactional
    public ReviewLike toggleLike(Long reviewId, Long accountId, LikeType type) {

        // 1) 리뷰와 계정 존재 확인
        var review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다: " + reviewId));

        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("계정을 찾을 수 없습니다: " + accountId));

        // 2) soft-delete 포함 기존 좋아요/싫어요 조회
        var existing = reviewLikeRepository
                .findByReviewIdAndAccountId(reviewId, accountId); // soft-deleted 포함

        if (existing.isPresent()) {
            var like = existing.get();

            if (!like.isActive()) {
                // soft-deleted 상태면 재활성화 + 타입 변경
                like.restore();
                like.setType(type);
                return like;
            }

            // 같은 타입이면 취소 (soft delete)
            if (like.getType() == type) {
                like.markDeleted();
                return like;
            }

            // 다른 타입이면 타입 변경
            like.setType(type);
            return like;
        }

        // 3) 기존 레코드 없으면 새로 생성
        ReviewLike newLike = ReviewLike.builder()
                .review(review)
                .account(account)
                .type(type)
                .build();

        return reviewLikeRepository.save(newLike);
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
