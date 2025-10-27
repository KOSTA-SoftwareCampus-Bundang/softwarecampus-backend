package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 과정 리뷰 레포지토리
 */
@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    
    // 과정별 승인된 리뷰 목록
    List<CourseReview> findByCourseAndCourseReviewApprovedAndIsDeletedFalseOrderByCreatedAtDesc(
        Course course, 
        ApprovalStatus approved
    );
    
    // 과정별 리뷰 페이징
    Page<CourseReview> findByCourseAndIsDeletedFalse(Course course, Pageable pageable);
}
