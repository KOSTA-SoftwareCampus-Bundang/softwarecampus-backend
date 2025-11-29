package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.domain.common.VerificationType;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.ChangePasswordRequest;
import com.softwarecampus.backend.dto.user.EmailVerificationCodeRequest;
import com.softwarecampus.backend.dto.user.ResetPasswordRequest;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.exception.user.PhoneNumberAlreadyExistsException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.repository.user.EmailVerificationRepository;
import com.softwarecampus.backend.service.user.email.EmailVerificationService;
import com.softwarecampus.backend.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 조회 및 프로필 관리 Service 구현체
 * - Phase 5: 기본 조회 기능
 * - Phase 15-1: 프로필 수정/삭제 기능 추가
 * - Phase 15-2: 비밀번호 재설정 기능 추가
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {
    
    private final AccountRepository accountRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    
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
     * 프로필 수정
     */
    @Override
    @Transactional
    public AccountResponse updateProfile(String email, UpdateProfileRequest request) {
        // 1. 입력 검증
        validateEmailInput(email);
        
        // 2. Account 조회
        log.info("프로필 수정 시도: email={}", EmailUtils.maskEmail(email));
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다."));
        
        // 3. 전화번호 중복 검증 (변경하는 경우에만)
        if (request.getPhoneNumber() != null && 
            !request.getPhoneNumber().equals(account.getPhoneNumber())) {
            
            if (accountRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                log.warn("전화번호 중복: phoneNumber={}", request.getPhoneNumber());
                throw new PhoneNumberAlreadyExistsException(request.getPhoneNumber());
            }
        }
        
        // 4. Account 필드 업데이트
        updateAccountFields(account, request);
        
        // 5. 저장 및 응답 (JPA dirty checking으로 자동 저장)
        log.info("프로필 수정 완료: email={}, accountId={}", EmailUtils.maskEmail(email), account.getId());
        return toAccountResponse(account);
    }
    
    /**
     * 계정 삭제 (소프트 삭제)
     */
    @Override
    @Transactional
    public void deleteAccount(String email) {
        // 1. 입력 검증
        validateEmailInput(email);
        
        // 2. Account 조회
        log.info("계정 삭제 시도: email={}", EmailUtils.maskEmail(email));
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다."));
        
        // 3. 소프트 삭제
        account.markDeleted();
        
        log.info("계정 삭제 완료 (소프트): email={}, accountId={}", EmailUtils.maskEmail(email), account.getId());
    }
    
    /**
     * 비밀번호 재설정 (이메일 인증 코드 검증)
     * - EmailVerificationService.verifyResetCode() 재사용
     */
    @Override
    @Transactional
    public void resetPassword(String email, ResetPasswordRequest request) {
        // 1. 입력 검증
        validateEmailInput(email);
        
        log.info("비밀번호 재설정 시도: email={}", EmailUtils.maskEmail(email));
        
        // 2. 계정 조회
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다."));
        
        // 3. 이메일 인증 코드 검증 (EmailVerificationService 재사용)
        // - 차단 상태 확인, 코드 만료 확인, 코드 일치 검증, 시도 횟수 관리 등 모두 포함
        EmailVerificationCodeRequest codeRequest = new EmailVerificationCodeRequest(email, request.getCode());
        emailVerificationService.verifyResetCode(codeRequest);
        
        // 4. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        account.setPassword(encodedPassword);
        
        // 5. 인증 레코드 삭제 (일회용 보장)
        emailVerificationRepository.deleteByEmailAndType(email, VerificationType.PASSWORD_RESET);
        
        log.info("비밀번호 재설정 완료: email={}, accountId={}", 
            EmailUtils.maskEmail(email), account.getId());
    }

    /**
     * 비밀번호 변경 (로그인 상태)
     */
    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        // 1. 입력 검증
        validateEmailInput(email);
        
        log.info("비밀번호 변경 시도: email={}", EmailUtils.maskEmail(email));
        
        // 2. 계정 조회
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다."));
        
        // 3. 새 비밀번호 변경 (현재 비밀번호 확인 없이 덮어쓰기)
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        account.setPassword(encodedPassword);
        
        log.info("비밀번호 변경 완료: email={}, accountId={}", 
            EmailUtils.maskEmail(email), account.getId());
    }
    
    /**
     * Account 필드 업데이트 (null이 아닌 필드만)
     */
    private void updateAccountFields(Account account, UpdateProfileRequest request) {
        if (request.getUserName() != null) {
            account.setUserName(request.getUserName());
        }
        if (request.getPhoneNumber() != null) {
            account.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            account.setAddress(request.getAddress());
        }
        if (request.getAffiliation() != null) {
            account.setAffiliation(request.getAffiliation());
        }
        if (request.getPosition() != null) {
            account.setPosition(request.getPosition());
        }
        if (request.getProfileImage() != null) {
            account.setProfileImage(request.getProfileImage());
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
            account.getPosition(),
            account.getProfileImage()
        );
    }
}
