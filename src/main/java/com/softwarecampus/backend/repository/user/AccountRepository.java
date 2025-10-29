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
 * Account 엔티티에 대한 Repository
 * - 사용자 계정 CRUD 및 조회 기능
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    /**
     * 이메일로 계정 조회 (로그인)
     */
    Optional<Account> findByEmail(String email);
    
    /**
     * 이메일 중복 체크
     */
    boolean existsByEmail(String email);
    
    /**
     * 사용자명 중복 체크
     */
    boolean existsByUserName(String userName);
    
    /**
     * 전화번호 중복 체크
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    /**
     * 승인 대기중인 기관 계정 조회 (관리자용)
     */
    @Query("SELECT a FROM Account a WHERE a.accountType = 'ACADEMY' AND a.accountApproved = 'PENDING' AND a.isDeleted = false")
    List<Account> findPendingAcademyAccounts();
    
    /**
     * 계정 타입별 조회 (삭제되지 않은 것만)
     */
    List<Account> findByAccountTypeAndIsDeleted(AccountType accountType, Boolean isDeleted);
    
    /**
     * 계정 타입 및 승인 상태별 조회
     */
    List<Account> findByAccountTypeAndAccountApprovedAndIsDeleted(
        AccountType accountType, 
        ApprovalStatus accountApproved,
        Boolean isDeleted
    );
}
