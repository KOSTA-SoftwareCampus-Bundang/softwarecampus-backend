# 1. SignupServiceImplTest 구현

**경로:** `test/java/com/softwarecampus/backend/service/user/signup/SignupServiceImplTest.java`

**설명:** 회원가입 Service 단위 테스트

---

## 📋 테스트 개요

SignupServiceImpl의 비즈니스 로직을 검증합니다:
- 정상 회원가입 처리
- 이메일 형식 검증 (RFC 5322, RFC 1035)
- 중복 검사 (이메일, 전화번호)
- Race Condition 처리
- 비밀번호 암호화
- 계정 타입별 승인 상태 (USER: APPROVED, ACADEMY: PENDING)

---

## 🔧 전체 코드

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
    
    private SignupRequest userRequest;
    private SignupRequest academyRequest;
    private Account savedUserAccount;
    private Account savedAcademyAccount;
    
    @BeforeEach
    void setUp() {
        // USER 요청
        userRequest = new SignupRequest(
            "user@example.com",
            "password123",
            "홍길동",
            "010-1234-5678",
            "서울시 강남구",
            null,  // affiliation
            null,  // position
            AccountType.USER,
            null   // academyId
        );
        
        // ACADEMY 요청
        academyRequest = new SignupRequest(
            "academy@example.com",
            "password123",
            "김선생",
            "010-9876-5432",
            "서울시 서초구",
            "소프트웨어 캠퍼스",
            "강사",
            AccountType.ACADEMY,
            100L   // academyId
        );
        
        // USER 저장 결과
        savedUserAccount = Account.builder()
            .id(1L)
            .email("user@example.com")
            .password("encodedPassword")
            .userName("홍길동")
            .phoneNumber("010-1234-5678")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
        
        // ACADEMY 저장 결과
        savedAcademyAccount = Account.builder()
            .id(2L)
            .email("academy@example.com")
            .password("encodedPassword")
            .userName("김선생")
            .phoneNumber("010-9876-5432")
            .accountType(AccountType.ACADEMY)
            .academyId(100L)
            .accountApproved(ApprovalStatus.PENDING)
            .build();
    }
    
    @Test
    @DisplayName("정상 회원가입 - USER (즉시 승인)")
    void signup_성공_USER() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(savedUserAccount);
        
        // When
        AccountResponse response = signupService.signup(userRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.userName()).isEqualTo("홍길동");
        assertThat(response.accountType()).isEqualTo(AccountType.USER);
        assertThat(response.accountApproved()).isEqualTo(ApprovalStatus.APPROVED);
        
        // 메서드 호출 검증
        verify(passwordEncoder).encode("password123");
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    @DisplayName("정상 회원가입 - ACADEMY (승인 대기)")
    void signup_성공_ACADEMY() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(savedAcademyAccount);
        
        // When
        AccountResponse response = signupService.signup(academyRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountType()).isEqualTo(AccountType.ACADEMY);
        assertThat(response.accountApproved()).isEqualTo(ApprovalStatus.PENDING);
        
        // academyId 검증
        verify(accountRepository).save(argThat(account ->
            account.getAcademyId() != null && account.getAcademyId().equals(100L)
        ));
    }
    
    @Test
    @DisplayName("이메일 형식 오류 - @ 없음 (RFC 5322 위반)")
    void signup_이메일형식오류_골뱅이없음() {
        // Given
        SignupRequest invalidRequest = new SignupRequest(
            "invalid-email",  // @ 없음
            "password123",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(invalidRequest))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("올바른 이메일 형식이 아닙니다.");
        
        // Repository 호출되지 않아야 함
        verify(accountRepository, never()).save(any(Account.class));
    }
    
    @Test
    @DisplayName("이메일 형식 오류 - 하이픈 시작 (RFC 1035 위반)")
    void signup_이메일형식오류_하이픈시작() {
        // Given
        SignupRequest invalidRequest = new SignupRequest(
            "user@-invalid.com",  // 도메인 레이블 하이픈 시작
            "password123",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(invalidRequest))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("올바른 이메일 형식이 아닙니다.");
    }
    
    @Test
    @DisplayName("이메일 중복 - DataIntegrityViolationException (Race Condition)")
    void signup_이메일중복_RaceCondition() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new DataIntegrityViolationException(
                "Duplicate entry 'user@example.com' for key 'UK_ACCOUNT_EMAIL'"
            ));
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(userRequest))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessage("이미 사용 중인 이메일입니다.");
        
        // save() 호출은 되어야 함
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    @DisplayName("전화번호 중복 - DataIntegrityViolationException")
    void signup_전화번호중복() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new DataIntegrityViolationException(
                "Duplicate entry '010-1234-5678' for key 'UK_ACCOUNT_PHONE'"
            ));
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(userRequest))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("이미 사용 중인 전화번호입니다.");
    }
    
    @Test
    @DisplayName("ACADEMY 타입 - academyId 필수 검증")
    void signup_ACADEMY_academyId없음() {
        // Given
        SignupRequest invalidRequest = new SignupRequest(
            "academy@example.com",
            "password123",
            "김선생",
            "010-9876-5432",
            null,
            "소프트웨어 캠퍼스",
            "강사",
            AccountType.ACADEMY,
            null  // academyId 없음!
        );
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(invalidRequest))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("기관 회원은 기관 ID가 필수입니다.");
    }
    
    @Test
    @DisplayName("ADMIN 타입 - 회원가입 차단")
    void signup_ADMIN_차단() {
        // Given
        SignupRequest adminRequest = new SignupRequest(
            "admin@example.com",
            "password123",
            "관리자",
            "010-0000-0000",
            null, null, null,
            AccountType.ADMIN,  // ADMIN 타입!
            null
        );
        
        // When & Then
        assertThatThrownBy(() -> signupService.signup(adminRequest))
            .isInstanceOf(InvalidInputException.class)
            .hasMessage("관리자 계정은 회원가입으로 생성할 수 없습니다.");
    }
    
    @Test
    @DisplayName("비밀번호 암호화 확인")
    void signup_비밀번호암호화() {
        // Given
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenReturn(savedUserAccount);
        
        // When
        signupService.signup(userRequest);
        
        // Then
        verify(passwordEncoder).encode("password123");
        
        // 저장되는 Account의 password가 암호화되었는지 확인
        verify(accountRepository).save(argThat(account ->
            account.getPassword().equals("encodedPassword")
        ));
    }
}
```

---

## 📊 테스트 시나리오

| 번호 | 테스트명 | 검증 내용 | 예상 결과 |
|------|----------|----------|----------|
| 1 | signup_성공_USER | USER 회원가입 성공 | APPROVED |
| 2 | signup_성공_ACADEMY | ACADEMY 회원가입 성공 | PENDING |
| 3 | signup_이메일형식오류_골뱅이없음 | @ 없는 이메일 | InvalidInputException |
| 4 | signup_이메일형식오류_하이픈시작 | RFC 1035 위반 | InvalidInputException |
| 5 | signup_이메일중복_RaceCondition | DB 제약 위반 | DuplicateEmailException |
| 6 | signup_전화번호중복 | 전화번호 중복 | InvalidInputException |
| 7 | signup_ACADEMY_academyId없음 | ACADEMY academyId 필수 | InvalidInputException |
| 8 | signup_ADMIN_차단 | ADMIN 회원가입 차단 | InvalidInputException |
| 9 | signup_비밀번호암호화 | PasswordEncoder 호출 | 암호화 검증 |

---

## 🎯 검증 포인트

### 1. Mock 설정
```java
when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
```

### 2. 행위 검증
```java
verify(passwordEncoder).encode("password123");
verify(accountRepository).save(any(Account.class));
verify(accountRepository, never()).save(any());  // 호출 안 됨
```

### 3. 인자 검증
```java
verify(accountRepository).save(argThat(account ->
    account.getAccountType() == AccountType.USER &&
    account.getAccountApproved() == ApprovalStatus.APPROVED
));
```

---

## 📝 주요 패턴

### Given-When-Then
```java
// Given: 테스트 준비
when(repository.findById(1L)).thenReturn(Optional.of(entity));

// When: 실행
Result result = service.doSomething(1L);

// Then: 검증
assertThat(result).isNotNull();
verify(repository).findById(1L);
```

### 예외 검증
```java
assertThatThrownBy(() -> service.doSomething())
    .isInstanceOf(CustomException.class)
    .hasMessage("에러 메시지");
```

---

## ✅ 완료 체크리스트

- [ ] Mock 설정 (`@Mock`, `@InjectMocks`)
- [ ] 정상 케이스 테스트 (USER, ACADEMY)
- [ ] 이메일 형식 검증 (RFC 5322, RFC 1035)
- [ ] 중복 검사 (이메일, 전화번호)
- [ ] ACADEMY academyId 필수 검증
- [ ] ADMIN 회원가입 차단
- [ ] 비밀번호 암호화 검증
- [ ] `verify()` 행위 검증
- [ ] Given-When-Then 구조
