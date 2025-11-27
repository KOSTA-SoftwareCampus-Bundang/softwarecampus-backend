package com.softwarecampus.backend.dto.course;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReviewListResponse {

    private List<CourseReviewResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
