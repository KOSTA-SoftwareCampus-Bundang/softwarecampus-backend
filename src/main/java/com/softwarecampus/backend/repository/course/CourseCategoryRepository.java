package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {

    @Modifying
    @Query(value = "INSERT INTO course_category (id, category_name, category_type, is_deleted) VALUES (:id, :name, :type, false)", nativeQuery = true)
    void saveWithId(@Param("id") Long id, @Param("name") String name, @Param("type") String type);

    Optional<CourseCategory> findByCategoryTypeAndCategoryName(CategoryType categoryType, String categoryName);

}
