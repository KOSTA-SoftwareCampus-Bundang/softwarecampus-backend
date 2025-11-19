package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseReviewRecommend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRecommendRepository extends JpaRepository<CourseReviewRecommend, Long> {

    Optional<CourseReviewRecommend> findByReview_IdAndAccount_Id(Long reviewId, Long accountId);

    // 좋아요/싫어요 일괄 조회
    @Query("SELECT r.review.id, COUNT(r) FROM CourseReviewRecommend r " +
            "WHERE r.review.id IN :reviewIds AND r.liked = true GROUP BY r.review.id")
    List<Object[]> countLikes(@Param("reviewIds") List<Long> reviewIds);

    @Query("SELECT r.review.id, COUNT(r) FROM CourseReviewRecommend r " +
            "WHERE r.review.id IN :reviewIds AND r.liked = false GROUP BY r.review.id")
    List<Object[]> countDislikes(@Param("reviewIds") List<Long> reviewIds);
}
