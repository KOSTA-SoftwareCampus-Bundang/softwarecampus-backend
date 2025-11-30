package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.ReviewLike;
import com.softwarecampus.backend.domain.course.ReviewLike.LikeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    // 한 유저가 이미 눌렀는지 체크
    Optional<ReviewLike> findByReviewIdAndAccountId(Long reviewId, Long accountId);

    // LikeType 에 따라 좋아요/싫어요 수 조회
    int countByReviewIdAndType(Long reviewId, LikeType type);

}
