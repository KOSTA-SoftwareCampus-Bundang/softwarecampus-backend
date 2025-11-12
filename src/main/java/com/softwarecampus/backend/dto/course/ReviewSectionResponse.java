package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.ReviewSectionType;
import lombok.*;

/**
 * 리뷰의 각 섹션별 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSectionResponse {

    private ReviewSectionType sectionType;
    private Integer point;
    private String text;
}
