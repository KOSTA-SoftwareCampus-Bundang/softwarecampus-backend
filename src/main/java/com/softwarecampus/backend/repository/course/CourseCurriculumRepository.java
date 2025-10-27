package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseCurriculum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 과정 커리큘럼 레포지토리
 */
@Repository
public interface CourseCurriculumRepository extends JpaRepository<CourseCurriculum, Long> {
}
