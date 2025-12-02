package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseImageType;
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

    /**
     * 이미지 업로드
     * @param type 카테고리 타입 (EMPLOYEE/JOB_SEEKER)
     * @param courseId 과정 ID
     * @param file 업로드할 파일
     * @param imageType 이미지 타입 (THUMBNAIL, HEADER, CONTENT) - 기본값: THUMBNAIL
     * @param isThumbnail (deprecated) 하위 호환을 위한 파라미터, imageType 사용 권장
     */
    @PostMapping("/{courseId}/images")
    public ResponseEntity<CourseImageResponse> uploadCourseImage(
            @PathVariable("type") CategoryType type,
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "imageType", required = false) CourseImageType imageType,
            @RequestParam(value = "isThumbnail", defaultValue = "false") boolean isThumbnail
    ) {
        // imageType이 명시되지 않은 경우 isThumbnail 파라미터로 결정 (하위 호환)
        CourseImageType finalImageType = imageType;
        if (finalImageType == null) {
            finalImageType = isThumbnail ? CourseImageType.THUMBNAIL : CourseImageType.CONTENT;
        }
        
        CourseImageResponse saved = courseImageService.uploadCourseImage(type, courseId, file, finalImageType);
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
