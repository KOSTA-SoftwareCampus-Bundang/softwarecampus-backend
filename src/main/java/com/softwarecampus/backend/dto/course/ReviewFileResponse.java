package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.CourseReviewFile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewFileResponse {

    private Long id;
    private Long reviewId;
    private String fileUrl;
    private Long uploaderId;

    public static ReviewFileResponse from(CourseReviewFile file) {
        return ReviewFileResponse.builder()
                .id(file.getId())
                .reviewId(file.getReviewId())
                .fileUrl(file.getFileUrl())
                .uploaderId(file.getUploader() != null ? file.getUploader().getId() : null)
                .build();
    }
}
