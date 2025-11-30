package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.ReviewLike;
import com.softwarecampus.backend.domain.course.ReviewLike.LikeType;
import com.softwarecampus.backend.dto.course.ReviewLikeResponse;

public interface ReviewLikeService {

    /**
     * reviewId가 courseId에 속하는지 검증
     * 
     * @param courseId 코스 ID
     * @param reviewId 리뷰 ID
     * @throws com.softwarecampus.backend.exception.course.NotFoundException 리뷰를 찾을
     *                                                                       수 없거나
     *                                                                       삭제된 경우
     */
    void validateReviewBelongsToCourse(Long courseId, Long reviewId);

    ReviewLikeResponse toggleLike(Long courseId, Long reviewId, Long accountId, LikeType type);

    long getLikeCount(Long reviewId);

    long getDislikeCount(Long reviewId);

}
