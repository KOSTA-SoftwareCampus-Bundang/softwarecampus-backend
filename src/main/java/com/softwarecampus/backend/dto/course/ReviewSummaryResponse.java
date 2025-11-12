package com.softwarecampus.backend.dto.course;

import lombok.*;

import java.util.Map;

/**
 * 과정별 리뷰 통계 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummaryResponse {

    private double totalAverage;
    private Map<String, Double> sectionAverages; // "CONTENT" → 4.2, "INSTRUCTOR" → 4.8
    private long reviewCount;
}
