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
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "account", "answeredBy" })
    @Query("SELECT q FROM CourseQna q WHERE q.course.id = :courseId AND q.isDeleted = false AND (q.title LIKE %:keyword% OR q.questionText LIKE %:keyword%)")
    Page<CourseQna> searchByCourseIdAndKeyword(@Param("courseId") Long courseId, @Param("keyword") String keyword,
            Pageable pageable);

    // 페이징 지원 (검색어 없을 때)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "account", "answeredBy" })
    @Query("SELECT q FROM CourseQna q WHERE q.course.id = :courseId AND q.isDeleted = false")
    Page<CourseQna> findByCourseId(@Param("courseId") Long courseId, Pageable pageable);

    // 단건 상세 조회 (연관 엔티티 함께 로딩)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "account", "answeredBy" })
    java.util.Optional<CourseQna> findWithDetailsById(Long id);

}
