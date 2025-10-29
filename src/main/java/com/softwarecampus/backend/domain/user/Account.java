package com.softwarecampus.backend.domain.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 계정 엔티티
 * - 일반 사용자, 기관, 관리자 모두 이 엔티티를 사용
 * - Soft Delete 지원 (BaseSoftDeleteSupportEntity 상속)
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "account",
    indexes = {
        @Index(name = "idx_account_email", columnList = "email"),
        @Index(name = "idx_account_type_approved", columnList = "account_type, account_approved")
    }
)
public class Account extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;
    
    private String nickname;
    
    @Column(nullable = false)
    private String password;
    
    private String address;
    
    private String affiliation;
    
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_approved", nullable = false)
    private ApprovalStatus accountApproved;
    
    // academy_id는 추후 Academy 엔티티 생성 시 추가 예정
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "academy_id")
    // private Academy academy;
}
