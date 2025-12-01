package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import com.softwarecampus.backend.domain.course.CourseStatus;

public interface CourseRepository extends JpaRepository<Course, Long> {

        /**
         * 카테고리 타입별 과정 전체 조회 (재직자/취업예정자)
         * 
         * @deprecated searchCourses() 페이지네이션 버전 사용 권장
         */
        @Deprecated
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "images" })
        List<Course> findByCategory_CategoryTypeAndDeletedAtIsNull(CategoryType type);

        /**
         * 카테고리 타입 + 온/오프라인 필터 조회
         * 
         * @deprecated searchCourses() 페이지네이션 버전 사용 권장
         */
        @Deprecated
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "images" })
        List<Course> findByCategory_CategoryTypeAndIsOfflineAndDeletedAtIsNull(CategoryType type, boolean isOffline);

        /**
         * 온/오프라인 필터만으로 조회
         * 
         * @deprecated searchCourses() 페이지네이션 버전 사용 권장
         */
        @Deprecated
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "images" })
        List<Course> findByIsOfflineAndDeletedAtIsNull(boolean isOffline);

        /**
         * 전체 과정 조회 (삭제되지 않은 것만)
         * 
         * @deprecated searchCourses() 페이지네이션 버전 사용 권장
         */
        @Deprecated
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "images" })
        List<Course> findByDeletedAtIsNull();

        /**
         * 카테고리 ID + 온/오프라인 필터 조회
         * 
         * @deprecated searchCourses() 페이지네이션 버전 사용 권장
         */
        @Deprecated
        @Query("SELECT c FROM Course c " +
                        "WHERE c.category.id = :categoryId " +
                        "AND c.deletedAt IS NULL " +
                        "AND (:isOffline IS NULL OR c.isOffline = :isOffline)")
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "images" })
        List<Course> findByCategoryIdAndFilters(@Param("categoryId") Long categoryId,
                        @Param("isOffline") Boolean isOffline);

        /**
         * 통합 검색 (categoryId, categoryType, isOffline, keyword 조합) - 페이지네이션 지원
         * 
         * 성능 고려사항:
         * - LIKE '%keyword%' 패턴은 인덱스를 활용할 수 없음
         * - 대용량 데이터의 경우 course.name 컨럼에 Full-Text Index 추가 권장
         * - 또는 Elasticsearch 등 전문 검색 엔진 도입 검토
         * 
         * @param categoryId   카테고리 ID (옵션)
         * @param categoryType 카테고리 타입 (옵션)
         * @param isOffline    온/오프라인 필터 (옵션)
         * @param keyword      검색 키워드 (옵션)
         * @param pageable     페이지 정보
         * @return 페이지네이션된 과정 목록
         */
        @Query("SELECT c FROM Course c " +
                        "WHERE c.deletedAt IS NULL " +
                        "AND (:categoryId IS NULL OR c.category.id = :categoryId) " +
                        "AND (:categoryType IS NULL OR c.category.categoryType = :categoryType) " +
                        "AND (:isOffline IS NULL OR c.isOffline = :isOffline) " +
                        "AND (:keyword IS NULL OR LOWER(c.name) LIKE CONCAT('%', LOWER(:keyword), '%')) " +
                        "AND (:status IS NULL OR (" +
                        "   (:status = com.softwarecampus.backend.domain.course.CourseStatus.RECRUITING AND c.recruitStart IS NOT NULL AND c.recruitEnd IS NOT NULL AND CURRENT_DATE BETWEEN c.recruitStart AND c.recruitEnd) OR "
                        +
                        "   (:status = com.softwarecampus.backend.domain.course.CourseStatus.IN_PROGRESS AND c.courseStart IS NOT NULL AND c.courseEnd IS NOT NULL AND CURRENT_DATE BETWEEN c.courseStart AND c.courseEnd) OR "
                        +
                        "   (:status = com.softwarecampus.backend.domain.course.CourseStatus.ENDED AND c.courseEnd IS NOT NULL AND CURRENT_DATE > c.courseEnd)"
                        +
                        "))")
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "images" })
        Page<Course> searchCourses(@Param("categoryId") Long categoryId,
                        @Param("categoryType") CategoryType categoryType,
                        @Param("isOffline") Boolean isOffline,
                        @Param("keyword") String keyword,
                        @Param("status") CourseStatus status,
                        Pageable pageable);

        /**
         * 통합 검색 (categoryId, categoryType, isOffline, keyword 조합) - 전체 조회
         * 
         * @deprecated 페이지네이션 버전 사용 권장
         */
        @Deprecated
        @Query("SELECT c FROM Course c " +
                        "WHERE c.deletedAt IS NULL " +
                        "AND (:categoryId IS NULL OR c.category.id = :categoryId) " +
                        "AND (:categoryType IS NULL OR c.category.categoryType = :categoryType) " +
                        "AND (:isOffline IS NULL OR c.isOffline = :isOffline) " +
                        "AND (:keyword IS NULL OR LOWER(c.name) LIKE CONCAT('%', LOWER(:keyword), '%'))")
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "images" })
        List<Course> searchCoursesAll(@Param("categoryId") Long categoryId,
                        @Param("categoryType") CategoryType categoryType,
                        @Param("isOffline") Boolean isOffline,
                        @Param("keyword") String keyword);

        @Query("SELECT DISTINCT c FROM Course c " +
                        "LEFT JOIN FETCH c.reviews r " +
                        "LEFT JOIN FETCH r.sections " +
                        "LEFT JOIN FETCH c.favorites " +
                        "WHERE c.category.categoryType = :categoryType AND c.deletedAt IS NULL")
        List<Course> findHomeCoursesByCategory(@Param("categoryType") CategoryType categoryType);

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
