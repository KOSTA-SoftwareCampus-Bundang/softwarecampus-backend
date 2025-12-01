package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseCategoryDTO;
import com.softwarecampus.backend.dto.course.CourseDetailResponseDTO;
import com.softwarecampus.backend.dto.course.CourseRequestDTO;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;
import com.softwarecampus.backend.repository.course.CourseCategoryRepository;
import com.softwarecampus.backend.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.softwarecampus.backend.domain.course.CourseStatus;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final CourseCategoryRepository courseCategoryRepository;

    /**
     * 과정 카테고리 목록 조회
     * @param categoryType 카테고리 타입 필터 (옵션: EMPLOYEE/JOB_SEEKER)
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CourseCategoryDTO>> getCategories(
            @RequestParam(required = false) CategoryType categoryType) {
        List<CourseCategoryDTO> categories;
        if (categoryType != null) {
            categories = courseCategoryRepository.findByCategoryTypeAndDeletedAtIsNull(categoryType)
                    .stream()
                    .map(CourseCategoryDTO::fromEntity)
                    .toList();
        } else {
            categories = courseCategoryRepository.findAllByDeletedAtIsNull()
                    .stream()
                    .map(CourseCategoryDTO::fromEntity)
                    .toList();
        }
        return ResponseEntity.ok(categories);
    }

    /**
     * 과정 목록 조회 (다양한 필터 지원 + 페이지네이션)
     * 
     * @param categoryId   카테고리 ID (예: 33=백엔드 개발)
     * @param categoryType 카테고리 타입 (EMPLOYEE/JOB_SEEKER)
     * @param isOffline    온/오프라인 필터
     * @param keyword      검색 키워드
     * @param status       과정 상태 (RECRUITING, IN_PROGRESS, ENDED)
     * @param pageable     페이지 정보 (page, size, sort)
     */

    @GetMapping
    public ResponseEntity<Page<CourseResponseDTO>> getCourses(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) CategoryType categoryType,
            @RequestParam(required = false) Boolean isOffline,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CourseStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity
                .ok(courseService.getCourses(categoryId, categoryType, isOffline, keyword, status, pageable));
    }

    /** 과정 상세 조회 */
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponseDTO> getCourseDetail(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    /** 관리자 - 과정 등록 승인 */
    @PostMapping("/{courseId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponseDTO> approveCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.approveCourse(courseId));
    }

    /** 관리자 - 과정 등록 거부 */
    @PostMapping("/{courseId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponseDTO> rejectCourse(
            @PathVariable Long courseId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(courseService.rejectCourse(courseId, reason));
    }

    /** 과정 수정 */
    @PutMapping("/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTITUTION')")
    public ResponseEntity<CourseResponseDTO> updateCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, dto));
    }

    /** 과정 삭제 */
    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    /** 기관유저 - 과정 등록 요청 */
    @PostMapping("/request")
    @PreAuthorize("hasRole('INSTITUTION')")
    public ResponseEntity<CourseResponseDTO> requestCourse(@RequestBody @Valid CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.requestCourseRegistration(dto));
    }

    /** 관리자 - 과정 직접 등록 (즉시 승인) */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponseDTO> createCourse(@RequestBody @Valid CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.createCourseByAdmin(dto));
    }
}
