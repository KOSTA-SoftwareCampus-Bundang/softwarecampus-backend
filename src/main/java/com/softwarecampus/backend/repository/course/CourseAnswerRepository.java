package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 과정 답변 레포지토리
 */
@Repository
public interface CourseAnswerRepository extends JpaRepository<CourseAnswer, Long> {
}
