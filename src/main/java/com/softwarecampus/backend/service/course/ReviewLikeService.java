package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.ReviewLike;
import com.softwarecampus.backend.domain.course.ReviewLike.LikeType;

public interface ReviewLikeService {

    ReviewLike toggleLike(Long reviewId, Long accountId, LikeType type);

    long getLikeCount(Long reviewId);

    long getDislikeCount(Long reviewId);

//    // 좋아요 / 싫어요 토글 (반환값: 최종 상태)
//    void toggleLike(Long reviewId, Long accountId, LikeType type);
//
//    // 좋아요 개수
//    int countLikes(Long reviewId);
//
//    // 싫어요 개수
//    int countDislikes(Long reviewId);

    // 해당 유저가 좋아요/싫어요 눌렀는지 조회
 //   LikeType getUserLikeType(Long reviewId, Long accountId);
}
