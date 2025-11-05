package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 조회 Service 구현체
 * - Phase 5: 기본 조회 기능
 * - Phase 18: 수정/삭제 기능 추가 예정
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {
    
    private final AccountRepository accountRepository;
    
    /**
     * ID로 계정 조회
     */
    @Override
    public AccountResponse getAccountById(Long accountId) {
        log.info("계정 조회: accountId={}", accountId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다: " + accountId));
        
        return toAccountResponse(account);
    }
    
    /**
     * 이메일로 계정 조회
     */
    @Override
    public AccountResponse getAccountByEmail(String email) {
        log.info("계정 조회: email={}", email);
        
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다: " + email));
        
        return toAccountResponse(account);
    }
    
    /**
     * Entity → DTO 변환
     */
    private AccountResponse toAccountResponse(Account account) {
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
    
    // Phase 18에서 추가 예정:
    // - updateProfile(Long id, UpdateRequest request)
    // - deleteAccount(Long id)
}
