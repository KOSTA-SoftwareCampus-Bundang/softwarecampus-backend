package com.softwarecampus.backend.service.user.signup;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 Service 구현체
 * - 이메일 형식 검증
 * - 비밀번호 암호화
 * - Account 엔티티 생성 및 저장
 * - DB UNIQUE 제약을 통한 동시성 안전 보장
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
     * - DB UNIQUE 제약을 활용하여 동시성 안전 보장
     * - DataIntegrityViolationException 캐치로 중복 처리
     */
    @Override
    @Transactional
    public AccountResponse signup(SignupRequest request) {
        log.info("회원가입 시도 시작");
        
        // 1. 이메일 형식 검증
        validateEmailFormat(request.email());
        
        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        
        // 3. Account 엔티티 생성
        Account account = createAccount(request, encodedPassword);
        
        // 4. 저장 (DB UNIQUE 제약으로 동시성 안전)
        try {
            Account savedAccount = accountRepository.save(account);
            log.info("회원가입 완료: accountId={}", savedAccount.getId());
            
            // 5. DTO 변환
            return toAccountResponse(savedAccount);
        } catch (DataIntegrityViolationException ex) {
            // DB 제약 조건 위반 - 어떤 제약인지 확인
            String message = ex.getMessage();
            if (log.isDebugEnabled()) {
                log.debug("DataIntegrityViolationException details", ex);
            }
            
            if (message != null) {
                // 이메일 중복 확인 (제약 조건 이름: uk_account_email)
                if (message.contains("uk_account_email") || message.contains("email")) {
                    log.warn("Email duplicate detected during database insert");
                    throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
                }
                // 전화번호 중복 확인 (제약 조건 이름: uk_account_phone)
                if (message.contains("uk_account_phone") || message.contains("phoneNumber")) {
                    log.warn("Phone number duplicate detected during database insert");
                    throw new InvalidInputException("이미 사용 중인 전화번호입니다.");
                }
            }
            
            // 그 외 알 수 없는 무결성 제약 위반
            log.error("Unexpected data integrity violation during signup", ex);
            throw new InvalidInputException("회원가입 처리 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 이메일 형식 검증
     */
    private void validateEmailFormat(String email) {
        if (email == null || email.isBlank()) {
            log.warn("Invalid email input: null or blank");
            throw new InvalidInputException("이메일을 입력해주세요.");
        }
        
        if (!EmailUtils.isValidFormat(email)) {
            log.warn("Invalid email format detected: {}", EmailUtils.maskEmail(email));
            throw new InvalidInputException("올바른 이메일 형식이 아닙니다.");
        }
    }
    
    /**
     * Account 엔티티 생성
     * - USER: 즉시 승인 (APPROVED)
     * - ACADEMY: 관리자 승인 대기 (PENDING)
     */
    private Account createAccount(SignupRequest request, String encodedPassword) {
        // 계정 타입별 승인 상태 결정
        AccountType accountType = determineAccountType(request);
        ApprovalStatus approvalStatus = (accountType == AccountType.USER) 
            ? ApprovalStatus.APPROVED   // 일반 사용자: 즉시 승인
            : ApprovalStatus.PENDING;   // 기관: 관리자 승인 대기
        
        return Account.builder()
            .email(request.email())
            .password(encodedPassword)
            .userName(request.userName())
            .phoneNumber(request.phoneNumber())
            .address(request.address())
            .affiliation(request.affiliation())
            .position(request.position())
            .accountType(accountType)
            .accountApproved(approvalStatus)
            .build();
    }
    
    /**
     * 계정 타입 결정
     * - affiliation이 있으면 ACADEMY (기관)
     * - 없으면 USER (일반 사용자)
     */
    private AccountType determineAccountType(SignupRequest request) {
        // 소속이 있으면 기관으로 간주
        if (request.affiliation() != null && !request.affiliation().isBlank()) {
            return AccountType.ACADEMY;
        }
        return AccountType.USER;
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
