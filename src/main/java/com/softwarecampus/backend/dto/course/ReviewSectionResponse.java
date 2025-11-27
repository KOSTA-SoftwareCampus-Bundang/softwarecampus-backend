package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.ReviewSection;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSectionResponse {
    private String sectionType;
    private int score;
    private String comment;

    public static ReviewSectionResponse fromEntity(ReviewSection section) {
        return ReviewSectionResponse.builder()
                .sectionType(section.getSectionType().name())
                .score(section.getScore())
                .build();
    }
}
