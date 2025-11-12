package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
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
@RequestMapping("/api/{type}/course")
public class CourseController {

    private final CourseService courseService;

    /** 과정 목록 조회 */
    @GetMapping
    public ResponseEntity<List<CourseResponseDTO>> getAllCourses(@PathVariable CategoryType type) {
        return ResponseEntity.ok(courseService.getAllCourses(type));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CourseResponseDTO>> searchCourses(
            @PathVariable CategoryType type,
            @RequestParam String keyword) {
        return ResponseEntity.ok(courseService.searchCourses(type, keyword));
    }


    /** 관리자 - 과정 등록 승인 */
    @PostMapping("/{courseId}/approve")
    public ResponseEntity<CourseResponseDTO> approveCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.approveCourse(courseId));
    }

    /** 과정 수정 */
    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponseDTO> updateCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, dto));
    }

    /** 과정 삭제 */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    /** 기관유저 - 과정 등록 요청 */
    @PostMapping("/request")
    public ResponseEntity<CourseResponseDTO> requestCourse(@RequestBody @Valid CourseRequestDTO dto) {
        return ResponseEntity.ok(courseService.requestCourseRegistration(dto));
    }
}
