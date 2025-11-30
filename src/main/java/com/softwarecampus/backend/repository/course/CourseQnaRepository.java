package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseQna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseQnaRepository extends JpaRepository<CourseQna, Long> {

    // 검색 및 페이징 지원
    @Query("SELECT q FROM CourseQna q WHERE q.course.id = :courseId AND (q.title LIKE %:keyword% OR q.questionText LIKE %:keyword%)")
    Page<CourseQna> searchByCourseIdAndKeyword(@Param("courseId") Long courseId, @Param("keyword") String keyword,
            Pageable pageable);

    // 페이징 지원 (검색어 없을 때)
    Page<CourseQna> findByCourseId(Long courseId, Pageable pageable);

}
