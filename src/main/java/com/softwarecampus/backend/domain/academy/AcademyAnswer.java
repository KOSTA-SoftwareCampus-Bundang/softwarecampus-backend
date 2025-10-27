package com.softwarecampus.backend.domain.academy;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 기관 질문 답변 엔티티
 */
@Entity
@Table(name = "academy_answer")
public class AcademyAnswer extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_question_id", nullable = false)
    private AcademyQuestion academyQuestion;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus isApproved;

    private LocalDateTime approvedAt;

    protected AcademyAnswer() {
    }

    // 연관관계 편의 메소드
    public void assignQuestion(AcademyQuestion question) {
        this.academyQuestion = question;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public AcademyQuestion getAcademyQuestion() {
        return academyQuestion;
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
