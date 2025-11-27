package com.softwarecampus.backend.service.user.signup;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.exception.email.EmailNotVerifiedException;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.domain.common.VerificationType;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.service.user.email.EmailVerificationService;
import com.softwarecampus.backend.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

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
    private final EmailVerificationService emailVerificationService;

    /**
     * 회원가입 처리
     * - DB UNIQUE 제약을 활용하여 동시성 안전 보장
     * - DataIntegrityViolationException 캐치로 중복 처리
     */
    @Override
    @Transactional
    public AccountResponse signup(SignupRequest request) {
        // 0. 요청 null 검증
        Objects.requireNonNull(request, "SignupRequest must not be null");
        
        log.info("회원가입 시도 시작: accountType={}", request.accountType());

        // 1. 이메일 인증 확인
        if (!emailVerificationService.isEmailVerified(request.email(), VerificationType.SIGNUP)) {
            log.warn("회원가입 실패: 이메일 인증되지 않음");
            throw new EmailNotVerifiedException("이메일 인증이 완료되지 않았습니다.");
        }

        // 2. 이메일 형식 검증
        validateEmailFormat(request.email());

        // 3. 계정 타입별 추가 검증
        validateAccountTypeRequirements(request);

        // 4. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 5. Account 엔티티 생성
        Account account = createAccount(request, encodedPassword);

        // 6. 저장 (DB UNIQUE 제약으로 동시성 안전)
        try {
            Account savedAccount = accountRepository.save(account);
            log.info("회원가입 완료: accountId={}, accountType={}",
                    savedAccount.getId(),
                    savedAccount.getAccountType());

            // 7. DTO 변환
            return toAccountResponse(savedAccount);
        } catch (DataIntegrityViolationException ex) {
            // DB 제약 조건 위반 - 어떤 제약인지 확인
            String message = ex.getMessage();
            if (log.isDebugEnabled()) {
                log.debug("DataIntegrityViolationException details", ex);
            }

            if (message != null) {
                // 대소문자 무시 비교를 위해 소문자로 정규화
                String normalizedMessage = message.toLowerCase(Locale.ROOT);
                
                // 이메일 중복 확인 (제약 조건 이름: uk_account_email)
                if (normalizedMessage.contains("uk_account_email")) {
                    log.warn("Email duplicate detected during database insert");
                    throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
                }
                // 전화번호 중복 확인 (제약 조건 이름: uk_account_phone)
                if (normalizedMessage.contains("uk_account_phone")) {
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
     * 계정 타입별 추가 검증
     * - ADMIN 타입: 회원가입 불가 (DB 직접 수정으로만 생성)
     * - ACADEMY 타입: academyId 필수
     * - USER 타입: academyId 금지
     */
    private void validateAccountTypeRequirements(SignupRequest request) {
        // ADMIN은 회원가입 불가 (DB 직접 수정 또는 시스템 관리 스크립트로만 생성)
        if (request.accountType() == AccountType.ADMIN) {
            log.warn("ADMIN type signup attempt blocked");
            throw new InvalidInputException("관리자 계정은 회원가입으로 생성할 수 없습니다.");
        }
        
        if (request.accountType() == AccountType.ACADEMY) {
            if (request.academyId() == null) {
                log.warn("ACADEMY type signup without academyId");
                throw new InvalidInputException("기관 회원은 기관 ID가 필수입니다.");
            }
            // 향후: Academy 엔티티 존재 여부 검증 추가 가능
            // academyRepository.findById(request.academyId())
            // .orElseThrow(() -> new InvalidInputException("존재하지 않는 기관입니다."));
        } else if (request.accountType() == AccountType.USER) {
            if (request.academyId() != null) {
                log.warn("USER type signup with academyId");
                throw new InvalidInputException("일반 회원은 기관 ID를 가질 수 없습니다.");
            }
        }
    }

    /**
     * Account 엔티티 생성
     * - USER: 즉시 승인 (APPROVED)
     * - ACADEMY: 관리자 승인 대기 (PENDING)
     * - ADMIN: 이 메서드 호출 전 validateAccountTypeRequirements()에서 차단됨
     */
    private Account createAccount(SignupRequest request, String encodedPassword) {
        // 계정 타입별 승인 상태 결정
        ApprovalStatus approvalStatus = switch (request.accountType()) {
            case USER -> ApprovalStatus.APPROVED;      // 일반 사용자: 즉시 승인
            case ACADEMY -> ApprovalStatus.PENDING;    // 기관: 관리자 승인 대기
            case ADMIN -> throw new IllegalStateException(
                "ADMIN 계정은 validateAccountTypeRequirements()에서 차단되어야 합니다."
            );
        };

        return Account.builder()
                .email(request.email())
                .password(encodedPassword)
                .userName(request.userName())
                .phoneNumber(request.phoneNumber())
                .address(request.address())
                .affiliation(request.affiliation())
                .position(request.position())
                .accountType(request.accountType())
                .academyId(request.academyId())
                .accountApproved(approvalStatus)
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
                account.getPosition());
    }
    
    /**
     * 이메일 중복 확인
     */
    @Override
    public boolean isEmailAvailable(String email) {
        // 이메일 형식 검증 (기존 메서드 재사용)
        validateEmailFormat(email);
        
        // 중복 확인
        return !accountRepository.existsByEmail(email);
    }
}
