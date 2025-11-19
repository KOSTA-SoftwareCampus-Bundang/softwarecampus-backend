# 2. ProfileServiceImplTest 구현

**경로:** `test/java/com/softwarecampus/backend/service/user/profile/ProfileServiceImplTest.java`

**설명:** 프로필 조회 Service 단위 테스트

---

## 📋 테스트 개요

ProfileServiceImpl의 조회 로직을 검증합니다:
- ID로 계정 조회 (`getAccountById`)
- 이메일로 계정 조회 (`getAccountByEmail`)
- 존재하지 않는 계정 처리
- Soft Delete 미적용 계정 조회

---

## 🔧 전체 코드

```java
package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
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
    
    private Account testAccount;
    
    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
            .id(1L)
            .email("user@example.com")
            .password("encodedPassword")
            .userName("홍길동")
            .phoneNumber("010-1234-5678")
            .address("서울시 강남구")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
    }
    
    @Test
    @DisplayName("ID로 계정 조회 - 성공")
    void getAccountById_성공() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        
        // When
        AccountResponse response = profileService.getAccountById(1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.userName()).isEqualTo("홍길동");
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(response.accountType()).isEqualTo(AccountType.USER);
        assertThat(response.accountApproved()).isEqualTo(ApprovalStatus.APPROVED);
        
        // 메서드 호출 검증
        verify(accountRepository).findById(1L);
    }
    
    @Test
    @DisplayName("ID로 계정 조회 - 존재하지 않는 계정")
    void getAccountById_존재하지않음() {
        // Given
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> profileService.getAccountById(999L))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessage("계정을 찾을 수 없습니다.");
        
        // findById는 호출되어야 함
        verify(accountRepository).findById(999L);
    }
    
    @Test
    @DisplayName("이메일로 계정 조회 - 성공")
    void getAccountByEmail_성공() {
        // Given
        when(accountRepository.findByEmail("user@example.com"))
            .thenReturn(Optional.of(testAccount));
        
        // When
        AccountResponse response = profileService.getAccountByEmail("user@example.com");
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.userName()).isEqualTo("홍길동");
        
        // 메서드 호출 검증
        verify(accountRepository).findByEmail("user@example.com");
    }
    
    @Test
    @DisplayName("이메일로 계정 조회 - 존재하지 않는 계정")
    void getAccountByEmail_존재하지않음() {
        // Given
        when(accountRepository.findByEmail("none@example.com"))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> profileService.getAccountByEmail("none@example.com"))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessage("계정을 찾을 수 없습니다.");
        
        // findByEmail은 호출되어야 함
        verify(accountRepository).findByEmail("none@example.com");
    }
    
    @Test
    @DisplayName("Soft Delete 계정 조회 - 실패 (Repository 레벨에서 필터링)")
    void getAccountById_SoftDeleted() {
        // Given
        // Soft Delete된 계정은 Repository.findById()에서 Optional.empty() 반환
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> profileService.getAccountById(1L))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessage("계정을 찾을 수 없습니다.");
        
        // 설명: @Where(clause = "deleted_at IS NULL")로 인해
        // Soft Delete된 계정은 JPA 쿼리에서 자동 제외됨
        verify(accountRepository).findById(1L);
    }
    
    @Test
    @DisplayName("계정 응답 DTO 변환 검증")
    void AccountResponse_DTO변환() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        
        // When
        AccountResponse response = profileService.getAccountById(1L);
        
        // Then - 모든 필드 매핑 확인
        assertThat(response.id()).isEqualTo(testAccount.getId());
        assertThat(response.email()).isEqualTo(testAccount.getEmail());
        assertThat(response.userName()).isEqualTo(testAccount.getUserName());
        assertThat(response.phoneNumber()).isEqualTo(testAccount.getPhoneNumber());
        assertThat(response.address()).isEqualTo(testAccount.getAddress());
        assertThat(response.affiliation()).isEqualTo(testAccount.getAffiliation());
        assertThat(response.position()).isEqualTo(testAccount.getPosition());
        assertThat(response.accountType()).isEqualTo(testAccount.getAccountType());
        assertThat(response.accountApproved()).isEqualTo(testAccount.getAccountApproved());
        
        // password는 응답에 포함되지 않음 (AccountResponse에 password 필드 없음)
    }
}
```

---

## 📊 테스트 시나리오

| 번호 | 테스트명 | 검증 내용 | 예상 결과 |
|------|----------|----------|----------|
| 1 | getAccountById_성공 | ID로 계정 조회 성공 | AccountResponse 반환 |
| 2 | getAccountById_존재하지않음 | 존재하지 않는 ID | AccountNotFoundException |
| 3 | getAccountByEmail_성공 | 이메일로 계정 조회 성공 | AccountResponse 반환 |
| 4 | getAccountByEmail_존재하지않음 | 존재하지 않는 이메일 | AccountNotFoundException |
| 5 | getAccountById_SoftDeleted | Soft Delete 계정 | Optional.empty() |
| 6 | AccountResponse_DTO변환 | DTO 필드 매핑 | 모든 필드 일치 |

---

## 🎯 검증 포인트

### 1. Optional 처리
```java
// Repository는 Optional.empty() 반환
when(accountRepository.findById(999L)).thenReturn(Optional.empty());

// Service는 AccountNotFoundException 발생
assertThatThrownBy(() -> service.getAccountById(999L))
    .isInstanceOf(AccountNotFoundException.class);
```

### 2. DTO 변환
```java
// Entity → DTO 매핑 검증
assertThat(response.id()).isEqualTo(entity.getId());
assertThat(response.email()).isEqualTo(entity.getEmail());
```

### 3. Soft Delete 처리
```java
// @Where(clause = "deleted_at IS NULL") 적용 확인
// Soft Delete된 계정은 findById()에서 자동 제외됨
when(accountRepository.findById(1L)).thenReturn(Optional.empty());
```

---

## 📝 주요 패턴

### Optional.empty() 테스트
```java
// Given
when(repository.findById(999L)).thenReturn(Optional.empty());

// When & Then
assertThatThrownBy(() -> service.getById(999L))
    .isInstanceOf(NotFoundException.class)
    .hasMessage("찾을 수 없습니다.");
```

### DTO 필드 전체 검증
```java
// Entity의 모든 필드가 DTO에 올바르게 매핑되는지 확인
assertThat(dto.field1()).isEqualTo(entity.getField1());
assertThat(dto.field2()).isEqualTo(entity.getField2());
// ... 모든 필드
```

---

## ✅ 완료 체크리스트

- [ ] Mock 설정 (`@Mock`, `@InjectMocks`)
- [ ] ID 조회 성공/실패 케이스
- [ ] 이메일 조회 성공/실패 케이스
- [ ] Optional.empty() 처리
- [ ] AccountNotFoundException 발생 확인
- [ ] DTO 변환 검증 (모든 필드)
- [ ] Soft Delete 계정 제외 확인
- [ ] `verify()` 행위 검증
- [ ] Given-When-Then 구조

---

## 🔗 관련 문서

- [SignupServiceImplTest](01_signup_service_test.md) - 회원가입 테스트
- [Mockito 패턴](04_mockito_patterns.md) - Mock 사용법
