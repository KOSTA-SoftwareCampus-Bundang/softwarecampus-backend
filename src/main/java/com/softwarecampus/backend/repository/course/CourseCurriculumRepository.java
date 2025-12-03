package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseCurriculum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 과정 커리큘럼 Repository
 * 작성일: 2025-12-03
 */
@Repository
public interface CourseCurriculumRepository extends JpaRepository<CourseCurriculum, Long> {

    /**
     * 특정 과정의 모든 커리큘럼 조회 (챕터 번호순 정렬)
     */
    List<CourseCurriculum> findByCourseIdOrderByChapterNumberAsc(Long courseId);

    /**
     * 특정 과정의 모든 커리큘럼 삭제
     */
    void deleteByCourseId(Long courseId);
}
