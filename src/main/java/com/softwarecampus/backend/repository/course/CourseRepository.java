package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 과정 레포지토리
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    // 카테고리별 승인된 과정 목록 조회
    List<Course> findByCourseCategoryAndIsApprovedAndIsDeletedFalseOrderByCreatedAtDesc(
        CourseCategory category, 
        ApprovalStatus approved
    );
    
    // 기관별 과정 목록 조회
    List<Course> findByAcademyAndIsDeletedFalseOrderByCreatedAtDesc(Academy academy);
    
    // 승인 상태별 조회
    Page<Course> findByIsApprovedAndIsDeletedFalse(ApprovalStatus approved, Pageable pageable);
    
    // 커스텀 검색 쿼리
    @Query("SELECT c FROM Course c WHERE " +
           "c.isApproved = 'APPROVED' AND " +
           "c.isDeleted = false AND " +
           "(:category IS NULL OR c.courseCategory = :category) AND " +
           "(:keyword IS NULL OR c.name LIKE %:keyword%)")
    Page<Course> searchCourses(
        @Param("category") CourseCategory category,
        @Param("keyword") String keyword,
        Pageable pageable
    );
    
    // 진행중인 과정 (모집 기간 기준)
    @Query("SELECT c FROM Course c WHERE " +
           "c.isApproved = 'APPROVED' AND " +
           "c.isDeleted = false AND " +
           "c.recrutStart <= :now AND c.recrutEnd >= :now")
    List<Course> findOngoingRecruitment(@Param("now") LocalDateTime now);
}
