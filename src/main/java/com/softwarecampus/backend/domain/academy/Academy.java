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
    
    // 거절 사유 (작성자: GitHub Copilot, 작성일: 2025-11-28)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    
    /**
     * 기관 첨부파일 목록 (사업자등록증, 교육기관 인증서 등)
     * CascadeType.ALL: Academy 삭제 시 파일도 함께 삭제
     * orphanRemoval: 관계가 끊어진 파일 자동 삭제
     */
    @OneToMany(mappedBy = "academy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AcademyFile> files = new ArrayList<>();

    // 승인 처리 메서드
    public void approve() {
        this.isApproved = ApprovalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = null;  // 승인 시 거절 사유 초기화
    }
    
    // 승인 거절 처리 메서드 (작성자: GitHub Copilot, 작성일: 2025-11-28)
    public void reject(String reason) {
        this.isApproved = ApprovalStatus.REJECTED;
        this.approvedAt = null;
        this.rejectionReason = reason;
    }
}
