package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.ReviewSectionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 리뷰의 각 항목(섹션) 점수 및 의견 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSectionRequest {

    @NotNull
    private ReviewSectionType sectionType;

    @Min(1)
    @Max(5)
    private Integer point;

    private String text;
}
