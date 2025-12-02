package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseCategory;
import lombok.*;

/**
 * 과정 카테고리 응답 DTO
 * 작성일: 2025-12-02
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCategoryDTO {
    
    private Long id;
    private String categoryName;
    private CategoryType categoryType;
    
    /**
     * Entity → DTO 변환
     */
    public static CourseCategoryDTO fromEntity(CourseCategory entity) {
        return CourseCategoryDTO.builder()
                .id(entity.getId())
                .categoryName(entity.getCategoryName())
                .categoryType(entity.getCategoryType())
                .build();
    }
}
