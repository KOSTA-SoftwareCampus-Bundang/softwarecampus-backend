package com.softwarecampus.backend.repository.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * 이메일로 활성 계정 조회 (Soft Delete 고려)
     * - isDeleted=false인 계정만 조회
     * - 로그인, 프로필 조회 등에 사용
     */
    Optional<Account> findByEmailAndIsDeletedFalse(String email);

    /**
     * 활성 이메일 중복 체크 (Soft Delete 고려)
     * - isDeleted=false인 계정 중에서만 중복 체크
     * - 삭제된 계정의 이메일 재사용 허용
     */
    boolean existsByEmailAndIsDeletedFalse(String email);

    /**
     * 활성 사용자명 중복 체크 (Soft Delete 고려)
     * - isDeleted=false인 계정 중에서만 중복 체크
     */
    boolean existsByUserNameAndIsDeletedFalse(String userName);

    /**
     * 사용자명으로 활성 계정 조회 (Soft Delete 고려)
     */
    Optional<Account> findByUserNameAndIsDeletedFalse(String userName);

    /**
     * 활성 전화번호 중복 체크 (Soft Delete 고려)
     * - isDeleted=false인 계정 중에서만 중복 체크
     * - 삭제된 계정의 전화번호 재사용 허용
     */
    boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);

    /**
     * 계정 타입별 활성 계정 조회
     */
    List<Account> findByAccountTypeAndIsDeletedFalse(AccountType accountType);

    /**
     * 계정 타입 및 승인 상태별 활성 계정 조회
     */
    List<Account> findByAccountTypeAndAccountApprovedAndIsDeletedFalse(
            AccountType accountType,
            ApprovalStatus accountApproved);

    /**
     * 전체 활성 계정 목록 조회
     */
    Page<Account> findByIsDeletedFalse(Pageable pageable);

    /**
     * 활성 계정을 대상으로 특정 회원 검색
     */
    @Query("SELECT a FROM Account a " +
            "WHERE a.isDeleted = false AND " + // Soft Delete 제외 조건
            "(:keyword IS NULL OR " +
            "LOWER(a.userName) LIKE %:keyword% OR " +
            "LOWER(a.email) LIKE %:keyword% OR " +
            "a.phoneNumber LIKE %:keyword%)")
    Page<Account> searchActiveAccounts(@Param("keyword") String keyword, Pageable pageable);
}
