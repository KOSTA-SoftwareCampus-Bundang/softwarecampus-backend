package com.softwarecampus.backend.dto.course;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 리뷰 상세/목록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;
    private String title;
    private String authorName;
    private List<ReviewSectionResponse> sections;
    private double averageScore;
    private int likeCount;
    private int dislikeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
