# Phase 5: Service Layer + 도메인 예외

**목표:** 회원가입 비즈니스 로직을 처리하는 Service Layer 구현  
**담당자:** 태윤  
**상태:** ✅ 완료 (2025-11-05)

---

## 📋 작업 개요

회원가입의 핵심 비즈니스 로직을 처리하는 Service Layer를 구현합니다. 이메일 중복 체크, 비밀번호 암호화, 엔티티 저장 등의 작업을 수행하며, 발생 가능한 도메인 예외를 함께 정의합니다.

**설계 원칙:**
- 기능별 독립 패키지 (signup/login/profile)
- 각 기능은 인터페이스 + 구현체 쌍으로 구성
- DTO ↔ Entity 변환은 Service Layer에서 처리
- 비즈니스 예외는 도메인 예외로 명확히 표현
- `@Transactional` 적용으로 트랜잭션 보장

---

## 📂 생성 파일

```
src/main/java/com/softwarecampus/backend/
├─ service/
│  └─ user/
│     ├─ signup/
│     │  ├─ SignupService.java              (회원가입 인터페이스)
│     │  └─ SignupServiceImpl.java          (회원가입 구현)
│     └─ profile/
│        ├─ ProfileService.java             (프로필 인터페이스)
│        └─ ProfileServiceImpl.java         (프로필 구현)
└─ exception/
   └─ user/                                  (도메인별 예외 패키지)
      ├─ DuplicateEmailException.java       (이메일 중복 예외)
      └─ AccountNotFoundException.java      (계정 미존재 예외)
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

**경로:** `service/user/signup/SignupServiceImpl.java`

**설명:** 회원가입 비즈니스 로직 전담

```java
package com.softwarecampus.backend.service.user.signup;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.exception.DuplicateEmailException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 Service
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
            .accountApproved(ApprovalStatus.APPROVED)  // 기본값: APPROVED
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
            account.getApprovalStatus(),
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
- 파일 크기 약 100줄

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
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 조회 Service
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
```

**설계 포인트:**
- Phase 5는 조회 기능만 구현 (Phase 6 테스트용)
- Phase 18에서 수정/삭제 기능 추가
- 파일 크기 약 80줄 (간단명료)

---

### 5. DuplicateEmailException.java

**경로:** `exception/user/DuplicateEmailException.java`

**설명:** 이메일 중복 시 발생하는 도메인 예외

```java
package com.softwarecampus.backend.exception.user;

/**
 * 이메일 중복 예외
 * - 회원가입 시 이미 존재하는 이메일로 가입 시도할 때 발생
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
- GlobalExceptionHandler에서 일괄 처리

---

### 6. AccountNotFoundException.java

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

---

## 🔗 GlobalExceptionHandler 수정

**기존 파일 수정:** `exception/GlobalExceptionHandler.java`

도메인 예외 핸들러 추가:

```java
package com.softwarecampus.backend.exception;

import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
// ... 기타 import

/**
 * 이메일 중복 예외 처리
 * HTTP 409 Conflict
 */
@ExceptionHandler(DuplicateEmailException.class)
public ProblemDetail handleDuplicateEmailException(DuplicateEmailException ex) {
    log.warn("이메일 중복: {}", ex.getMessage());
    
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problemDetail.setTitle("Duplicate Email");
    problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/duplicate-email"));
    
    return problemDetail;
}

/**
 * 계정 미존재 예외 처리
 * HTTP 404 Not Found
 */
@ExceptionHandler(AccountNotFoundException.class)
public ProblemDetail handleAccountNotFoundException(AccountNotFoundException ex) {
    log.warn("계정 미존재: {}", ex.getMessage());
    
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setTitle("Account Not Found");
    problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/account-not-found"));
    
    return problemDetail;;
}
```

**HTTP 상태 코드 매핑:**
- `DuplicateEmailException` → `409 Conflict`
- `AccountNotFoundException` → `404 Not Found`

---

## 📊 의존성 관계도

```
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
  - [x] `DuplicateEmailException.java` 예외 생성 (exception/user/)
  - [x] `AccountNotFoundException.java` 예외 생성 (exception/user/)

- [x] **GlobalExceptionHandler 수정**
  - [x] `DuplicateEmailException` 핸들러 추가
  - [x] `AccountNotFoundException` 핸들러 추가

- [x] **코드 검증**
  - [x] 컴파일 성공 (`mvn clean compile`)
  - [x] 인터페이스-구현체 메서드 시그니처 일치
  - [x] 로깅 적절히 배치
  - [x] `@Transactional` 올바르게 적용
  - [x] 도메인별 예외 패키지 분리 (exception/user/)

- [x] **문서화**
  - [x] Phase 5 설계 문서 최신화
  - [x] 실제 구현 내용 반영 (accountApproved 필드명 등)

---

## 🔜 다음 단계

**Phase 6: Service 단위 테스트 (Mockito)**
- SignupServiceImplTest 작성
- ProfileServiceImplTest 작성
- Mockito로 Repository, PasswordEncoder 모킹
- 정상 케이스: 회원가입 성공, 조회 성공
- 예외 케이스: 이메일 중복, 계정 미존재
- @ExtendWith(MockitoExtension.class) 사용

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
