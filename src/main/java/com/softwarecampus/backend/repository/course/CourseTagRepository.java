package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 과정 태그 레포지토리
 */
@Repository
public interface CourseTagRepository extends JpaRepository<CourseTag, Long> {
}
