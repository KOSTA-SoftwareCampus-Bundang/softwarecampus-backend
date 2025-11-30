package com.softwarecampus.backend.service.admin;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.AccountUpdateRequest;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.service.user.email.EmailSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountAdminServiceImpl implements AccountAdminService {

    private final AccountRepository accountRepository;
    private final EmailSendService emailSendService;

    private Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account with id: " + accountId + " not found"));
    }

    /**
     * Account 엔티티를 기존 AccountResponse DTO 레코드 타입으로 변환
     */
    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getUserName(),
                account.getPhoneNumber(),
                account.getAccountType(),
                account.getAccountApproved(),
                account.getAddress(),
                account.getAffiliation(),
                account.getPosition(),
                account.getProfileImage());
    }

    /**
     * 전체 활성 회원 목록 조회
     */
    @Override
    public Page<AccountResponse> getAllActiveAccounts(Pageable pageable) {
        Page<Account> accountPage = accountRepository.findByIsDeletedFalse(pageable);

        return accountPage.map(this::toResponse);
    }

    /**
     * 회원 목록 검색
     */
    @Override
    public Page<AccountResponse> searchAccounts(String keyword, Pageable pageable) {
        Page<Account> accountPage = accountRepository.searchActiveAccounts(keyword, pageable);

        return accountPage.map(this::toResponse);
    }

    /**
     * 특정 회원 상세 정보 조회
     */
    @Override
    public AccountResponse getAccountDetail(Long accountId) {
        Account account = findAccount(accountId);
        if (account.getIsDeleted()) {
            throw new NoSuchElementException("Account with id: " + accountId + " not found");
        }
        return toResponse(account);
    }

    /**
     * 회원 정보 수정
     */
    @Override
    @Transactional
    public AccountResponse updateAccount(Long accountId, AccountUpdateRequest request) {
        Account account = findAccount(accountId);

        // 삭제된 계정이 수정되지 못하도록 조건을 추가해준다.
        if (account.getIsDeleted()) {
            throw new NoSuchElementException("Account with id: " + accountId + " not found");
        }

        account.setUserName(request.getUserName());
        account.setPhoneNumber(request.getPhoneNumber());

        if (request.getAffiliation() != null) {
            account.setAffiliation(request.getAffiliation());
        }
        if (request.getPosition() != null) {
            account.setPosition(request.getPosition());
        }
        if (request.getAddress() != null) {
            account.setAddress(request.getAddress());
        }

        if (request.getAccountApproved() != null) {
            account.setAccountApproved(request.getAccountApproved());
        }

        return toResponse(account);
    }

    /**
     * 회원 삭제
     */
    @Override
    @Transactional
    public void deleteAccount(Long accountId) {
        Account account = findAccount(accountId);
        accountRepository.delete(account);
    }

    /**
     * 회원 승인
     * - 승인 상태 변경 및 승인 이메일 발송
     * - 수정일: 2025-11-29 - 이메일 발송을 트랜잭션 커밋 후로 분리
     */
    @Override
    @Transactional
    public AccountResponse approveAccount(Long accountId) {
        Account account = findAccount(accountId);

        if (account.getIsDeleted()) {
            throw new NoSuchElementException("Account with id: " + accountId + " not found");
        }

        account.setAccountApproved(ApprovalStatus.APPROVED);
        AccountResponse response = toResponse(account);

        // 트랜잭션 커밋 후 이메일 발송 (이메일 실패해도 승인은 완료)
        String email = account.getEmail();
        String userName = account.getUserName();
        if (email != null) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                emailSendService.sendAccountApprovalEmail(email, userName);
                            } catch (Exception e) {
                                log.error("회원 승인 이메일 발송 실패 - 회원 ID: {}", accountId, e);
                            }
                        }
                    });
        } else {
            log.warn("회원 ID {}는 이메일 주소가 없어 승인 이메일을 발송하지 않습니다", accountId);
        }

        return response;
    }
}
