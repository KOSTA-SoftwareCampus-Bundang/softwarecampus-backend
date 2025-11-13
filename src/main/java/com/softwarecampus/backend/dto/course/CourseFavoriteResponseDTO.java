package com.softwarecampus.backend.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 과정 찜 관련 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseFavoriteResponseDTO {
    private Long courseId;
    private String courseName;
    private boolean isFavorite;

    public CourseFavoriteResponseDTO(Long courseId, boolean isFavorite) {
        this.courseId = courseId;
        this.isFavorite = isFavorite;
    }
}
