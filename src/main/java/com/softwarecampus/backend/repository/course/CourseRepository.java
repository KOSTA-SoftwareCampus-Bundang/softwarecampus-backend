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
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "images" })
        List<Course> findByCategory_CategoryTypeAndDeletedAtIsNull(CategoryType type);

        /**
         * 카테고리 타입 + 온/오프라인 필터 조회
         */
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "images" })
        List<Course> findByCategory_CategoryTypeAndIsOfflineAndDeletedAtIsNull(CategoryType type, boolean isOffline);

        /**
         * 카테고리 타입 + 키워드 검색 (+ 온/오프라인 필터 옵션)
         */
        @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "false"))
        @Query(value = "SELECT * FROM course c " +
                        "JOIN course_category cc ON c.category_id = cc.id " +
                        "WHERE cc.category_type = :type " +
                        "AND c.deleted_at IS NULL " +
                        "AND (:isOffline IS NULL OR c.is_offline = :isOffline) " +
                        "AND LOWER(c.name) LIKE CONCAT('%', LOWER(:keyword), '%')", nativeQuery = true)
        List<Course> searchByName(@Param("type") String type, @Param("keyword") String keyword,
                        @Param("isOffline") Boolean isOffline);

        // 등록 요청 목록 (승인 대기 중)
        List<Course> findByIsApproved(ApprovalStatus status);

        // courseId 와 categoryType 으로 단일 Course 조회
        Optional<Course> findByIdAndCategory_CategoryType(Long id, CategoryType type);

        /**
         * ID로 과정 조회 (삭제된 과정 제외)
         */
        Optional<Course> findByIdAndDeletedAtIsNull(Long id);

        /**
         * ID로 과정 상세 조회 (삭제된 과정 제외, 연관엔티티 함께 로딩)
         * APPROVED 상태이고, Academy와 Category가 삭제되지 않은 경우만 조회
         * 
         * Note: MultipleBagFetchException 방지를 위해 curriculums만 FETCH
         * images는 필요시 lazy loading으로 처리
         */
        @Query("SELECT c FROM Course c " +
                        "JOIN FETCH c.academy a " +
                        "JOIN FETCH c.category cat " +
                        "LEFT JOIN FETCH c.curriculums " +
                        "WHERE c.id = :id " +
                        "AND c.deletedAt IS NULL " +
                        "AND c.isApproved = 'APPROVED' " +
                        "AND a.deletedAt IS NULL " +
                        "AND cat.deletedAt IS NULL")
        Optional<Course> findWithDetailsByIdAndDeletedAtIsNull(@Param("id") Long id);

}
