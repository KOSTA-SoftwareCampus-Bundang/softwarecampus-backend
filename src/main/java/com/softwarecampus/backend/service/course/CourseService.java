package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseRequestDTO;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;

import java.util.List;

public interface CourseService {

    List<CourseResponseDTO> getAllCourses(CategoryType type);

    List<CourseResponseDTO> searchCourses(CategoryType type, String keyword);

    /** 관리자 - 요청 승인 후 등록 */
    CourseResponseDTO approveCourse(Long courseId);

    CourseResponseDTO updateCourse(Long courseId, CourseRequestDTO dto);

    void deleteCourse(Long courseId);

    /** 기관유저 - 과정 등록 요청 (PENDING) */
    CourseResponseDTO requestCourseRegistration(CourseRequestDTO dto);
}
