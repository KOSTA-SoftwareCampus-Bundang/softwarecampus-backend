package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.dto.course.CourseRejectRequest;
import com.softwarecampus.backend.dto.course.CourseResponseDTO;
import com.softwarecampus.backend.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/courses")
@RequiredArgsConstructor
public class CourseAdminController {

    private final CourseService courseService;

    /**
     * 과정 승인 요청 목록 조회
     */
    @GetMapping("/approval-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CourseResponseDTO>> getApprovalRequests(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(courseService.getAdminCourses(status, keyword, pageable));
    }

    /**
     * 과정 승인 거부
     */
    @PostMapping("/{courseId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponseDTO> rejectCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid CourseRejectRequest request) {
        return ResponseEntity.ok(courseService.rejectCourse(courseId, request.getReason()));
    }
}
