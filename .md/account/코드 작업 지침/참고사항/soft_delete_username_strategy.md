# Soft Delete 환경에서 개인정보 재사용 전략

> **최종 업데이트**: 2025-12-01  
> **적용 범위**: Account 엔티티의 email, userName, phoneNumber 필드  
> **정책**: 모든 개인정보 재사용 허용

---

## 📋 정책 요약

**기존 정책 (변경 전)**:
- ✅ userName: Soft Delete 고려 → 재사용 가능
- ❌ email, phoneNumber: Soft Delete 미고려 → 재사용 불가

**새 정책 (2025-12-01 적용)**:
- ✅ **email**: Soft Delete 고려 → 재사용 가능
- ✅ **userName**: Soft Delete 고려 → 재사용 가능
- ✅ **phoneNumber**: Soft Delete 고려 → 재사용 가능

---

## 🎯 변경 배경

### 1. 사용자 경험 개선
- 탈퇴 후 재가입 시 동일한 이메일/전화번호 사용 가능
- 개인정보 재활용으로 편의성 향상

### 2. Soft Delete 정책 일관성
- 모든 개인정보 필드에 대해 동일한 정책 적용
- 삭제된 계정은 "존재하지 않는 것"으로 간주

### 3. GDPR 준수
- 삭제된 계정의 정보는 재사용 가능
- 필요시 물리적 삭제 정책 추가 가능

---

## 🔧 구현 방법

### 1. Repository 메서드 변경

#### Before (Soft Delete 미고려)
```java
// ❌ 삭제된 계정도 포함하여 중복 체크
boolean existsByEmail(String email);
boolean existsByPhoneNumber(String phoneNumber);
boolean existsByUserName(String userName);
```

#### After (Soft Delete 고려)
```java
// ✅ 활성 계정만 중복 체크
boolean existsByEmailAndIsDeletedFalse(String email);
boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);
boolean existsByUserNameAndIsDeletedFalse(String userName);
```

### 2. Service 레이어 변경

#### SignupServiceImpl.java
```java
@Override
public boolean isEmailAvailable(String email) {
    // 활성 계정만 체크
    return !accountRepository.existsByEmailAndIsDeletedFalse(email);
}
```

#### ProfileServiceImpl.java
```java
// 전화번호 중복 검증 (변경하는 경우에만)
if (request.getPhoneNumber() != null &&
        !request.getPhoneNumber().equals(account.getPhoneNumber())) {
    
    // 활성 계정만 체크
    if (accountRepository.existsByPhoneNumberAndIsDeletedFalse(request.getPhoneNumber())) {
        throw new PhoneNumberAlreadyExistsException(request.getPhoneNumber());
    }
}
```

---

## 📊 Repository 계약 정리

### 조회 메서드 (활성 계정만)
```java
Optional<Account> findByEmailAndIsDeletedFalse(String email);
Optional<Account> findByUserNameAndIsDeletedFalse(String userName);
List<Account> findByAccountTypeAndIsDeletedFalse(AccountType type);
Page<Account> findByIsDeletedFalse(Pageable pageable);
```

### 중복 체크 메서드 (활성 계정만)
```java
boolean existsByEmailAndIsDeletedFalse(String email);
boolean existsByUserNameAndIsDeletedFalse(String userName);
boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);
```

### 검색 메서드 (JPQL)
```java
@Query("SELECT a FROM Account a " +
       "WHERE a.isDeleted = false AND " +
       "(:keyword IS NULL OR " +
       "LOWER(a.userName) LIKE %:keyword% OR " +
       "LOWER(a.email) LIKE %:keyword% OR " +
       "a.phoneNumber LIKE %:keyword%)")
Page<Account> searchActiveAccounts(@Param("keyword") String keyword, Pageable pageable);
```

---

## 🧪 테스트 케이스 업데이트

### SignupIntegrationTest.java
```java
@Test
@DisplayName("이메일 중복 확인 - existsByEmailAndIsDeletedFalse() 검증")
void 이메일중복확인_Repository검증() {
    // given - 활성 계정 생성
    Account existingAccount = Account.builder()
            .email("existing@test.com")
            .build();
    accountRepository.save(existingAccount);

    // Repository 직접 검증 (Soft Delete 고려)
    assertThat(accountRepository.existsByEmailAndIsDeletedFalse("existing@test.com"))
            .isTrue();
    assertThat(accountRepository.existsByEmailAndIsDeletedFalse("new@test.com"))
            .isFalse();
}
```

---

## 📝 데이터베이스 제약

### Entity 설정 (변경 없음)
```java
@Table(
    name = "account",
    indexes = {
        @Index(name = "uk_account_email", columnList = "email", unique = true),
        @Index(name = "uk_account_phone", columnList = "phone_number", unique = true),
        @Index(name = "idx_account_username", columnList = "user_name"),
        @Index(name = "idx_account_deleted", columnList = "is_deleted")
    }
)
```

### ⚠️ 주의사항
- `email`과 `phoneNumber`는 여전히 unique 제약을 가짐
- 하지만 애플리케이션 레벨에서 `isDeleted=false`인 계정만 체크
- DB 레벨에서는 물리적 중복 허용 (삭제된 계정 + 활성 계정)

### Partial Index 지원 (선택사항)
PostgreSQL / MySQL 8.0+에서 가능:
```sql
-- 활성 계정만 unique 보장
CREATE UNIQUE INDEX uk_account_email_active 
ON account(email) 
WHERE is_deleted = false;

CREATE UNIQUE INDEX uk_account_phone_active 
ON account(phone_number) 
WHERE is_deleted = false;

CREATE UNIQUE INDEX uk_account_username_active 
ON account(user_name) 
WHERE is_deleted = false;
```

---

## 🔒 보안 고려사항

### 1. 정보 누출 방지
삭제된 계정 조회 시도는 "계정 없음"과 동일하게 처리:

```java
// LoginServiceImpl.java
Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
    .orElseThrow(() -> new InvalidCredentialsException(
        "이메일 또는 비밀번호가 올바르지 않습니다")); // 삭제 여부 노출 안 함
```

### 2. 테스트 Mock 일관성
```java
// ❌ 잘못된 Mock (Repository 계약 위반)
Account deletedAccount = Account.builder().build();
deletedAccount.markDeleted();
when(accountRepository.findByEmailAndIsDeletedFalse(...))
    .thenReturn(Optional.of(deletedAccount));

// ✅ 올바른 Mock
when(accountRepository.findByEmailAndIsDeletedFalse(...))
    .thenReturn(Optional.empty());
```

---

## 📈 장단점 분석

### 장점
- ✅ 사용자 편의성 향상 (재가입 시 동일 정보 사용)
- ✅ Soft Delete 정책 일관성 유지
- ✅ GDPR 등 개인정보 보호 규정 준수
- ✅ 데이터 재활용 가능

### 단점
- ⚠️ 히스토리 추적 복잡도 증가 (동일 이메일의 여러 계정)
- ⚠️ 감사(Audit) 로그 관리 필요
- ⚠️ 물리적 삭제 정책 추가 고려 필요

---

## 🎯 향후 고려사항

### 1. 물리적 삭제 정책
- 일정 기간(예: 1년) 후 물리적 삭제 고려
- 스케줄러를 통한 자동 정리
- 법적 보관 의무 기간 준수

### 2. 감사 로그
- 동일 이메일의 여러 계정 추적
- 탈퇴/재가입 이력 관리
- 보안 이벤트 모니터링

### 3. Race Condition 대응
- `@Transactional`로 기본 동시성 제어
- 필요시 비관적 락(`@Lock`) 추가
- Unique 제약 위반 시 재시도 로직

---

## 📚 관련 커밋

```
303fb42 - fix: Allow email reuse after soft delete
e284efe - fix: Allow phone number reuse after soft delete
49d982e - fix: Correct soft-deleted account test mock
```

---

**작성일**: 2025-10-29  
**최종 업데이트**: 2025-12-01  
**담당자**: GitHub Copilot
````
