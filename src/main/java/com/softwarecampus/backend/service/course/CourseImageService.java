package com.softwarecampus.backend.service.course;

import org.springframework.web.multipart.MultipartFile;
import com.softwarecampus.backend.domain.course.CourseImage;

import java.util.List;

public interface CourseImageService {

    CourseImage uploadCourseImage(Long courseId, MultipartFile file, boolean isThumbnail);

    void deleteCourseImage(Long imageId);

    List<CourseImage> getCourseImages(Long courseId);
}
