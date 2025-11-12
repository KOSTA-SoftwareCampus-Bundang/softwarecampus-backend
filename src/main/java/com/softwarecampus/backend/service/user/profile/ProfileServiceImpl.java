package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.util.EmailUtils;
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
        log.info("계정 조회 시도: accountId={}", accountId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다."));
        
        log.info("계정 조회 완료: accountId={}, email={}, accountType={}", 
            account.getId(),
            EmailUtils.maskEmail(account.getEmail()),
            account.getAccountType());
        
        return toAccountResponse(account);
    }
    
    /**
     * 이메일로 계정 조회
     */
    @Override
    public AccountResponse getAccountByEmail(String email) {
        // 1. 입력 검증
        validateEmailInput(email);
        
        // 2. 계정 조회 (PII 마스킹 로깅)
        log.info("계정 조회 시도: email={}", EmailUtils.maskEmail(email));
        
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다."));
        
        log.info("계정 조회 완료: accountId={}, accountType={}, userName={}", 
            account.getId(),
            account.getAccountType(),
            account.getUserName());
        
        return toAccountResponse(account);
    }
    
    /**
     * 이메일 입력 검증
     */
    private void validateEmailInput(String email) {
        if (email == null || email.isBlank()) {
            log.warn("Invalid email input: null or blank");
            throw new InvalidInputException("이메일을 입력해주세요.");
        }
        
        if (!EmailUtils.isValidFormat(email)) {
            log.warn("Invalid email format: {}", EmailUtils.maskEmail(email));
            throw new InvalidInputException("올바른 이메일 형식이 아닙니다.");
        }
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
