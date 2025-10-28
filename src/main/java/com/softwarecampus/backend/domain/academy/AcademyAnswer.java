package com.softwarecampus.backend.domain.academy;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademyAnswer extends BaseSoftDeleteSupportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_approved")
    private ApprovalStatus isApproved;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    //관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_question_id", nullable = false)
    private AcademyQuestion academyQuestion;

    public void approve() {
        this.isApproved = ApprovalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }
    public void reject() {
        this.isApproved = ApprovalStatus.REJECTED;
        this.approvedAt = null;
    }

}
