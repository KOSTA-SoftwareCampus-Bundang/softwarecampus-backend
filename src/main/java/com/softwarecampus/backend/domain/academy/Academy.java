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
public class Academy extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String businessNumber;
    private String email;

    // 승인 여부 (PENDING / APPROVED / REJECTED)
    @Enumerated(EnumType.STRING)
    @Column(name = "is_approved")
    private ApprovalStatus isApproved;

    // 승인 일시
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // 승인 처리 메서드
    public void approve() {
        this.isApproved = ApprovalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }
    // 승인 거절 처리 메서드
    public void reject() {
        this.isApproved = ApprovalStatus.REJECTED;
        this.approvedAt = null;
    }
}
