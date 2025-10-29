package com.softwarecampus.backend.domain.course;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    private String title;

    private Integer section1Point;
    private String section1Text;
    private Integer section2Point;
    private String section2Text;
    private Integer section3Point;
    private String section3Text;
    private Integer section4Point;
    private String section4Text;
    private Integer section5Point;
    private String section5Text;
    private Integer section6Point;
    private String section6Text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus reviewApproved = ApprovalStatus.WAITING;

    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
