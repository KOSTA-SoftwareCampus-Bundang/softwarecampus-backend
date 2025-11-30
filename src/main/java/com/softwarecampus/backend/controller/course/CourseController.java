package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseDetailResponseDTO;
import com.softwarecampus.backend.dto.course.CourseRequestDTO;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;
import com.softwarecampus.backend.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;

    /** 과정 목록 조회 */
    @GetMapping("/{type}/course")
    public ResponseEntity<List<CourseResponseDTO>> getAllCourses(
            @PathVariable CategoryType type,
            @RequestParam(required = false) Boolean isOffline) {
        return ResponseEntity.ok(courseService.getAllCourses(type, isOffline));
    }

    @GetMapping("/{type}/course/search")
    public ResponseEntity<List<CourseResponseDTO>> searchCourses(
            @PathVariable CategoryType type,
            @RequestParam String keyword,
            @RequestParam(required = false) Boolean isOffline) {
        return ResponseEntity.ok(courseService.searchCourses(type, keyword, isOffline));
    }

    /** 과정 상세 조회 (신규 추가) */
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<CourseDetailResponseDTO> getCourseDetail(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    /** 관리자 - 과정 등록 승인 */
    @PostMapping("/courses/{courseId}/approve")
    public ResponseEntity<CourseResponseDTO> approveCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.approveCourse(courseId));
    }

    /** 과정 수정 */
    @PutMapping("/courses/{courseId}")
    public ResponseEntity<CourseResponseDTO> updateCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, dto));
    }

    /** 과정 삭제 */
    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    /** 기관유저 - 과정 등록 요청 */
    @PostMapping("/courses/request")
    public ResponseEntity<CourseResponseDTO> requestCourse(@RequestBody @Valid CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.requestCourseRegistration(dto));
    }
}
