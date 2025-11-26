package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.dto.course.ReviewFileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CourseReviewFileService {

    ReviewFileResponse uploadReviewFile(CategoryType type, Long courseId, Long reviewId, Long userId, MultipartFile file);

    void deleteReviewFile(CategoryType type, Long courseId, Long reviewId, Long fileId, Long userId);

    void restoreReviewFile(CategoryType type, Long courseId, Long reviewId, Long fileId, Long adminId);

    void hardDeleteReviewFile(CategoryType type, Long courseId, Long reviewId, Long fileId, Long adminId);
}
