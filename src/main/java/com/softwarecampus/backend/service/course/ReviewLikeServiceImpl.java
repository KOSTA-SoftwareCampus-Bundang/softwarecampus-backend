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

    /**
     * 리뷰 좋아요/싫어요 토글
     * 
     * <p>
     * 하드 삭제 정책:
     * </p>
     * <ul>
     * <li>같은 타입 클릭: DELETE (취소)</li>
     * <li>다른 타입 클릭: UPDATE (타입 변경)</li>
     * <li>신규 클릭: INSERT</li>
     * </ul>
     * 
     * @param courseId  코스 ID (검증용)
     * @param reviewId  리뷰 ID
     * @param accountId 계정 ID
     * @param type      좋아요/싫어요 타입
     * @return 토글 결과 및 카운트
     */
    @Override
    @Transactional
    public ReviewLikeResponse toggleLike(Long courseId, Long reviewId, Long accountId, LikeType type) {

        // 1) 삭제되지 않은 리뷰 검증 (courseId도 함께 검증)
        var review = courseReviewRepository.findByIdAndCourseIdAndIsDeletedFalse(reviewId, courseId)
                .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없거나 삭제되었습니다"));

        // 2) 계정 존재 확인
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("계정을 찾을 수 없습니다: " + accountId));

        // 3) 기존 좋아요/싫어요 조회
        var existing = reviewLikeRepository.findByReviewIdAndAccountId(reviewId, accountId);

        ReviewLike resultLike = null;
        String resultType;
        long likeCount;
        long dislikeCount;

        if (existing.isPresent()) {
            var like = existing.get();

            if (like.getType() == type) {
                // 같은 타입: 취소 (하드 삭제)
                reviewLikeRepository.delete(like);
                resultType = "NONE";
            } else {
                // 다른 타입: 타입 변경 (UPDATE)
                like.setType(type);
                resultLike = like;
                resultType = type.name();
            }
        } else {
            // 신규: INSERT
            ReviewLike newLike = ReviewLike.builder()
                    .review(review)
                    .account(account)
                    .type(type)
                    .build();
            resultLike = reviewLikeRepository.save(newLike);
            resultType = type.name();
        }

        // 4) 카운트 조회
        likeCount = reviewLikeRepository.countByReviewIdAndType(reviewId, LikeType.LIKE);
        dislikeCount = reviewLikeRepository.countByReviewIdAndType(reviewId, LikeType.DISLIKE);

        return new ReviewLikeResponse(resultType, likeCount, dislikeCount);
    }

    @Override
    public long getLikeCount(Long reviewId) {
        return reviewLikeRepository.countByReviewIdAndType(reviewId, LikeType.LIKE);
    }

    @Override
    public long getDislikeCount(Long reviewId) {
        return reviewLikeRepository.countByReviewIdAndType(reviewId, LikeType.DISLIKE);
    }
}
