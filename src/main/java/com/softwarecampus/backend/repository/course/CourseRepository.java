package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CategoryType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * 카테고리 타입별 과정 전체 조회 (재직자/취업예정자)
     */
    List<Course> findByCategory_CategoryTypeAndDeletedAtIsNull(CategoryType type);

    /**
     * 카테고리 타입 + 키워드 검색
     */
    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "false"))
    @Query(value = "SELECT * FROM course c " +
            "JOIN course_category cc ON c.category_id = cc.id " +
            "WHERE cc.category_type = :type " +
            "AND c.deleted_at IS NULL " +
            "AND LOWER(c.name) LIKE CONCAT('%', LOWER(:keyword), '%')",
            nativeQuery = true)
    List<Course> searchByName(@Param("type") String type, @Param("keyword") String keyword);






    // 등록 요청 목록 (승인 대기 중)
    List<Course> findByIsApproved(ApprovalStatus status);

    // courseId 와 categoryType 으로 단일 Course 조회
    Optional<Course> findByIdAndCategory(Long id, CategoryType category);

    Optional<Course> findByIdAndCategory_CategoryType(Long id, CategoryType type);


}
