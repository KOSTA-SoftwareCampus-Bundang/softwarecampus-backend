package com.softwarecampus.backend.dto.course;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewResponse {

    private Long reviewId;
    private Long writerId;
    private Long courseId;

    private String comment;
    private String approvalStatus; // APPROVED / PENDING / REJECTED

    private double averageScore;

    private List<ReviewSectionResponse> sections;
    private List<ReviewAttachmentResponse> attachments;

    private int likeCount;
    private int dislikeCount;
}
