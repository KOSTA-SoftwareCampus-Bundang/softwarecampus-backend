package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    // 과정별 리뷰 목록
    List<CourseReview> findByCourseIdAndIsDeletedFalse(Long courseId);

    // 승인된 리뷰만 조회
    List<CourseReview> findByCourseIdAndApprovalStatusAndIsDeletedFalse(Long courseId, ApprovalStatus status);

    // 특정 유저가 특정 과정에 작성한 리뷰 (중복 작성 방지)
    Optional<CourseReview> findByWriterIdAndCourseIdAndIsDeletedFalse(Long accountId, Long courseId);

    // 유저 전체 리뷰 조회
    List<CourseReview> findByWriterIdAndIsDeletedFalse(Long accountId);

    /**
     * courseId 기준으로 삭제되지 않은 리뷰 전체 조회
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "writer", "sections", "attachments",
            "likes" })
    List<CourseReview> findAllByCourse_IdAndIsDeletedFalse(Long courseId);

    Optional<CourseReview> findByIdAndCourseIdAndIsDeletedFalse(Long reviewId, Long courseId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "writer", "sections", "attachments",
            "likes" })
    Optional<CourseReview> findWithDetailsById(Long id);
}
