
package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.dto.course.CourseDetailResponseDTO;
import com.softwarecampus.backend.dto.course.CourseRequestDTO;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.course.CourseCategoryRepository;
import com.softwarecampus.backend.repository.course.CourseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

        private final CourseRepository courseRepository;
        private final AcademyRepository academyRepository;
        private final CourseCategoryRepository courseCategoryRepository;

        @Override
        public Page<CourseResponseDTO> getCourses(Long categoryId, CategoryType categoryType, Boolean isOffline,
                        String keyword, String status, Pageable pageable) {
                // 통합 검색 쿼리 사용 (모든 파라미터가 null 가능)
                Page<Course> coursePage = courseRepository.searchCourses(categoryId, categoryType, isOffline, keyword,
                                status,
                                pageable);
                return coursePage.map(CourseResponseDTO::fromEntity);
        }

        /**
         * @deprecated This method fetches all courses without pagination, which can
         *             cause performance issues.
         *             Use
         *             {@link #getCourses(Long, CategoryType, Boolean, String, Pageable)}
         *             instead.
         */
        @Override
        @Deprecated
        public List<CourseResponseDTO> getCourses(Long categoryId, CategoryType categoryType, Boolean isOffline,
                        String keyword) {
                List<Course> courses = courseRepository.searchCoursesAll(categoryId, categoryType, isOffline, keyword);
                return courses.stream()
                                .map(CourseResponseDTO::fromEntity)
                                .toList();
        }

        /** 관리자 - 요청 승인 후 등록 */
        @Override
        @Transactional
        public CourseResponseDTO approveCourse(Long courseId) {
                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다. ID=" + courseId));

                course.setIsApproved(ApprovalStatus.APPROVED);
                return CourseResponseDTO.fromEntity(course);
        }

        /** 기관유저 - 과정 등록 요청 (PENDING) */
        @Override
        @Transactional
        public CourseResponseDTO requestCourseRegistration(CourseRequestDTO dto) {
                var academy = academyRepository.findById(dto.getAcademyId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "존재하지 않는 기관입니다. ID=" + dto.getAcademyId()));

                var category = courseCategoryRepository
                                .findByCategoryTypeAndCategoryName(dto.getCategoryType(), dto.getCategoryName())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "존재하지 않는 과정 카테고리입니다. type=" + dto.getCategoryType() + ", name="
                                                                + dto.getCategoryName()));

                var course = dto.toEntity(academy, category);
                course.setIsApproved(ApprovalStatus.PENDING); // 기관 유저 요청 상태

                courseRepository.save(course);
                return CourseResponseDTO.fromEntity(course);
        }

        @Override
        @Transactional
        public CourseResponseDTO updateCourse(Long courseId, CourseRequestDTO dto) {
                Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                                .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다."));

                course.setName(dto.getName());
                course.setCost(dto.getCost());
                course.setLocation(dto.getLocation());
                course.setClassDay(dto.getClassDay());
                course.setRecruitStart(dto.getRecruitStart());
                course.setRecruitEnd(dto.getRecruitEnd());
                course.setCourseStart(dto.getCourseStart());
                course.setCourseEnd(dto.getCourseEnd());
                course.setRequirement(dto.getRequirement());

                return CourseResponseDTO.fromEntity(course);
        }

        @Override
        @Transactional
        public void deleteCourse(Long courseId) {
                Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                                .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다."));
                course.markDeleted();
        }

        @Override
        public CourseDetailResponseDTO getCourseDetail(Long courseId) {
                Course course = courseRepository.findWithDetailsByIdAndDeletedAtIsNull(courseId)
                                .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다. ID=" + courseId));
                return CourseDetailResponseDTO.fromEntity(course);
        }
}
