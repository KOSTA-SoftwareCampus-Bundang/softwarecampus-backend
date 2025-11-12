package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(
        name = "review_section",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"review_id", "section_type"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSection extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * sectionType은 ReviewSectionType enum과 직접 매핑됨
     * 예: CONTENT, COURSEWARE, INSTRUCTOR, EQUIPMENT, OTHER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 20)
    private ReviewSectionType sectionType;

    /**
     * 평점 (1~5)
     * 단, sectionType.hasRating == false 인 경우 null 가능
     */
    @Min(1)
    @Max(5)
    @Column(nullable = true)
    private Integer point;

    /**
     * 섹션별 서술형 의견 (예: 기타의견 또는 부연설명)
     */
    @Column(columnDefinition = "TEXT")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private CourseReview review;
}
