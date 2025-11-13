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
     *  Page 처리 헬퍼 메서드 : Page의 내용을 Stream으로 필터링/매핑 후, 다시 Page 객체로 재구성
     */
    private Page<AccountResponse> filterAndMapToPage(Page<Account> accountPage, Pageable pageable, String searchKeyword) {

        String normalizedKeyword = searchKeyword == null ? null : searchKeyword.toLowerCase();

        List<AccountResponse> filteredList = accountPage.stream()
                .filter(account -> account.isActive() && (
                        normalizedKeyword == null ||
                                (account.getUserName() != null && account.getUserName().toLowerCase().contains(normalizedKeyword)) ||
                                (account.getEmail() != null && account.getEmail().toLowerCase().contains(normalizedKeyword)) ||
                                (account.getPhoneNumber() != null && account.getPhoneNumber().contains(normalizedKeyword))
                ))
                .map(this::toResponse)
                .collect(Collectors.toList());

        // Soft Delete 계정이 전체 요소 수에 포함되므로, 전체 요소 수는 그대로 유지하면서 현재 페이지 목록만 필터링합니다.
        // 이 방식은 성능상 비효율적이며, Repository에서 필터링하는 것이 최적입니다.
        return new PageImpl<>(filteredList, pageable, accountPage.getTotalElements());

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
