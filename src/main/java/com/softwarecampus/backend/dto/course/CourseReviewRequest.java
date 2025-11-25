package com.softwarecampus.backend.dto.course;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewRequest {

    private String comment;                       // 자유 코멘트
    private List<ReviewSectionRequest> sections;  // 점수 섹션
}
