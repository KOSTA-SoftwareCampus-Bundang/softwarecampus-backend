package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    Optional<CourseReview> findByIdAndAccount_Id(Long reviewId, Long accountId);

    long countByCourse_IdAndReviewApproved(Long courseId, ApprovalStatus reviewApproved);

    /**
     * 리뷰 + 섹션 + 작성자를 한 번에 fetch join
     */
    @Query("SELECT DISTINCT r FROM CourseReview r " +
            "LEFT JOIN FETCH r.sections s " +
            "LEFT JOIN FETCH r.account a " +
            "WHERE r.course.id = :courseId AND r.reviewApproved = :status " +
            "ORDER BY r.createdAt DESC")
    List<CourseReview> findByCourseIdWithSections(@Param("courseId") Long courseId,
                                                  @Param("status") ApprovalStatus status);
}
