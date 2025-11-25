package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.ReviewSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewSectionRepository extends JpaRepository<ReviewSection, Long> {

    List<ReviewSection> findByReviewIdAndIsDeletedFalse(Long reviewId);

    @Modifying
    @Query("UPDATE ReviewSection r SET r.isDeleted = true, r.deletedAt = CURRENT_TIMESTAMP WHERE r.review.id = :reviewId")
    void softDeleteByReviewId(@Param("reviewId") Long reviewId);
}
