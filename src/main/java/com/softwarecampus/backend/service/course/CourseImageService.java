package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.CourseImageResponse;
import org.springframework.web.multipart.MultipartFile;
import com.softwarecampus.backend.domain.course.CourseImage;

import java.util.List;

public interface CourseImageService {

    CourseImageResponse uploadCourseImage(CategoryType type, Long courseId, MultipartFile file, boolean isThumbnail);

    void deleteCourseImage(CategoryType type, Long imageId);

    List<CourseImageResponse> getCourseImages(CategoryType type, Long courseId);

    void hardDeleteCourseImage(CategoryType type, Long imageId);
}
