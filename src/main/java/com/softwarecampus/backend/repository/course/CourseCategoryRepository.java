package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {

    @Modifying
    @Query(value = "INSERT INTO course_category (id, category_name, category_type, is_deleted) VALUES (:id, :name, :type, false)", nativeQuery = true)
    void saveWithId(@Param("id") Long id, @Param("name") String name, @Param("type") String type);

    Optional<CourseCategory> findByCategoryTypeAndCategoryName(CategoryType categoryType, String categoryName);

    /**
     * 삭제되지 않은 전체 카테고리 목록 조회
     */
    List<CourseCategory> findAllByDeletedAtIsNull();

    /**
     * 특정 타입의 카테고리 목록 조회
     */
    List<CourseCategory> findByCategoryTypeAndDeletedAtIsNull(CategoryType categoryType);
}
