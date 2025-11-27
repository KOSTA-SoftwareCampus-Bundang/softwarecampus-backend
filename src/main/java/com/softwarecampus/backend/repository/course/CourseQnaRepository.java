package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseQna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseQnaRepository extends JpaRepository<CourseQna, Long> {

    List<CourseQna> findByCourseAndIsDeletedFalseOrderByIdDesc(Course course);


}
