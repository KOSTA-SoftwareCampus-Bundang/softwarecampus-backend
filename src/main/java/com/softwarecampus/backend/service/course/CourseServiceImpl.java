package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.dto.course.CourseCategoryDTO;
import com.softwarecampus.backend.dto.course.CourseDetailResponseDTO;
import com.softwarecampus.backend.dto.course.CourseRequestDTO;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;
import com.softwarecampus.backend.repository.academy.AcademyRepository;
import com.softwarecampus.backend.repository.course.CourseCategoryRepository;
import com.softwarecampus.backend.repository.course.CourseRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.domain.course.CourseStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 과정 서비스 구현체
 * 수정일: 2025-12-02 - Soft Delete 준수, @Transactional 추가, 등록자 정보 추가
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

        private final CourseRepository courseRepository;
        private final AcademyRepository academyRepository;
        private final CourseCategoryRepository courseCategoryRepository;
        private final AccountRepository accountRepository;

        /**
         * 과정 카테고리 목록 조회
         * 작성일: 2025-12-02 - 레이어 규칙 준수를 위해 서비스 계층으로 이동
         */
        @Override
        public List<CourseCategoryDTO> getCategories(CategoryType categoryType) {
                if (categoryType != null) {
                        return courseCategoryRepository.findByCategoryTypeAndDeletedAtIsNull(categoryType)
                                        .stream()
                                        .map(CourseCategoryDTO::fromEntity)
                                        .toList();
                }
                return courseCategoryRepository.findAllByDeletedAtIsNull()
                                .stream()
                                .map(CourseCategoryDTO::fromEntity)
                                .toList();
        }

        @Override
        public Page<CourseResponseDTO> getCourses(Long categoryId, CategoryType categoryType, Boolean isOffline,
                        String keyword, CourseStatus status, Pageable pageable) {
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

        /** 관리자 - 과정 승인 (APPROVED) */
        @Override
        @Transactional
        public CourseResponseDTO approveCourse(@NonNull Long courseId) {
                // Soft Delete 준수: findByIdAndDeletedAtIsNull 사용
                Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                                .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다. ID=" + courseId));

                course.setIsApproved(ApprovalStatus.APPROVED);
                return CourseResponseDTO.fromEntity(course);
        }

        /** 기관유저 - 과정 등록 요청 (PENDING) */
        @Override
        @Transactional
        public CourseResponseDTO requestCourseRegistration(CourseRequestDTO dto, Long requesterId) {
                var academy = academyRepository.findById(dto.getAcademyId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "존재하지 않는 기관입니다. ID=" + dto.getAcademyId()));

                var category = courseCategoryRepository
                                .findByCategoryTypeAndCategoryName(dto.getCategoryType(), dto.getCategoryName())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "존재하지 않는 과정 카테고리입니다. type=" + dto.getCategoryType() + ", name="
                                                                + dto.getCategoryName()));

                var requester = accountRepository.findById(requesterId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "존재하지 않는 사용자입니다. ID=" + requesterId));

                var course = dto.toEntity(academy, category);
                course.setIsApproved(ApprovalStatus.PENDING); // 기관 유저 요청 상태
                course.setRequester(requester); // 등록자 설정

                courseRepository.save(course);
                return CourseResponseDTO.fromEntity(course);
        }

        /** 관리자 - 과정 직접 등록 (즉시 APPROVED) */
        @Override
        @Transactional
        public CourseResponseDTO createCourseByAdmin(CourseRequestDTO dto, Long requesterId) {
                var academy = academyRepository.findById(dto.getAcademyId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "존재하지 않는 기관입니다. ID=" + dto.getAcademyId()));

                var category = courseCategoryRepository
                                .findByCategoryTypeAndCategoryName(dto.getCategoryType(), dto.getCategoryName())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "존재하지 않는 과정 카테고리입니다. type=" + dto.getCategoryType() + ", name="
                                                                + dto.getCategoryName()));

                var requester = accountRepository.findById(requesterId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "존재하지 않는 사용자입니다. ID=" + requesterId));

                var course = dto.toEntity(academy, category);
                course.setIsApproved(ApprovalStatus.APPROVED); // 관리자는 즉시 승인
                course.setRequester(requester); // 등록자 설정

                courseRepository.save(course);
                return CourseResponseDTO.fromEntity(course);
        }

        @Override
        @Transactional
        public CourseResponseDTO updateCourse(@NonNull Long courseId, CourseRequestDTO dto) {
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

                // 거부된 항목 수정 시 대기 상태로 변경 (재심사 요청)
                if (course.getIsApproved() == ApprovalStatus.REJECTED) {
                        course.setIsApproved(ApprovalStatus.PENDING);
                        course.setRejectionReason(null); // 거부 사유 초기화
                }

                return CourseResponseDTO.fromEntity(course);
        }

        @Override
        @Transactional
        public void deleteCourse(@NonNull Long courseId) {
                Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                                .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다."));
                course.markDeleted();
        }

        /**
         * 과정 상세 조회 (조회수 증가 포함)
         * 수정일: 2025-12-02 - @Transactional 추가 (viewCount 증가 반영을 위해)
         * 수정일: 2025-12-02 - curriculums 초기화 추가 (MultipleBagFetchException 방지)
         */
        @Override
        @Transactional
        public CourseDetailResponseDTO getCourseDetail(@NonNull Long courseId) {
                Course course = courseRepository.findWithDetailsByIdAndDeletedAtIsNull(courseId)
                                .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다. ID=" + courseId));

                // 조회수 증가
                course.incrementViewCount();

                // curriculums 초기화 (Lazy Loading - MultipleBagFetchException 방지)
                org.hibernate.Hibernate.initialize(course.getCurriculums());

                return CourseDetailResponseDTO.fromEntity(course);
        }

        @Override
        public Page<CourseResponseDTO> getAdminCourses(ApprovalStatus status, String keyword, Pageable pageable) {
                Page<Course> coursePage = courseRepository.searchAdminCourses(status, keyword, pageable);
                return coursePage.map(CourseResponseDTO::fromEntity);
        }

        /**
         * 관리자 - 과정 승인 거부
         * 수정일: 2025-12-02 - Soft Delete 준수, 거부 사유 저장
         */
        @Override
        @Transactional
        public CourseResponseDTO rejectCourse(@NonNull Long courseId, String reason) {
                // Soft Delete 준수: findByIdAndDeletedAtIsNull 사용
                Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                                .orElseThrow(() -> new EntityNotFoundException("해당 과정이 존재하지 않습니다. ID=" + courseId));

                course.reject(reason); // 거부 상태 변경 및 사유 저장

                return CourseResponseDTO.fromEntity(course);
        }

        @Override
        public Page<CourseResponseDTO> getInstitutionCourses(@NonNull Long academyId, ApprovalStatus status,
                        String keyword,
                        Pageable pageable) {
                Page<Course> coursePage = courseRepository.searchInstitutionCourses(academyId, status, keyword,
                                pageable);
                return coursePage.map(CourseResponseDTO::fromEntity);
        }
}
