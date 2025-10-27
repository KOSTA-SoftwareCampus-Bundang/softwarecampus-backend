package com.softwarecampus.backend.domain.user;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.common.BaseSoftDeleteSupportEntity;
import jakarta.persistence.*;

/**
 * 계정 엔티티
 */
@Entity
@Table(name = "account",
    indexes = {
        @Index(name = "idx_account_email", columnList = "email"),
        @Index(name = "idx_account_type_approved", columnList = "accountType,accountApproved")
    }
)
public class Account extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    private String nickname;

    @Column(nullable = false)
    private String password;

    private String address;
    private String affiliation;
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus accountApproved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id")
    private Academy academy;

    protected Account() {
    }

    // 연관관계 편의 메소드
    public void assignAcademy(Academy academy) {
        this.academy = academy;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getPosition() {
        return position;
    }

    public ApprovalStatus getAccountApproved() {
        return accountApproved;
    }

    public Academy getAcademy() {
        return academy;
    }
}