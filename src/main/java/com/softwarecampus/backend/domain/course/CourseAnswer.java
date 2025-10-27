package com.softwarecampus.backend.domain.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 과정 질문 답변 엔티티
 */
@Entity
@Table(name = "course_answer")
public class CourseAnswer extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_question_id", nullable = false)
    private CourseQuestion courseQuestion;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus isApproved;

    private LocalDateTime approvedAt;

    protected CourseAnswer() {
    }

    // 연관관계 편의 메소드
    public void assignQuestion(CourseQuestion question) {
        this.courseQuestion = question;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public CourseQuestion getCourseQuestion() {
        return courseQuestion;
    }

    public String getText() {
        return text;
    }

    public ApprovalStatus getIsApproved() {
        return isApproved;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
}
