package com.softwarecampus.backend.repository.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 훈련기관 레포지토리
 */
@Repository
public interface AcademyRepository extends JpaRepository<Academy, Long> {
    
    // 승인된 기관 목록
    List<Academy> findByIsApprovedAndIsDeletedFalseOrderByCreatedAtDesc(ApprovalStatus approved);
    
    // 승인 대기중인 기관 목록
    List<Academy> findByIsApprovedAndIsDeletedFalse(ApprovalStatus approved);
    
    // 사업자번호로 검색
    Optional<Academy> findByBusinessNumberAndIsDeletedFalse(String businessNumber);
    
    boolean existsByBusinessNumber(String businessNumber);
}
