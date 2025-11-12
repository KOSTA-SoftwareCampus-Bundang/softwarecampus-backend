package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseReviewRecommend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseReviewRecommendRepository extends JpaRepository<CourseReviewRecommend, Long> {

    /** ✅ 특정 회원이 특정 리뷰를 추천/비추천 했는지 조회 */
    Optional<CourseReviewRecommend> findByReview_IdAndAccount_Id(Long reviewId, Long accountId);

    /** ✅ 특정 리뷰의 추천 수 */
    long countByReview_IdAndLikedTrue(Long reviewId);

    /** ✅ 특정 리뷰의 비추천 수 */
    long countByReview_IdAndLikedFalse(Long reviewId);
}
