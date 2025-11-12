package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
    Optional<CourseCategory> findByCategoryTypeAndCategoryName(CategoryType categoryType, String categoryName);

}
