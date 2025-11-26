package com.softwarecampus.backend.repository.user;

import com.softwarecampus.backend.domain.EmailVerification;
import com.softwarecampus.backend.domain.common.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 이메일 인증 Repository
 */
@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    
    /**
     * 이메일과 타입으로 가장 최근 인증 레코드 조회
     * (생성 시간 기준 내림차순)
     */
    Optional<EmailVerification> findTopByEmailAndTypeOrderByCreatedAtDesc(
            String email, 
            VerificationType type
    );
    
    /**
     * 인증 완료된 레코드 존재 여부 확인
     * (회원가입 시 이메일 인증 완료 여부 체크용)
     */
    boolean existsByEmailAndTypeAndVerifiedTrue(
            String email, 
            VerificationType type
    );
    
    /**
     * 이메일과 타입으로 인증되지 않은 레코드 조회
     */
    Optional<EmailVerification> findByEmailAndTypeAndVerifiedFalse(
            String email, 
            VerificationType type
    );
    
    /**
     * 특정 시간 이전에 생성되고 미인증 상태인 레코드 삭제
     * (배치 작업용 - 미인증 상태로 24시간 지난 데이터 삭제)
     */
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.createdAt < :threshold AND e.verified = false")
    void deleteByCreatedAtBeforeAndVerifiedFalse(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 이메일, 타입, 코드로 레코드 조회
     * (인증 코드 검증용)
     */
    Optional<EmailVerification> findByEmailAndTypeAndCode(
            String email, 
            VerificationType type, 
            String code
    );
    
    /**
     * 특정 이메일과 타입의 모든 인증 레코드 삭제
     * (새 인증 코드 발송 전 기존 코드 삭제용)
     */
    @Modifying
    void deleteByEmailAndType(String email, VerificationType type);
    
    /**
     * 특정 시간 이전 인증 완료 데이터 삭제
     */
    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.verified = true AND ev.verifiedAt < :cutoff")
    int deleteOldVerified(@Param("cutoff") LocalDateTime cutoff);
    
    /**
     * 만료된 데이터 카운트 (로깅용)
     */
    long countByExpiresAtBefore(LocalDateTime now);
    
    /**
     * 인증 완료 데이터 카운트
     */
    long countByVerifiedTrueAndVerifiedAtBefore(LocalDateTime cutoff);
}
