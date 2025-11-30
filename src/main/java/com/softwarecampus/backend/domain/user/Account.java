package com.softwarecampus.backend.domain.user;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.Comment;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 계정 엔티티
 * - 일반 사용자, 기관, 관리자 모두 이 엔티티를 사용
 * - Soft Delete 지원 (BaseSoftDeleteSupportEntity 상속)
 * - Redis 캐싱 지원을 위해 Serializable 구현
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "account", indexes = {
        @Index(name = "uk_account_email", columnList = "email", unique = true),
        @Index(name = "uk_account_phone", columnList = "phoneNumber", unique = true),
        @Index(name = "idx_account_username", columnList = "userName"),
        @Index(name = "idx_account_type_approved", columnList = "account_type,account_approved")
})
public class Account extends BaseSoftDeleteSupportEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 기존 필드 (유지) =====
    @Column
    private String userName; // 사용자명 (Soft Delete 고려: unique 제약 없음, 비즈니스 로직에서 활성 계정만 중복 체크)

    @Column(nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(nullable = false)
    private String email; // 이메일 (로그인 ID, unique 제약은 @Index에서 관리)

    @Column(nullable = false)
    private String phoneNumber; // 전화번호 (unique 제약은 @Index에서 관리)

    // ===== 수정된 필드 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType; // 계정 유형 (USER/ACADEMY/ADMIN)

    private String affiliation; // 소속 (회사/학교 등)

    private String position; // 직책/역할

    // ===== 새로 추가된 필드 =====
    @Column(name = "address", length = 255)
    private String address; // 주소

    @Enumerated(EnumType.STRING)
    @Column(name = "account_approved")
    private ApprovalStatus accountApproved; // 승인 상태 (기관 계정용)

    @Column(name = "academy_id")
    private Long academyId; // 기관 ID (ACADEMY 타입일 때 설정, 향후 FK로 변경 예정)

    @Column(name = "profile_image", length = 1000)
    private String profileImage; // 프로필 이미지 URL (S3)

    // ===== 약관 동의 필드 =====
    @Column(name = "terms_agreed", nullable = false)
    private boolean termsAgreed; // 이용약관 동의 여부

    @Column(name = "terms_agreed_at")
    private java.time.LocalDateTime termsAgreedAt; // 이용약관 동의 일시

    @Column(name = "privacy_agreed", nullable = false)
    private boolean privacyAgreed; // 개인정보 처리방침 동의 여부

    @Column(name = "privacy_agreed_at")
    private java.time.LocalDateTime privacyAgreedAt; // 개인정보 처리방침 동의 일시

    @Column(name = "marketing_agreed", nullable = false)
    private boolean marketingAgreed; // 마케팅 수신 동의 여부

    @Column(name = "marketing_agreed_at")
    private java.time.LocalDateTime marketingAgreedAt; // 마케팅 수신 동의 일시

    // ===== 향후 추가 예정 (다른 도메인 작업 후) =====
    // academy_id는 추후 Academy 엔티티 생성 시 @ManyToOne으로 변경 예정
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "academy_id")
    // private Academy academy;

    // Board 엔티티와 관계 매핑을 위해 추가
    @OneToMany(mappedBy = "account")
    @Builder.Default
    private List<Board> boards = new ArrayList<>();

    // Comment 엔티티와 관계 매핑을 위해 추가
    @OneToMany(mappedBy = "account")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}
