package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseStatus;
import com.softwarecampus.backend.dto.course.CourseRequestDTO;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseService {

        /**
         * 과정 목록 조회 (페이지네이션 지원)
         * 
         * @param categoryId   카테고리 ID (옵션)
         * @param categoryType 카테고리 타입 EMPLOYEE/JOB_SEEKER (옵션)
         * @param isOffline    온/오프라인 필터 (옵션)
         * @param keyword      검색 키워드 (옵션)
         * @param pageable     페이지 정보
         */
        Page<CourseResponseDTO> getCourses(Long categoryId, CategoryType categoryType, Boolean isOffline,
                        String keyword,
                        CourseStatus status, Pageable pageable);

        /**
         * 과정 목록 조회 (전체)
         * 
         * @deprecated 페이지네이션 버전 사용 권장
         */
        @Deprecated
        List<CourseResponseDTO> getCourses(Long categoryId, CategoryType categoryType, Boolean isOffline,
                        String keyword);

        /** 관리자 - 요청 승인 후 등록 */
        CourseResponseDTO approveCourse(Long courseId);

        CourseResponseDTO updateCourse(Long courseId, CourseRequestDTO dto);

        void deleteCourse(Long courseId);

        /** 기관유저 - 과정 등록 요청 (PENDING) */
        CourseResponseDTO requestCourseRegistration(CourseRequestDTO dto);

        /** 관리자 - 과정 직접 등록 (즉시 APPROVED) */
        CourseResponseDTO createCourseByAdmin(CourseRequestDTO dto);

        /** 과정 상세 조회 (커리큘럼 포함) */
        com.softwarecampus.backend.dto.course.CourseDetailResponseDTO getCourseDetail(Long courseId);

        /** 관리자 - 과정 목록 조회 (승인 상태별) */
        Page<CourseResponseDTO> getAdminCourses(com.softwarecampus.backend.domain.common.ApprovalStatus status,
                        String keyword, Pageable pageable);

        /** 관리자 - 과정 승인 거부 */
        CourseResponseDTO rejectCourse(Long courseId, String reason);

        /** 기관 - 과정 목록 조회 (상태별) */
        Page<CourseResponseDTO> getInstitutionCourses(Long academyId,
                        com.softwarecampus.backend.domain.common.ApprovalStatus status,
                        String keyword, Pageable pageable);
}
