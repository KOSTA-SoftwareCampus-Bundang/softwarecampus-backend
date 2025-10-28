package com.softwarecampus.backend.domain.academy;

import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademyQuestion extends BaseSoftDeleteSupportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question_number;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_approved", nullable = false)
    private ApprovalStatus isApproved;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ✅ 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @OneToMany(mappedBy = "academyQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AcademyAnswer> answers = new ArrayList<>();

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
