package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSection extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 리뷰에 속하나
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private CourseReview review;

    // CURRICULUM / COURSEWARE / INSTRUCTOR / EQUIPMENT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewSectionType sectionType;

    // 1~5점
    @Column(nullable = false)
    private int score;

    // 각 항목 코멘트 (optional)
    @Column(columnDefinition = "TEXT")
    private String comment;
}
