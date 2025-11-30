package com.softwarecampus.backend.dto.home;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import lombok.*;

import java.time.LocalDate;

/**
 * 메인페이지 - 과정 정보 DTO
 * 최소한의 정보만 포함하여 성능 최적화
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeCourseDTO {

    private Long id;
    private String name;
    private Long academyId;
    private String academyName;
    private Long categoryId;
    private String categoryName;
    private CategoryType categoryType;

    private LocalDate recruitStart;
    private LocalDate recruitEnd;
    private LocalDate courseStart;
    private LocalDate courseEnd;

    private Integer cost;
    private String location;

    private boolean isKdt;
    private boolean isNailbaeum;
    private boolean isOffline;

    private String imageUrl;

    /**
     * Entity → DTO 변환
     */
    public static HomeCourseDTO fromEntity(Course course) {
        return HomeCourseDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .academyId(course.getAcademy() != null ? course.getAcademy().getId() : null)
                .academyName(course.getAcademy() != null ? course.getAcademy().getName() : null)
                .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getCategoryName() : null)
                .categoryType(course.getCategory() != null ? course.getCategory().getCategoryType() : null)
                .recruitStart(course.getRecruitStart())
                .recruitEnd(course.getRecruitEnd())
                .courseStart(course.getCourseStart())
                .courseEnd(course.getCourseEnd())
                .cost(course.getCost())
                .location(course.getLocation())
                .isKdt(course.isKdt())
                .isNailbaeum(course.isNailbaeum())
                .isOffline(course.isOffline())
                .imageUrl(course.getImages().stream()
                        .filter(img -> img.isActive() && img.isThumbnail())
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse(null))
                .build();
    }
}
