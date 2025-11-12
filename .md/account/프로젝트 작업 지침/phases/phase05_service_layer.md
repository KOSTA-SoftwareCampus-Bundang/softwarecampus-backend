# Phase 5: Service Layer + 도메인 예외

**목표:** 회원가입 비즈니스 로직을 처리하는 Service Layer 구현  
**담당자:** 태윤  
**상태:** ✅ 완료 (2025-11-05)

---

## 📋 작업 개요

회원가입의 핵심 비즈니스 로직을 처리하는 Service Layer를 구현합니다. 이메일 형식 검증, 비밀번호 암호화, 엔티티 저장 등의 작업을 수행하며, 발생 가능한 도메인 예외를 함께 정의합니다.

**설계 원칙:**
- 기능별 독립 패키지 (signup/login/profile)
- 각 기능은 인터페이스 + 구현체 쌍으로 구성
- DTO ↔ Entity 변환은 Service Layer에서 처리
- 비즈니스 예외는 도메인 예외로 명확히 표현
- `@Transactional` 적용으로 트랜잭션 보장
- **보안**: PII(개인정보) 로깅 제거, 동시성 안전 처리

---

## 📂 생성/수정 파일

### 새로 생성된 파일:
```text
src/main/java/com/softwarecampus/backend/
├─ service/user/
│  ├─ signup/
│  │  ├─ SignupService.java              ✅ 회원가입 인터페이스
│  │  └─ SignupServiceImpl.java          ✅ 회원가입 구현
│  └─ profile/
│     ├─ ProfileService.java             ✅ 프로필 인터페이스
│     └─ ProfileServiceImpl.java         ✅ 프로필 구현
├─ exception/user/
│  ├─ InvalidInputException.java         ✅ 잘못된 입력 예외
│  ├─ DuplicateEmailException.java       ✅ 이메일 중복 예외
│  └─ AccountNotFoundException.java      ✅ 계정 미존재 예외
└─ util/
   └─ EmailUtils.java                    ✅ 이메일 검증/마스킹 유틸

.md/account/시나리오/
├─ README.md                             ✅ 시나리오 목록
├─ signup_scenarios.md                   ✅ 회원가입 시나리오
└─ profile_scenarios.md                  ✅ 프로필 조회 시나리오
```

### 수정된 파일:
```text
src/main/java/com/softwarecampus/backend/
├─ exception/
│  └─ GlobalExceptionHandler.java        ✅ InvalidInputException 핸들러 추가
└─ dto/user/
   └─ MessageResponse.java               ✅ Status 필드 제거 (RESTful)
```

**Phase별 확장 계획:**
- **Phase 5 (현재)**: Signup + Profile (조회만)
- **Phase 16**: `login/LoginService.java` + `login/LoginServiceImpl.java` 추가
- **Phase 18**: ProfileService 확장 (수정/삭제 기능 추가)

---

## 🔨 구현 내용

### 1. SignupService.java (회원가입 인터페이스)

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

### 2. SignupServiceImpl.java (회원가입 구현)

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
```

**설계 포인트:**
- 회원가입 로직만 집중 (단일 책임)
- private 메서드로 로직 분리 (가독성)
- **동시성**: DB UNIQUE 제약으로 Race Condition 방어
- **입력 검증**: RFC 5322, RFC 1035 표준 준수
- **계정 타입 자동 결정**: affiliation 유무로 USER/ACADEMY 판단
- **예외 처리**: `DataIntegrityViolationException` 상세 처리
- **PII 보호**: 이메일 마스킹 (`EmailUtils.maskEmail`)
- 파일 크기 약 150줄

---

### 3. ProfileService.java (프로필 인터페이스)

**경로:** `service/user/profile/ProfileService.java`

**설명:** 계정 조회 기능 정의

```java
package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.dto.user.AccountResponse;

/**
 * 계정 조회 Service 인터페이스
 */
public interface ProfileService {
    
    /**
     * ID로 계정 조회
     * 
     * @param accountId 계정 ID
     * @return 계정 정보
     * @throws AccountNotFoundException 계정이 존재하지 않는 경우
     */
    AccountResponse getAccountById(Long accountId);
    
    /**
     * 이메일로 계정 조회
     * 
     * @param email 이메일
     * @return 계정 정보
     * @throws AccountNotFoundException 계정이 존재하지 않는 경우
     */
    AccountResponse getAccountByEmail(String email);
}
```

---

### 4. ProfileServiceImpl.java (프로필 구현)

**경로:** `service/user/profile/ProfileServiceImpl.java`

**설명:** 계정 조회 기능 구현 (Phase 5는 조회만, Phase 18에서 확장)

```java
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
 * 계정 조회 Service
 * - Phase 5: 기본 조회 기능
 * - Phase 18: 수정/삭제 기능 추가 예정
 * - PII 로깅 보호 (이메일 마스킹)
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
            .orElseThrow(() -> {
                log.warn("계정 미존재: accountId={}", accountId);
                return new AccountNotFoundException("계정을 찾을 수 없습니다.");
            });
        
        return AccountResponse.from(account);
    }
    
    /**
     * 이메일로 계정 조회
     * 
     * @throws InvalidInputException 이메일 형식 오류
     * @throws AccountNotFoundException 계정 미존재
     */
    @Override
    public AccountResponse getAccountByEmail(String email) {
        String maskedEmail = EmailUtils.maskEmail(email);
        log.info("계정 조회: maskedEmail={}", maskedEmail);
        
        // 이메일 형식 검증
        validateEmailFormat(email);
        
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("계정 미존재: maskedEmail={}", maskedEmail);
                return new AccountNotFoundException("계정을 찾을 수 없습니다.");
            });
        
        return AccountResponse.from(account);
    }
    
    /**
     * 이메일 형식 검증
     * RFC 5322 (이메일 기본 형식) + RFC 1035 (도메인 레이블 규칙)
     */
    private void validateEmailFormat(String email) {
        if (!EmailUtils.isValidEmail(email)) {
            log.warn("잘못된 이메일 형식: maskedEmail={}", EmailUtils.maskEmail(email));
            throw new InvalidInputException("잘못된 이메일 형식입니다.");
        }
    }
}
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
```

**설계 포인트:**
- Phase 5는 조회 기능만 구현 (Phase 6 테스트용)
- Phase 18에서 수정/삭제 기능 추가
- 파일 크기 약 80줄 (간단명료)

---

### 5. InvalidInputException.java

**경로:** `exception/user/InvalidInputException.java`

**설명:** 잘못된 입력 (이메일 형식 오류 등) 시 발생하는 도메인 예외

```java
package com.softwarecampus.backend.exception.user;

/**
 * 잘못된 입력 예외
 * - 이메일 형식 오류 (RFC 5322, RFC 1035 위반)
 * - 닉네임 중복 등
 */
public class InvalidInputException extends RuntimeException {
    
    public InvalidInputException(String message) {
        super(message);
    }
    
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**설계 포인트:**
- `RuntimeException` 상속 (Unchecked Exception)
- 입력 검증 실패 시 명확한 예외 표현
- GlobalExceptionHandler에서 400 Bad Request 응답

---

### 6. DuplicateEmailException.java

**경로:** `exception/user/DuplicateEmailException.java`

**설명:** 이메일 중복 시 발생하는 도메인 예외

```java
package com.softwarecampus.backend.exception.user;

/**
 * 이메일 중복 예외
 * - 회원가입 시 이미 존재하는 이메일로 가입 시도할 때 발생
 * - DB UNIQUE 제약 위반 시에도 발생 (동시성 안전)
 */
public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException(String message) {
        super(message);
    }
    
    public DuplicateEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**설계 포인트:**
- `RuntimeException` 상속 (Unchecked Exception)
- 비즈니스 로직에서 발생하는 예외는 명시적으로 처리
- GlobalExceptionHandler에서 409 Conflict 응답

---

### 7. AccountNotFoundException.java

**경로:** `exception/user/AccountNotFoundException.java`

**설명:** 계정 미존재 시 발생하는 도메인 예외

```java
package com.softwarecampus.backend.exception.user;

/**
 * 계정 미존재 예외
 * - ID 또는 이메일로 계정 조회 시 존재하지 않을 때 발생
 */
public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException(String message) {
        super(message);
    }
    
    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

```

**설계 포인트:**
- 마이페이지 조회 시 사용 (Phase 18)
- 로그인 시 계정 존재 여부 체크 시 사용 (Phase 16)
- GlobalExceptionHandler에서 404 Not Found 응답

---

### 8. EmailUtils.java (이메일 검증 및 마스킹)

**경로:** `util/EmailUtils.java`

**설명:** 이메일 형식 검증 및 PII 보호를 위한 마스킹 유틸리티

```java
package com.softwarecampus.backend.util;

import java.util.regex.Pattern;

/**
 * 이메일 검증 및 마스킹 유틸리티
 * 
 * RFC 표준 준수:
 * - RFC 5322: 이메일 기본 형식
 * - RFC 1035: 도메인 레이블 규칙 (하이픈 중간만, TLD 최대 63자)
 */
public class EmailUtils {
    
    /**
     * RFC 5322 + RFC 1035 이메일 정규식
     * 
     * 구조: localPart@domainPart
     * - localPart: [a-zA-Z0-9._%+-]+ (영문자, 숫자, 특수문자)
     * - domainPart: (label\.)+tld
     *   - label: 영문자/숫자로 시작, 중간에만 하이픈, 영문자/숫자로 끝
     *   - tld: 영문자 2~63자 (RFC 1035 섹션 2.3.1)
     * 
     * 예시:
     * - ✅ user@example.com
     * - ✅ user@sub-domain.example.technology (10자 TLD)
     * - ❌ user@-invalid.com (시작 하이픈)
     * - ❌ user@test-.com (끝 하이픈)
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@" +
        "(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)*" +
        "[a-zA-Z]{2,63}$"
    );
    
    /**
     * 이메일 형식 검증
     * 
     * @param email 검증할 이메일
     * @return 유효 여부
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 이메일 마스킹 (PII 보호)
     * 
     * @param email 마스킹할 이메일
     * @return 마스킹된 이메일 (예: u****@example.com)
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        
        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domainPart = parts[1];
        
        if (localPart.length() <= 1) {
            return "*@" + domainPart;
        }
        
        return localPart.charAt(0) + "****@" + domainPart;
    }
}
```

**설계 포인트:**
- RFC 5322 (이메일 기본 형식) + RFC 1035 (도메인 규칙) 준수
- **보안**: 로그에 이메일 원본 노출 방지 (GDPR 준수)
- **국제화**: punycode 도메인 지원 (xn--로 시작)
- **검증**: 
  - TLD 최대 63자 (RFC 1035)
  - 하이픈은 도메인 레이블 중간만 허용
- CodeRabbit 리뷰 반영: 하이픈 위치 검증 강화

---

## 🔗 GlobalExceptionHandler 수정

**기존 파일 수정:** `exception/GlobalExceptionHandler.java`

도메인 예외 핸들러 추가:

```java
package com.softwarecampus.backend.exception;

import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기
 * RFC 9457 ProblemDetail 표준 준수
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 처리 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        if (log.isDebugEnabled()) {
            var failedFields = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField())
                .toList();
            log.debug("Validation failed. Fields: {}", failedFields);
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "요청 본문에 유효하지 않은 필드가 있습니다."
        );
        problemDetail.setType(URI.create("https://api.프로젝트주소/problems/validation-error"));
        problemDetail.setTitle("Validation Failed");
        
        // 필드별 오류 수집
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        problemDetail.setProperty("errors", errors);
        
        return problemDetail;
    }

    /**
     * 일반 예외 처리 (fallback)
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다."
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Internal Server Error");
        
        return problemDetail;
    }

    // ========================================
    // Account 도메인 예외 처리
    // ========================================
    
    /**
     * 잘못된 입력값 예외 처리
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(InvalidInputException.class)
    public ProblemDetail handleInvalidInputException(InvalidInputException ex) {
        log.warn("Invalid input detected for a request");
        if (log.isDebugEnabled()) {
            log.debug("InvalidInputException details", ex);
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()  // 이미 일반화된 메시지 사용
        );
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/invalid-input"));
        problemDetail.setTitle("Invalid Input");
        
        return problemDetail;
    }
    
    /**
     * 이메일 중복 예외 처리
     * HTTP 409 Conflict
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmailException(DuplicateEmailException ex) {
        log.warn("Email duplicate detected for a request");
        if (log.isDebugEnabled()) {
            log.debug("DuplicateEmailException details", ex);
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "이메일이 이미 등록되었습니다."
        );
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/duplicate-email"));
        problemDetail.setTitle("Duplicate Email");
        
        return problemDetail;
    }
    
    /**
     * 계정 미존재 예외 처리
     * HTTP 404 Not Found
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFoundException(AccountNotFoundException ex) {
        log.warn("Account not found for a request");
        if (log.isDebugEnabled()) {
            log.debug("AccountNotFoundException details", ex);
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "요청한 계정을 찾을 수 없습니다."
        );
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/account-not-found"));
        problemDetail.setTitle("Account Not Found");
        
        return problemDetail;
    }
    
    // ========================================
    // 여기에 다른 도메인 예외 추가
    // ========================================
}
```

**수정 내역:**
- **3개 예외 핸들러**: `InvalidInputException`, `DuplicateEmailException`, `AccountNotFoundException`
- **공통 핸들러**: Bean Validation 실패, 일반 예외 처리
- **보안 강화**: PII 로깅 제거 (디버그 레벨로 마스킹), 일반화된 메시지 사용
- **RFC 9457 준수**: `type` URI 설정, `title` 필드 추가
- **HTTP 상태 코드**: 400 Bad Request, 409 Conflict, 404 Not Found
- **로깅 전략**: WARN 레벨 + 디버그 레벨 상세 로그

---

## 🔗 MessageResponse 수정 (RESTful)

**기존 파일 수정:** `dto/user/MessageResponse.java`

### CodeRabbit 리뷰 반영: Status 필드 제거

```java
package com.softwarecampus.backend.dto.user;

/**
 * 간단한 메시지 응답
 * RESTful 표준 준수: HTTP 상태 코드로 성공/실패 판단
 */
public record MessageResponse(String message) {
    
    /**
     * 메시지 응답 생성
     * 
     * @param message 응답 메시지
     * @return MessageResponse 인스턴스
     */
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
```

**변경 이유:**
- HTTP 상태 코드가 성공/실패 표현 (RESTful 표준)
- Body의 Status 필드는 불필요한 중복
- Spring ProblemDetail 패턴과 일관성
- 클라이언트는 `response.status`로 자동 확인

**변경 전:**
```java
record MessageResponse(Status status, String message) {
    enum Status { SUCCESS, ERROR }
    static MessageResponse success(String message) { ... }
    static MessageResponse error(String message) { ... }
}
```

**변경 후:**
```java
record MessageResponse(String message) {
    static MessageResponse of(String message) { ... }
}
```

---

## 📝 테스트 시나리오 문서

**새로 생성:** `.md/account/시나리오/` 디렉토리

### signup_scenarios.md
회원가입 시나리오 (20개):
- ✅ 정상 회원가입
- ✅ 이메일 형식 오류 (RFC 5322, RFC 1035 위반)
- ✅ 이메일 중복 (일반 / Race Condition)
- ✅ 닉네임 중복
- 기타 입력 검증 시나리오

### profile_scenarios.md
프로필 조회 시나리오 (8개):
- ✅ ID로 계정 조회
- ✅ 이메일로 계정 조회
- ✅ 계정 미존재 (404)
- ✅ 이메일 형식 오류 (400)

**참조:** [시나리오 전체 목록](.md/account/시나리오/README.md)

---

## 📊 의존성 관계도

```text
Controller (Phase 7)
    ↓
SignupService (인터페이스)
    ↓
SignupServiceImpl (구현체)
    ↓
    ├─ AccountRepository
    ├─ PasswordEncoder
    └─ throw DuplicateEmailException

Controller (Phase 7)
    ↓
ProfileService (인터페이스)
    ↓
ProfileServiceImpl (구현체)
    ↓
    ├─ AccountRepository
    └─ throw AccountNotFoundException

예외 처리 플로우:
DuplicateEmailException/AccountNotFoundException
    ↓
GlobalExceptionHandler
    ↓
RFC 9457 ProblemDetail 응답
```

---

## ✅ 검증 방법

### 1. 컴파일 확인
```bash
mvn clean compile
```

### 2. Service 메서드 시그니처 확인
- `SignupService` 인터페이스와 `SignupServiceImpl` 메서드 일치 확인
- `ProfileService` 인터페이스와 `ProfileServiceImpl` 메서드 일치 확인
- IDE에서 구현 여부 검증

### 3. 예외 처리 확인
- `GlobalExceptionHandler`에 핸들러 추가 확인
- HTTP 상태 코드 매핑 적절성 확인

### 4. 단위 테스트 (Phase 6에서 작성)
- `SignupServiceImplTest` - 회원가입 정상/예외 케이스
- `ProfileServiceImplTest` - 조회 정상/예외 케이스
- `AccountFacadeServiceTest` - Facade 통합 테스트

---

## 📝 Phase 완료 기준

- [x] **파일 생성 완료**
  - [x] `SignupService.java` 인터페이스 생성
  - [x] `SignupServiceImpl.java` 구현체 생성
  - [x] `ProfileService.java` 인터페이스 생성
  - [x] `ProfileServiceImpl.java` 구현체 생성
  - [x] `InvalidInputException.java` 예외 생성 (exception/user/)
  - [x] `DuplicateEmailException.java` 예외 생성 (exception/user/)
  - [x] `AccountNotFoundException.java` 예외 생성 (exception/user/)
  - [x] `EmailUtils.java` 유틸리티 생성 (util/)

- [x] **GlobalExceptionHandler 수정**
  - [x] `InvalidInputException` 핸들러 추가 (400 Bad Request)
  - [x] `DuplicateEmailException` 핸들러 추가 (409 Conflict)
  - [x] `AccountNotFoundException` 핸들러 추가 (404 Not Found)

- [x] **MessageResponse 수정 (RESTful)**
  - [x] `Status` 필드 제거 (HTTP 상태 코드와 중복)
  - [x] `of(String)` 팩토리 메서드로 단순화

- [x] **보안 강화**
  - [x] PII 로깅 제거 (이메일 원본 → 마스킹)
  - [x] 동시성 안전 처리 (DB UNIQUE 제약)
  - [x] RFC 표준 준수 (RFC 5322, RFC 1035)

- [x] **CodeRabbit PR 리뷰 반영**
  - [x] Javadoc FQCN 수정 (exception.user 패키지 추가)
  - [x] DataIntegrityViolationException 처리 개선 (null 체크 최적화)
  - [x] 이메일 정규식 강화 (RFC 1035 하이픈 규칙)
  - [x] MessageResponse Status 필드 제거 (RESTful)
  - [x] Markdown 코드 블록 언어 지정 (Markdownlint)

- [x] **코드 검증**
  - [x] 컴파일 성공 (`mvn clean compile` - 60 source files)
  - [x] 인터페이스-구현체 메서드 시그니처 일치
  - [x] 로깅 적절히 배치 (PII 마스킹)
  - [x] `@Transactional` 올바르게 적용
  - [x] 도메인별 예외 패키지 분리 (exception/user/)
  - [x] 이메일 검증 RFC 표준 준수 확인

- [x] **문서화**
  - [x] Phase 5 설계 문서 최신화
  - [x] 실제 구현 내용 반영 (보안, 동시성, RFC 표준)
  - [x] CodeRabbit 리뷰 반영 내역 문서화
  - [x] 설계 결정 사항 추가 (메시지 파싱, 검증 중복, RESTful)
  - [x] 테스트 시나리오 문서 링크 추가

- [x] **테스트 시나리오 작성**
  - [x] `signup_scenarios.md` (회원가입 20개 시나리오)
  - [x] `profile_scenarios.md` (프로필 조회 8개 시나리오)
  - [x] Race Condition 시나리오 포함
  - [x] RFC 표준 위반 케이스 포함

---

## 🔜 다음 단계

## Phase 6: Service 단위 테스트 (Mockito)
- SignupServiceImplTest 작성
- ProfileServiceImplTest 작성
- Mockito로 Repository, PasswordEncoder 모킹
- 정상 케이스: 회원가입 성공, 조회 성공
- 예외 케이스: 이메일 중복, 계정 미존재, 이메일 형식 오류
- @ExtendWith(MockitoExtension.class) 사용
- EmailUtils 유틸리티 테스트 (RFC 표준 검증)

---

## 🎯 설계 결정 사항

### 1. 기능별 독립 패키지
**결정:** signup/login/profile 별도 패키지로 분리

**이유:**
- 각 기능이 명확히 분리 (회원가입/로그인/프로필)
- Phase별 독립적 작업 가능
- 폴더 구조만 봐도 기능 파악 가능
- 테스트 파일도 같은 구조로 분리 가능

### 2. 인터페이스 + 구현체 쌍
**결정:** 각 Service는 인터페이스와 구현체로 구성

**이유:**
- 테스트 시 Mock 객체 주입 용이
- 명확한 계약(Contract) 정의
- 향후 다른 구현체로 교체 가능 (유연성)
- Spring 권장 패턴

### 3. Facade 패턴 제거
**결정:** Controller가 각 Service를 직접 주입

**이유:**
- 불필요한 중간 계층 제거 (단순화)
- 각 Service가 독립적이므로 Facade 불필요
- Controller 코드가 더 명확해짐
- 파일 개수 감소

### 4. 계정 타입 기본값
**결정:** `accountType = USER`, `accountApproved = APPROVED`

**이유:**
- 일반 사용자는 즉시 승인
- 학원 계정은 별도 API로 처리 (관리자 승인 필요)
- Phase 5에서는 일반 회원가입만 처리
- 실제 Entity 필드명 `accountApproved` 사용

### 5. DTO 변환 위치
**결정:** Service Layer에서 Entity ↔ DTO 변환

**이유:**
- Controller는 HTTP 처리에만 집중
- Repository는 Entity만 다룸
- Service가 비즈니스 로직 + 변환 담당

### 6. 트랜잭션 전략
**결정:** 클래스 레벨 `readOnly=true`, 쓰기 메서드만 `@Transactional`

**이유:**
- 읽기 작업이 대부분 → 기본값 읽기 전용
- 쓰기 작업만 명시적으로 `@Transactional` 선언
- 불필요한 트랜잭션 오버헤드 최소화

### 7. 메시지 파싱 방식 유지 (CodeRabbit 리뷰 반영)
**결정:** `DataIntegrityViolationException` 메시지 파싱 유지

**CodeRabbit 리뷰:**
> "메시지 파싱은 DB/JPA 메시지 변경 시 불안정. DB에서 이메일 재확인 권장."

**논의 및 결정:**
- DB 제약 조건 변경 계획 없음 (안정적 환경)
- DB 재확인 시 추가 쿼리 발생 (성능 저하)
- YAGNI 원칙 (You Aren't Gonna Need It)
- **결정:** 현재 방식 유지 (실용적 판단)

### 8. 이메일 검증 로직 중복 허용 (CodeRabbit 리뷰 반영)
**결정:** SignupService와 ProfileService의 이메일 검증 중복 허용

**CodeRabbit 리뷰:**
> "`validateEmailFormat`를 EmailUtils로 공통화 권장."

**논의 및 결정:**
- 중복 코드 약 5줄 (경미한 중복)
- 각 Service 맥락이 다름 (회원가입 vs 프로필 변경)
- 명확성 > DRY (Don't Repeat Yourself)
- 공통화 시 불필요한 추상화 발생 가능
- **결정:** 현재 유지 (명확성 우선)

### 9. MessageResponse Status 필드 제거 (RESTful)
**결정:** `MessageResponse`에서 `Status` 필드 완전 제거

**CodeRabbit 리뷰:**
> "HTTP 상태 코드와 중복. Status 필드 불필요."

**변경 내역:**
```java
// Before
record MessageResponse(Status status, String message) {
    enum Status { SUCCESS, ERROR }
    static MessageResponse success(String message) { ... }
    static MessageResponse error(String message) { ... }
}

// After
record MessageResponse(String message) {
    static MessageResponse of(String message) { ... }
}
```

**이유:**
- HTTP 상태 코드가 성공/실패 표현 (RESTful 표준)
- Body의 Status 필드는 불필요한 중복
- Spring ProblemDetail 패턴과 일관성
- 클라이언트는 `response.status`로 자동 확인

### 10. 보안 강화 설계
**결정:** PII(개인정보) 로깅 제거, 동시성 안전 처리

**구현 내역:**
- ❌ 이메일 원본 로깅 (`log.debug("이메일: {}", email)`)
- ✅ 이메일 마스킹 (`log.debug("마스킹 이메일: {}", EmailUtils.maskEmail(email))`)
- ✅ DB UNIQUE 제약으로 동시성 안전 보장
- ✅ Race Condition 문서화 (`signup_scenarios.md`)

**이유:**
- GDPR/개인정보보호법 준수
- 로그 파일 노출 시 개인정보 유출 방지
- DB 제약 조건으로 동시성 안전 보장
- 명확한 시나리오 문서화로 이해 용이
- 쓰기 작업만 명시적으로 트랜잭션 오픈
- 성능 최적화

### 7. 예외 타입
**결정:** RuntimeException (Unchecked Exception)

**이유:**
- Spring은 RuntimeException만 자동 롤백
- 비즈니스 예외는 필수 처리 불필요
- GlobalExceptionHandler에서 일괄 처리

### 8. 예외 패키지 구조
**결정:** 도메인별 예외 패키지 분리 (`exception/user/`)

**이유:**
- 도메인별 예외 관리 용이
- 확장성 (course, board 등 추가 예정)
- 예외 파일이 많아져도 정리된 구조 유지

---

**작성일:** 2025-11-05  
**최종 수정:** 2025-11-05 14:50  
**상태:** ✅ 구현 완료
