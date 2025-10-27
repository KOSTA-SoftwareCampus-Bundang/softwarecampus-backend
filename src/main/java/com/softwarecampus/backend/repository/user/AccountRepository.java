package com.softwarecampus.backend.repository.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 계정 레포지토리
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    // 승인 대기중인 기관 계정 조회
    @Query("SELECT a FROM Account a WHERE a.accountType = 'ACADEMY' AND a.accountApproved = 'PENDING'")
    List<Account> findPendingAcademyAccounts();
    
    // 타입별 계정 목록
    List<Account> findByAccountTypeAndIsDeletedFalse(AccountType accountType);
}
