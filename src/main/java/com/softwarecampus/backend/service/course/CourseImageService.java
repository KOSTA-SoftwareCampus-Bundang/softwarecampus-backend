package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseImageType;
import com.softwarecampus.backend.dto.course.CourseImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseImageService {

    /**
     * 과정 이미지 업로드
     * @param type 카테고리 타입
     * @param courseId 과정 ID
     * @param file 업로드할 파일
     * @param imageType 이미지 타입 (THUMBNAIL, HEADER, CONTENT)
     * @return 업로드된 이미지 정보
     */
    CourseImageResponse uploadCourseImage(CategoryType type, Long courseId, MultipartFile file, CourseImageType imageType);

    void deleteCourseImage(CategoryType type, Long imageId);

    List<CourseImageResponse> getCourseImages(CategoryType type, Long courseId);

    void hardDeleteCourseImage(CategoryType type, Long imageId);
}
