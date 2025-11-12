# Phase 6: Service 단위 테스트 (Mockito)

**목표:** Mockito를 사용한 Service Layer 단위 테스트 작성  
**담당자:** 태윤  
**상태:** 🚧 준비 중

---

## 📋 작업 개요

Phase 5에서 구현한 Service Layer의 비즈니스 로직을 검증하는 단위 테스트를 작성합니다. Mockito를 사용하여 의존성(Repository, PasswordEncoder 등)을 모킹하고, 정상 케이스와 예외 케이스를 모두 테스트합니다.

**테스트 원칙:**
- **단위 테스트**: Service Layer만 격리하여 테스트
- **Mockito 모킹**: 외부 의존성(Repository, PasswordEncoder) 모킹
- **Given-When-Then**: 테스트 구조 명확화
- **예외 케이스**: 정상 케이스뿐만 아니라 예외 상황도 철저히 검증
- **행위 검증**: `verify()`로 메서드 호출 여부 확인

---

## 📂 생성 파일

```text
src/test/java/com/softwarecampus/backend/
├─ service/user/
│  ├─ signup/
│  │  └─ SignupServiceImplTest.java       ✅ 회원가입 Service 테스트
│  └─ profile/
│     └─ ProfileServiceImplTest.java      ✅ 프로필 Service 테스트
└─ util/
   └─ EmailUtilsTest.java                 ✅ 이메일 유틸리티 테스트
```

---

## 🔧 구현 내용

### 1. SignupServiceImplTest.java

**경로:** `test/java/com/softwarecampus/backend/service/user/signup/SignupServiceImplTest.java`

**설명:** 회원가입 Service 단위 테스트

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SignupServiceImpl 단위 테스트
 * 
 * 테스트 대상:
 * - signup(SignupRequest): 회원가입 처리
 * 
 * Mock 대상:
 * - AccountRepository: DB 접근 모킹
 * - PasswordEncoder: 비밀번호 암호화 모킹
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SignupService 단위 테스트")
class SignupServiceImplTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private SignupServiceImpl signupService;
    
    private SignupRequest validRequest;
    private Account savedAccount;
    
    @BeforeEach
    void setUp() {
        validRequest = new SignupRequest(
            "user@example.com",
            "password123",
            "홍길동"
        );
        
        savedAccount = Account.builder()
            .accountId(1L)
            .email("user@example.com")
            .password("encodedPassword")
            .name("홍길동")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
    }
    
    @Test
    @DisplayName("정상 회원가입 - 일반 사용자")
    void signup_성공_일반사용자() {
        // Given
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        
        // When
        AccountResponse response = signupService.signup(validRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.accountType()).isEqualTo(AccountType.USER);
        assertThat(response.accountApproved()).isEqualTo(ApprovalStatus.APPROVED);
        
        // 메서드 호출 검증
        verify(accountRepository).existsByEmail("user@example.com");
        verify(passwordEncoder).encode("password123");
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    @DisplayName("이메일 형식 오류 - RFC 5322 위반")
    void signup_이메일형식오류_예외발생() {
        // Given
        SignupRequest invalidRequest = new SignupRequest(
            "invalid-email",  // @ 없음
            "password123",
            "홍길동"
        );
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(invalidRequest))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("잘못된 이메일 형식입니다.");
        
        // Repository 호출되지 않아야 함
        verify(accountRepository, never()).existsByEmail(anyString());
        verify(accountRepository, never()).save(any(Account.class));
    }
    
    @Test
    @DisplayName("이메일 형식 오류 - RFC 1035 위반 (하이픈 시작)")
    void signup_이메일형식오류_하이픈시작_예외발생() {
        // Given
        SignupRequest invalidRequest = new SignupRequest(
            "user@-invalid.com",  // 도메인 레이블 하이픈 시작
            "password123",
            "홍길동"
        );
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(invalidRequest))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("잘못된 이메일 형식입니다.");
    }
    
    @Test
    @DisplayName("이메일 중복 - existsByEmail() true")
    void signup_이메일중복_예외발생() {
        // Given
        when(accountRepository.existsByEmail(anyString())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(validRequest))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("이미 사용 중인 이메일입니다.");
        
        // PasswordEncoder, save() 호출되지 않아야 함
        verify(passwordEncoder, never()).encode(anyString());
        verify(accountRepository, never()).save(any(Account.class));
    }
    
    @Test
    @DisplayName("이메일 중복 - DataIntegrityViolationException (Race Condition)")
    void signup_동시요청_이메일중복_예외발생() {
        // Given
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new DataIntegrityViolationException(
                "Duplicate entry 'user@example.com' for key 'uk_account_email'"
            ));
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(validRequest))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("이미 사용 중인 이메일입니다.");
        
        // save() 호출은 되어야 함
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    @DisplayName("닉네임 중복 - DataIntegrityViolationException")
    void signup_닉네임중복_예외발생() {
        // Given
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new DataIntegrityViolationException(
                "Duplicate entry '홍길동' for key 'uk_account_name'"
            ));
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(validRequest))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("이미 사용 중인 닉네임입니다.");
    }
    
    @Test
    @DisplayName("비밀번호 암호화 - PasswordEncoder 호출 확인")
    void signup_비밀번호암호화_확인() {
        // Given
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        
        // When
        signupService.signup(validRequest);
        
        // Then
        verify(passwordEncoder).encode("password123");
        
        // 저장되는 Account의 password가 암호화되었는지 확인
        verify(accountRepository).save(argThat(account ->
            account.getPassword().equals("encodedPassword")
        ));
    }
    
    @Test
    @DisplayName("기본값 설정 - USER, APPROVED")
    void signup_기본값설정_확인() {
        // Given
        when(accountRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        
        // When
        signupService.signup(validRequest);
        
        // Then
        verify(accountRepository).save(argThat(account ->
            account.getAccountType() == AccountType.USER &&
            account.getAccountApproved() == ApprovalStatus.APPROVED
        ));
    }
}
```

**테스트 시나리오:**
1. ✅ 정상 회원가입 (일반 사용자)
2. ✅ 이메일 형식 오류 (RFC 5322 위반)
3. ✅ 이메일 형식 오류 (RFC 1035 위반 - 하이픈 시작)
4. ✅ 이메일 중복 (`existsByEmail()` true)
5. ✅ 이메일 중복 (Race Condition - `DataIntegrityViolationException`)
6. ✅ 닉네임 중복 (`uk_account_name`)
7. ✅ 비밀번호 암호화 확인
8. ✅ 기본값 설정 확인 (USER, APPROVED)

---

### 2. ProfileServiceImplTest.java

**경로:** `test/java/com/softwarecampus/backend/service/user/profile/ProfileServiceImplTest.java`

**설명:** 프로필 조회 Service 단위 테스트

```java
package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProfileServiceImpl 단위 테스트
 * 
 * 테스트 대상:
 * - getAccountById(Long): ID로 계정 조회
 * - getAccountByEmail(String): 이메일로 계정 조회
 * 
 * Mock 대상:
 * - AccountRepository: DB 접근 모킹
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService 단위 테스트")
class ProfileServiceImplTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @InjectMocks
    private ProfileServiceImpl profileService;
    
    private Account mockAccount;
    
    @BeforeEach
    void setUp() {
        mockAccount = Account.builder()
            .accountId(1L)
            .email("user@example.com")
            .password("encodedPassword")
            .name("홍길동")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
    }
    
    @Test
    @DisplayName("ID로 계정 조회 - 성공")
    void getAccountById_성공() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(mockAccount));
        
        // When
        AccountResponse response = profileService.getAccountById(1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.name()).isEqualTo("홍길동");
        
        verify(accountRepository).findById(1L);
    }
    
    @Test
    @DisplayName("ID로 계정 조회 - 존재하지 않음")
    void getAccountById_계정미존재_예외발생() {
        // Given
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> profileService.getAccountById(999L))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessage("계정을 찾을 수 없습니다.");
        
        verify(accountRepository).findById(999L);
    }
    
    @Test
    @DisplayName("이메일로 계정 조회 - 성공")
    void getAccountByEmail_성공() {
        // Given
        when(accountRepository.findByEmail("user@example.com"))
            .thenReturn(Optional.of(mockAccount));
        
        // When
        AccountResponse response = profileService.getAccountByEmail("user@example.com");
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("user@example.com");
        
        verify(accountRepository).findByEmail("user@example.com");
    }
    
    @Test
    @DisplayName("이메일로 계정 조회 - 존재하지 않음")
    void getAccountByEmail_계정미존재_예외발생() {
        // Given
        when(accountRepository.findByEmail("notfound@example.com"))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> 
            profileService.getAccountByEmail("notfound@example.com"))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessage("계정을 찾을 수 없습니다.");
        
        verify(accountRepository).findByEmail("notfound@example.com");
    }
    
    @Test
    @DisplayName("이메일로 계정 조회 - 이메일 형식 오류")
    void getAccountByEmail_이메일형식오류_예외발생() {
        // Given
        String invalidEmail = "invalid-email";
        
        // When & Then
        assertThatThrownBy(() -> profileService.getAccountByEmail(invalidEmail))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("잘못된 이메일 형식입니다.");
        
        // Repository 호출되지 않아야 함
        verify(accountRepository, never()).findByEmail(anyString());
    }
    
    @Test
    @DisplayName("이메일로 계정 조회 - RFC 1035 위반 (하이픈 끝)")
    void getAccountByEmail_이메일형식오류_하이픈끝_예외발생() {
        // Given
        String invalidEmail = "user@test-.com";  // 도메인 레이블 하이픈 끝
        
        // When & Then
        assertThatThrownBy(() -> profileService.getAccountByEmail(invalidEmail))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("잘못된 이메일 형식입니다.");
    }
}
```

**테스트 시나리오:**
1. ✅ ID로 계정 조회 - 성공
2. ✅ ID로 계정 조회 - 존재하지 않음 (404)
3. ✅ 이메일로 계정 조회 - 성공
4. ✅ 이메일로 계정 조회 - 존재하지 않음 (404)
5. ✅ 이메일로 계정 조회 - 이메일 형식 오류 (400)
6. ✅ 이메일로 계정 조회 - RFC 1035 위반 (하이픈 끝)

---

### 3. EmailUtilsTest.java

**경로:** `test/java/com/softwarecampus/backend/util/EmailUtilsTest.java`

**설명:** 이메일 검증 및 마스킹 유틸리티 테스트

```java
package com.softwarecampus.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * EmailUtils 단위 테스트
 * 
 * 테스트 대상:
 * - isValidEmail(String): RFC 5322 + RFC 1035 이메일 검증
 * - maskEmail(String): PII 보호를 위한 이메일 마스킹
 */
@DisplayName("EmailUtils 단위 테스트")
class EmailUtilsTest {
    
    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "test.user@example.com",
        "user+tag@example.com",
        "user@sub.example.com",
        "user@sub-domain.example.com",
        "user@example.technology",  // 10자 TLD (RFC 1035)
        "user@xn--bcher-kva.com"    // punycode (국제화 도메인)
    })
    @DisplayName("이메일 검증 - 유효한 이메일")
    void isValidEmail_유효한이메일_true반환(String email) {
        // When
        boolean result = EmailUtils.isValidEmail(email);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-email",            // @ 없음
        "@example.com",            // localPart 없음
        "user@",                   // domainPart 없음
        "user@@example.com",       // @ 중복
        "user@-invalid.com",       // 하이픈 시작 (RFC 1035 위반)
        "user@test-.com",          // 하이픈 끝 (RFC 1035 위반)
        "user@example.c",          // TLD 1자 (최소 2자)
        "user@example." + "a".repeat(64)  // TLD 64자 (최대 63자)
    })
    @DisplayName("이메일 검증 - 무효한 이메일")
    void isValidEmail_무효한이메일_false반환(String email) {
        // When
        boolean result = EmailUtils.isValidEmail(email);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("이메일 검증 - null")
    void isValidEmail_null_false반환() {
        // When
        boolean result = EmailUtils.isValidEmail(null);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("이메일 검증 - 빈 문자열")
    void isValidEmail_빈문자열_false반환() {
        // When
        boolean result = EmailUtils.isValidEmail("");
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("이메일 마스킹 - 정상")
    void maskEmail_정상() {
        // Given
        String email = "user@example.com";
        
        // When
        String masked = EmailUtils.maskEmail(email);
        
        // Then
        assertThat(masked).isEqualTo("u****@example.com");
    }
    
    @Test
    @DisplayName("이메일 마스킹 - localPart 1자")
    void maskEmail_localPart1자() {
        // Given
        String email = "u@example.com";
        
        // When
        String masked = EmailUtils.maskEmail(email);
        
        // Then
        assertThat(masked).isEqualTo("*@example.com");
    }
    
    @Test
    @DisplayName("이메일 마스킹 - null")
    void maskEmail_null() {
        // When
        String masked = EmailUtils.maskEmail(null);
        
        // Then
        assertThat(masked).isEqualTo("***");
    }
    
    @Test
    @DisplayName("이메일 마스킹 - @ 없음")
    void maskEmail_골뱅이없음() {
        // Given
        String email = "invalid-email";
        
        // When
        String masked = EmailUtils.maskEmail(email);
        
        // Then
        assertThat(masked).isEqualTo("***");
    }
}
```

**테스트 시나리오:**
1. ✅ 이메일 검증 - 유효한 이메일 (7개 케이스)
   - 기본 형식
   - `.` 포함
   - `+` 태그
   - 서브도메인
   - 하이픈 중간
   - 10자 TLD
   - punycode (국제화)
2. ✅ 이메일 검증 - 무효한 이메일 (8개 케이스)
   - @ 없음
   - localPart 없음
   - domainPart 없음
   - @ 중복
   - 하이픈 시작/끝 (RFC 1035)
   - TLD 길이 위반
3. ✅ 이메일 검증 - null/빈 문자열
4. ✅ 이메일 마스킹 - 정상/1자/null/@ 없음

---

## 🎯 Mockito 패턴

### 1. Given-When-Then 구조

```java
@Test
void 테스트이름() {
    // Given: 테스트 준비 (Mock 설정)
    when(repository.findById(1L)).thenReturn(Optional.of(entity));
    
    // When: 실행
    Result result = service.doSomething(1L);
    
    // Then: 검증
    assertThat(result).isNotNull();
    verify(repository).findById(1L);
}
```

### 2. Mock 설정 (`when()`)

```java
// 값 반환
when(repository.save(any())).thenReturn(savedEntity);

// 예외 발생
when(repository.save(any())).thenThrow(new RuntimeException());

// Optional 반환
when(repository.findById(1L)).thenReturn(Optional.of(entity));
when(repository.findById(999L)).thenReturn(Optional.empty());
```

### 3. 행위 검증 (`verify()`)

```java
// 메서드 호출 확인
verify(repository).save(any());

// 호출 횟수 확인
verify(repository, times(1)).save(any());
verify(repository, never()).delete(any());

// 인자 검증
verify(repository).save(argThat(account ->
    account.getAccountType() == AccountType.USER
));
```

### 4. ArgumentMatchers

```java
any()                    // 모든 타입
any(Account.class)       // Account 타입
anyString()              // String 타입
anyLong()                // Long 타입
eq("value")              // 정확한 값
argThat(predicate)       // 조건 검증
```

---

## 📝 검증 방법

### 1. 테스트 실행

```bash
# 전체 테스트
mvn test

# Service 테스트만 실행
mvn test -Dtest=*ServiceImplTest

# 특정 테스트 클래스만 실행
mvn test -Dtest=SignupServiceImplTest
```

### 2. 커버리지 확인

```bash
# JaCoCo 커버리지 리포트 생성
mvn test jacoco:report

# 리포트 확인
# target/site/jacoco/index.html
```

**커버리지 목표:**
- Line Coverage: 80% 이상
- Branch Coverage: 70% 이상

### 3. 빌드 검증

```bash
mvn clean verify
```

**확인 사항:**
- ✅ 모든 테스트 PASS
- ✅ 빌드 SUCCESS
- ✅ 코드 스타일 검증 통과

---

## 📚 참고 자료

### Mockito 문서
- [Mockito 공식 문서](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Mockito Annotations](https://www.baeldung.com/mockito-annotations)

### AssertJ 문서
- [AssertJ 공식 문서](https://assertj.github.io/doc/)
- [AssertJ Exception Assertions](https://www.baeldung.com/assertj-exception-assertion)

### JUnit 5 문서
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Parameterized Tests](https://www.baeldung.com/parameterized-tests-junit-5)

---

## ✅ 완료 기준

- [ ] **테스트 파일 생성**
  - [ ] `SignupServiceImplTest.java` (8개 테스트)
  - [ ] `ProfileServiceImplTest.java` (6개 테스트)
  - [ ] `EmailUtilsTest.java` (12개 테스트)

- [ ] **정상 케이스 테스트**
  - [ ] 회원가입 성공
  - [ ] ID로 계정 조회 성공
  - [ ] 이메일로 계정 조회 성공
  - [ ] 이메일 검증 성공 (유효한 이메일 7개)
  - [ ] 이메일 마스킹 성공

- [ ] **예외 케이스 테스트**
  - [ ] 이메일 형식 오류 (RFC 5322, RFC 1035)
  - [ ] 이메일 중복 (일반 + Race Condition)
  - [ ] 닉네임 중복
  - [ ] 계정 미존재 (ID, 이메일)
  - [ ] 이메일 검증 실패 (무효한 이메일 8개)

- [ ] **Mockito 패턴 적용**
  - [ ] `@ExtendWith(MockitoExtension.class)` 사용
  - [ ] `@Mock`, `@InjectMocks` 애노테이션 적용
  - [ ] `when()` Mock 설정
  - [ ] `verify()` 행위 검증
  - [ ] `ArgumentMatchers` 활용

- [ ] **테스트 실행 및 검증**
  - [ ] 모든 테스트 PASS (`mvn test`)
  - [ ] 커버리지 80% 이상
  - [ ] 빌드 성공 (`mvn clean verify`)
  - [ ] Given-When-Then 구조 준수

- [ ] **문서화**
  - [ ] Phase 6 설계 문서 작성
  - [ ] 테스트 시나리오 명시
  - [ ] Mockito 패턴 정리

---

## 🔜 다음 단계

**Phase 7: Controller Layer (회원가입 API)**
- `AuthController.java` 작성
- POST /api/v1/auth/signup 엔드포인트 구현
- `@RestController`, `@PostMapping` 사용
- Bean Validation 적용 (`@Valid`)
- HTTP 201 Created + Location 헤더

---

## 📊 테스트 통계

**총 테스트 개수:** 26개
- SignupServiceImplTest: 8개
- ProfileServiceImplTest: 6개
- EmailUtilsTest: 12개

**커버리지 목표:**
- Line Coverage: 80% 이상
- Branch Coverage: 70% 이상
- Method Coverage: 90% 이상

**예상 소요 시간:** 3-4시간
