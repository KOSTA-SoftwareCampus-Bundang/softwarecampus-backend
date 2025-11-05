package com.softwarecampus.backend.service.user.signup;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 Service 구현체
 * - 이메일 중복 체크
 * - 비밀번호 암호화
 * - Account 엔티티 생성 및 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SignupServiceImpl implements SignupService {
    
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 회원가입 처리
     */
    @Override
    @Transactional
    public AccountResponse signup(SignupRequest request) {
        log.info("회원가입 시도: email={}", request.email());
        
        // 1. 이메일 중복 체크
        validateEmailNotDuplicate(request.email());
        
        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        
        // 3. Account 엔티티 생성
        Account account = createAccount(request, encodedPassword);
        
        // 4. 저장
        Account savedAccount = accountRepository.save(account);
        log.info("회원가입 완료: accountId={}, email={}", savedAccount.getId(), savedAccount.getEmail());
        
        // 5. DTO 변환
        return toAccountResponse(savedAccount);
    }
    
    /**
     * 이메일 중복 체크
     */
    private void validateEmailNotDuplicate(String email) {
        if (accountRepository.existsByEmail(email)) {
            log.warn("이메일 중복: {}", email);
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다: " + email);
        }
    }
    
    /**
     * Account 엔티티 생성
     */
    private Account createAccount(SignupRequest request, String encodedPassword) {
        return Account.builder()
            .email(request.email())
            .password(encodedPassword)
            .userName(request.userName())
            .phoneNumber(request.phoneNumber())
            .address(request.address())
            .affiliation(request.affiliation())
            .position(request.position())
            .accountType(AccountType.USER)                 // 기본값: USER
            .accountApproved(ApprovalStatus.APPROVED)      // 기본값: APPROVED
            .build();
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
}
