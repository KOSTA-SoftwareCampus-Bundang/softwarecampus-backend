package com.softwarecampus.backend.scheduler;

import com.softwarecampus.backend.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 이메일 인증 데이터 정리 스케줄러
 * - 매일 새벽 2시 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {
    
    private final EmailVerificationRepository verificationRepository;
    
    /**
     * 만료된 인증 데이터 삭제
     * - 인증 완료 후 24시간 지난 데이터 (verifiedAt 기준)
     * - 미인증 상태로 24시간 지난 데이터 (createdAt 기준)
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    @Transactional
    public void cleanupExpiredVerifications() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        
        try {
            // 1. 인증 완료 후 24시간 지난 데이터 삭제 (verifiedAt 기준)
            int verifiedDeleted = verificationRepository.deleteOldVerified(threshold);
            
            // 2. 미인증 상태로 24시간 지난 데이터 삭제 (createdAt 기준)
            verificationRepository.deleteByCreatedAtBeforeAndVerifiedFalse(threshold);
            
            log.info("만료된 이메일 인증 데이터 정리 완료 - 인증 완료 삭제: {}, threshold: {}", 
                    verifiedDeleted, threshold);
        } catch (Exception e) {
            log.error("이메일 인증 데이터 정리 실패", e);
        }
    }
}
