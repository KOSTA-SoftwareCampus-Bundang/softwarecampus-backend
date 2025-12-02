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
    private String courseName; // 추가: 과정 이름

    private String comment;
    private String approvalStatus; // APPROVED / PENDING / REJECTED
    private String rejectionReason; // 거부 사유

    private double averageScore;

    private List<ReviewSectionResponse> sections;
    private List<ReviewAttachmentResponse> attachments;

    private int likeCount;
    private int dislikeCount;

    @Builder.Default
    private String myLikeType = "NONE"; // 추가: 내 좋아요 상태 (LIKE, DISLIKE, NONE)

    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt; // 추가: 작성일
}
