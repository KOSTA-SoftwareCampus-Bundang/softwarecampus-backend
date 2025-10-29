package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseQuestion extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus isApproved = ApprovalStatus.WAITING;

    private LocalDateTime approvedAt;

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL)
    private CourseAnswer answer;
}
