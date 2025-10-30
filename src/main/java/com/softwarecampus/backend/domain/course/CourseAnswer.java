package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_answer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAnswer extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private CourseQuestion question;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus isApproved = ApprovalStatus.WAITING;

    private LocalDateTime approvedAt;

}
