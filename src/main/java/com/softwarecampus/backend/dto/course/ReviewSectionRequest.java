package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.ReviewSectionType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSectionRequest {
    private String sectionType;  // CURRICULUM, COURSEWARE, INSTRUCTOR, EQUIPMENT
    private int score;           // 1~5
    private String comment;      // optional
}

