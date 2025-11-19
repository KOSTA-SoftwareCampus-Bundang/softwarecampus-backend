# 2. SignupService 구현

## SignupService.java (인터페이스)

**경로:** `service/user/signup/SignupService.java`

**설명:** 회원가입 기능 정의

```java
package com.softwarecampus.backend.service.user.signup;

import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.dto.user.AccountResponse;

/**
 * 회원가입 Service 인터페이스
 */
public interface SignupService {
    
    /**
     * 회원가입
     * 
     * @param request 회원가입 요청 DTO
     * @return 생성된 계정 정보
     * @throws DuplicateEmailException 이메일이 이미 존재하는 경우
     */
    AccountResponse signup(SignupRequest request);
}
```

---

## SignupServiceImpl.java (구현체)

**경로:** `service/user/signup/SignupServiceImpl.java`

**설명:** 회원가입 비즈니스 로직 전담

```java
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
 * 회원가입 Service
 * - 이메일 형식 검증 (RFC 5322, RFC 1035)
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
        log.info("회원가입 시도 시작: accountType={}", request.accountType());
        
        // 1. 이메일 형식 검증
        validateEmailFormat(request.email());
        
        // 2. 계정 타입별 추가 검증
        validateAccountTypeRequirements(request);
        
        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        
        // 4. Account 엔티티 생성
        Account account = createAccount(request, encodedPassword);
        
        // 5. 저장 (DB UNIQUE 제약으로 동시성 안전)
        try {
            Account savedAccount = accountRepository.save(account);
            log.info("회원가입 완료: accountId={}, accountType={}", 
                savedAccount.getId(), 
                savedAccount.getAccountType());
            
            // 6. DTO 변환
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
            //     .orElseThrow(() -> new InvalidInputException("존재하지 않는 기관입니다."));
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
            account.getPosition()
        );
    }
}
```

---

## 설계 포인트

- 회원가입 로직만 집중 (단일 책임)
- private 메서드로 로직 분리 (가독성)
- **동시성**: DB UNIQUE 제약으로 Race Condition 방어
- **입력 검증**: RFC 5322, RFC 1035 표준 준수
- **계정 타입**: 프론트엔드에서 accountType 전달받아 사용
- **ADMIN 차단**: 회원가입으로 ADMIN 계정 생성 불가 (보안)
- **ACADEMY 타입 검증**: academyId 필수 값 검증
- **USER 타입 검증**: academyId 금지
- **예외 처리**: `DataIntegrityViolationException` 상세 처리
- **PII 보호**: 이메일 마스킹 (`EmailUtils.maskEmail`)
- **로깅 개선**: accountType 정보 포함하여 디버깅 용이
- **명시적 분기**: Switch expression으로 모든 AccountType 처리 명확화
- 파일 크기 약 170줄

---

## PR 리뷰 반영 (2024-11-12)

### Issue #1: accountType 자동 결정 제거
**리뷰 내용:**
> "백엔드에서 affiliation 기반으로 accountType을 자동 결정하는 것은 비즈니스 로직 오류. 프론트엔드에서 명확하게 전달받아야 함."

**수정 내용:**
- `determineAccountType()` 메서드 제거
- `SignupRequest`에 `@NotNull AccountType accountType` 필드 추가
- `SignupRequest`에 `Long academyId` 필드 추가 (ACADEMY 타입용)
- `Account` 엔티티에 `academyId` 필드 추가
- `validateAccountTypeRequirements()` 메서드 추가 (ACADEMY 타입은 academyId 필수 검증)
- `createAccount()`에서 `request.accountType()` 직접 사용
- 로깅에 accountType 정보 포함

**변경 이유:**
- 프론트엔드가 사용자 선택을 기반으로 accountType을 결정해야 함
- 백엔드는 검증만 수행 (ACADEMY 타입일 때 academyId 필수 확인)
- affiliation은 선택 필드로, 비즈니스 로직 판단 근거로 부적합

### Issue #2: ADMIN 계정 생성 차단
**리뷰 내용:**
> "ADMIN 타입도 회원가입을 통해 생성될 수 있으나, 승인 상태 처리가 명확하지 않음. 보안 위험."

**수정 내용:**
- `validateAccountTypeRequirements()`에서 ADMIN 타입 회원가입 차단
- Switch expression으로 모든 AccountType 처리 명시화
- ADMIN 케이스에 `IllegalStateException` 추가 (이중 방어)

**변경 이유:**
- 🔒 보안: 일반 사용자가 ADMIN 계정 생성 불가
- 📋 정책: ADMIN은 DB 직접 수정 또는 시스템 관리 스크립트로만 생성
- 🛡️ 이중 방어: 검증 단계 + 생성 단계 모두에서 차단

---

## CodeRabbit 리뷰 반영

### 메시지 파싱 방식 유지
**CodeRabbit 리뷰:**
> "메시지 파싱은 DB/JPA 메시지 변경 시 불안정. DB에서 이메일 재확인 권장."

**논의 및 결정:**
- DB 제약 조건 변경 계획 없음 (안정적 환경)
- DB 재확인 시 추가 쿼리 발생 (성능 저하)
- YAGNI 원칙 (You Aren't Gonna Need It)
- **결정:** 현재 방식 유지 (실용적 판단)
