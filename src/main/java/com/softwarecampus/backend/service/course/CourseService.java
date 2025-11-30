package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseRequestDTO;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;

import java.util.List;

public interface CourseService {

    List<CourseResponseDTO> getAllCourses(CategoryType type, Boolean isOffline);

    List<CourseResponseDTO> searchCourses(CategoryType type, String keyword, Boolean isOffline);

    /** 관리자 - 요청 승인 후 등록 */
    CourseResponseDTO approveCourse(Long courseId);

    CourseResponseDTO updateCourse(Long courseId, CourseRequestDTO dto);

    void deleteCourse(Long courseId);

    /** 기관유저 - 과정 등록 요청 (PENDING) */
    CourseResponseDTO requestCourseRegistration(CourseRequestDTO dto);

    /** 과정 상세 조회 (커리큘럼 포함) */
    com.softwarecampus.backend.dto.course.CourseDetailResponseDTO getCourseDetail(Long courseId);
}
