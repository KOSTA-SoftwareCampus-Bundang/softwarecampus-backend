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
@Table(name = "account")
public class Account extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ===== 기존 필드 (유지) =====
    @Column(unique = true)
    private String userName;              // 사용자명
    
    @Column(nullable = false)
    private String password;              // 암호화된 비밀번호
    
    @Column(nullable = false, unique = true)
    private String email;                 // 이메일 (로그인 ID)
    
    @Column(nullable = false, unique = true)
    private String phoneNumber;           // 전화번호
    
    // ===== 수정된 필드 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;      // 계정 유형 (USER/ACADEMY/ADMIN)
    
    private String affiliation;           // 소속 (회사/학교 등)
    
    private String position;              // 직책/역할
    
    // ===== 새로 추가된 필드 =====
    private String address;               // 주소
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_approved")
    private ApprovalStatus accountApproved;  // 승인 상태 (기관 계정용)
    
    // ===== 향후 추가 예정 (다른 도메인 작업 후) =====
    // academy_id는 추후 Academy 엔티티 생성 시 추가 예정
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "academy_id")
    // private Academy academy;
}
