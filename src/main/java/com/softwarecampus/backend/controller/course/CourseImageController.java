package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CourseImage;
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
    public ResponseEntity<CourseImage> uploadCourseImage(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isThumbnail", defaultValue = "false") boolean isThumbnail
    ) {
        CourseImage saved = courseImageService.uploadCourseImage(courseId, file, isThumbnail);
        return ResponseEntity.ok(saved);
    }

    // 이미지 조회
    @GetMapping("/{courseId}/images")
    public ResponseEntity<List<CourseImage>> getCourseImages(
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(courseImageService.getCourseImages(courseId));
    }

    // Soft Delete
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteCourseImage(
            @PathVariable Long imageId
    ) {
        courseImageService.deleteCourseImage(imageId);
        return ResponseEntity.noContent().build();
    }
}
