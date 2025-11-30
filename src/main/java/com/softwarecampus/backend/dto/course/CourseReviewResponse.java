package com.softwarecampus.backend.dto.course;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewResponse {

    private Long reviewId;
    private Long writerId;
    private String writerName; // 추가: 작성자 이름
    private Long courseId;

    private String comment;
    private String approvalStatus; // APPROVED / PENDING / REJECTED

    private double averageScore;

    private List<ReviewSectionResponse> sections;
    private List<ReviewAttachmentResponse> attachments;

    private int likeCount;
    private int dislikeCount;
    
    private LocalDateTime createdAt; // 추가: 작성일
}
