package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 과정 카테고리 레포지토리
 */
@Repository
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
}
