package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 과정 질문 레포지토리
 */
@Repository
public interface CourseQuestionRepository extends JpaRepository<CourseQuestion, Long> {
    
    // 과정별 질문 목록
    List<CourseQuestion> findByCourseAndIsDeletedFalseOrderByCreatedAtDesc(Course course);
}
