package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseImageRepository extends JpaRepository<CourseImage, Long> {

    List<CourseImage> findByCourseIdAndIsDeletedFalse(Long courseId);

    List<CourseImage> findByCourseId(Long courseId);
}
