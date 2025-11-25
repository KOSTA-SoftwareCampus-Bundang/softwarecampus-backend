package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.CourseReviewAttachment;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewAttachmentResponse {
    private Long attachmentId;
    private String fileUrl;
    private String originalName;

    public static ReviewAttachmentResponse fromEntity(CourseReviewAttachment attachment) {
        return ReviewAttachmentResponse.builder()
                .attachmentId(attachment.getId())
                .originalName(attachment.getOriginalName())
                .fileUrl(attachment.getFileUrl())
                .build();
    }
}
