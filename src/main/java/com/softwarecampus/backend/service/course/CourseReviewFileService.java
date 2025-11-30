package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.dto.course.ReviewFileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CourseReviewFileService {

    ReviewFileResponse uploadReviewFile(Long courseId, Long reviewId, Long userId, MultipartFile file);

    void deleteReviewFile(Long courseId, Long reviewId, Long fileId, Long userId);

    void restoreReviewFile(Long courseId, Long reviewId, Long fileId, Long adminId);

    void hardDeleteReviewFile(Long courseId, Long reviewId, Long fileId, Long adminId);
}
