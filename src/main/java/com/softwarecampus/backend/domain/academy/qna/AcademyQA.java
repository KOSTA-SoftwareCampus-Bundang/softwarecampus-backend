package com.softwarecampus.backend.domain.academy.qna;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "academy_qna")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademyQA extends BaseSoftDeleteSupportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "answer_text", columnDefinition = "TEXT", nullable = true)
    private String answerText;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_approved", nullable = false)
    private ApprovalStatus isApproved =  ApprovalStatus.PENDING;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ✅ 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    // 답변 등록/수정
    public void updateAnswer(String answerText) {
        this.answerText = answerText;
    }

    // 답변 삭제
    public void deleteAnswer() {
        this.answerText = null;
    }

    // ✅ 승인 처리
    public void approve() {
        this.isApproved = ApprovalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject() {
        this.isApproved = ApprovalStatus.REJECTED;
        this.approvedAt = null;
    }




}
