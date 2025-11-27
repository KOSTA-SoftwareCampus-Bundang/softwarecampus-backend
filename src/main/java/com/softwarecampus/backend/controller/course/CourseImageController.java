package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseImageResponse;
import com.softwarecampus.backend.service.course.CourseImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{type}/courses")
public class CourseImageController {

    private final CourseImageService courseImageService;

    // 이미지 업로드
    @PostMapping("/{courseId}/images")
    public ResponseEntity<CourseImageResponse> uploadCourseImage(
            @PathVariable("type") CategoryType type,
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isThumbnail", defaultValue = "false") boolean isThumbnail
    ) {
        CourseImageResponse saved = courseImageService.uploadCourseImage(type, courseId, file, isThumbnail);
        return ResponseEntity.ok(saved);
    }

    // 이미지 조회
    @GetMapping("/{courseId}/images")
    public ResponseEntity<List<CourseImageResponse>> getCourseImages(
            @PathVariable("type") CategoryType type,
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(courseImageService.getCourseImages(type, courseId));
    }

    // Soft Delete
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteCourseImage(
            @PathVariable("type") CategoryType type,
            @PathVariable Long imageId
    ) {
        courseImageService.deleteCourseImage(type, imageId);
        return ResponseEntity.noContent().build();
    }

    // Hard Delete
    @DeleteMapping("/images/{imageId}/hard")
    public ResponseEntity<Void> hardDeleteCourseImage(
            @PathVariable("type") CategoryType type,
            @PathVariable Long imageId
    ) {
        courseImageService.hardDeleteCourseImage(type, imageId);
        return ResponseEntity.noContent().build();
    }

}
