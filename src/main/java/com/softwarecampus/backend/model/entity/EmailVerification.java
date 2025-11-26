package com.softwarecampus.backend.model.entity;

import com.softwarecampus.backend.model.enums.VerificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 이메일 인증 엔티티
 * - 회원가입 및 비밀번호 재설정 시 이메일 인증 코드 관리
 */
@Entity
@Table(
    name = "email_verification",
    indexes = {
        @Index(name = "idx_email_type", columnList = "email, type"),
        @Index(name = "idx_expires_at", columnList = "expires_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 인증 대상 이메일
     */
    @Column(nullable = false, length = 100)
    private String email;
    
    /**
     * 6자리 인증 코드 (000000 ~ 999999)
     */
    @Column(nullable = false, length = 6)
    private String code;
    
    /**
     * 인증 타입 (SIGNUP, PASSWORD_RESET)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationType type;
    
    /**
     * 인증 완료 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
    
    /**
     * 인증 시도 횟수
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;
    
    /**
     * 계정 차단 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean blocked = false;
    
    /**
     * 차단 해제 시간
     */
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;
    
    /**
     * 코드 만료 시간 (생성 후 3분)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * 생성 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 인증 완료 시간
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    /**
     * 인증 시도 증가
     */
    public void incrementAttempts() {
        this.attempts++;
    }
    
    /**
     * 계정 차단 설정
     */
    public void block(int blockDurationMinutes) {
        this.blocked = true;
        this.blockedUntil = LocalDateTime.now().plusMinutes(blockDurationMinutes);
    }
    
    /**
     * 인증 완료 처리
     */
    public void markAsVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }
    
    /**
     * 차단 상태 확인 및 만료 시 자동 해제
     * 
     * @apiNote 이 메서드는 상태를 변경할 수 있습니다 (side effect 주의):
     *          - 차단 시간이 만료된 경우 blocked, blockedUntil, attempts 초기화
     *          - JPA dirty checking으로 인한 자동 DB 업데이트 발생 가능
     * @return 현재 차단 상태 (true: 차단 중, false: 차단 아님)
     */
    public boolean checkAndUnblockIfExpired() {
        if (!blocked) {
            return false;
        }
        
        if (blockedUntil != null && LocalDateTime.now().isAfter(blockedUntil)) {
            // 차단 시간이 지나면 자동 해제
            this.blocked = false;
            this.blockedUntil = null;
            this.attempts = 0;
            return false;
        }
        
        return true;
    }
    
    /**
     * 코드 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
