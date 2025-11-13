package com.softwarecampus.backend.service.academy;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.AccountUpdateRequest;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountAdminServiceImpl implements AccountAdminService {

    private final AccountRepository accountRepository;

    private Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account with id: " + accountId + " not found"));
    }

    /**
     *  Account 엔티티를 기존 AccountResponse DTO 레코드 타입으로 변환
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
                account.getPosition()
        );
    }

    /**
     *  전체 활성 회원 목록 조회
     */
    @Override
    public Page<AccountResponse> getAllActiveAccounts(Pageable pageable) {
        Page<Account> accountPage = accountRepository.findByIsDeletedFalse(pageable);

        return accountPage.map(this::toResponse);
    }

    /**
     *  회원 목록 검색
     */
    @Override
    public Page<AccountResponse> searchAccounts(String keyword, Pageable pageable) {
        Page<Account> accountPage = accountRepository.searchActiveAccounts(keyword, pageable);

        return accountPage.map(this::toResponse);
    }

    /**
     *  특정 회원 상세 정보 조회
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
     *  회원 정보 수정
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
        account.setAffiliation(request.getAffiliation());
        account.setPosition(request.getPosition());

        if (request.getAccountApproved() != null) {
            account.setAccountApproved(request.getAccountApproved());
        }

        return toResponse(account);
    }

    /**
     *  회원 삭제
     */
    @Override
    @Transactional
    public void deleteAccount(Long accountId) {
        Account account = findAccount(accountId);
        accountRepository.delete(account);
    }
}
