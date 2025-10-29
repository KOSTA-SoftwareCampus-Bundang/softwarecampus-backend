# Soft Delete 환경에서 userName 중복 처리 전략

## 문제 상황

Soft Delete 시 userName이 DB에 남아있어 재가입 불가:

```java
// 1. 사용자 "kim123" 가입
Account account = Account.builder()
    .userName("kim123")
    .isDeleted(false)
    .build();

// 2. 사용자 "kim123" 탈퇴 (Soft Delete)
account.markDeleted();  // isDeleted = true

// 3. 다른 사람이 "kim123"으로 재가입 시도
// ❌ UNIQUE 제약 때문에 실패!
```

---

## 해결 방안

### Entity: userName unique 제약 제거

```java
@Table(
    name = "account",
    indexes = {
        @Index(name = "uk_account_email", columnList = "email", unique = true),
        @Index(name = "uk_account_phone", columnList = "phoneNumber", unique = true),
        @Index(name = "idx_account_username", columnList = "userName"),  // unique 없음
        @Index(name = "idx_account_type_approved", columnList = "account_type,account_approved")
    }
)
public class Account extends BaseSoftDeleteSupportEntity {
    @Column
    private String userName;  // unique 제약 없음
}
```

### Repository: 활성 계정만 중복 체크

```java
/**
 * 활성 사용자명 중복 체크 (Soft Delete 고려)
 * - isDeleted=false인 계정 중에서만 중복 체크
 */
@Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.userName = :userName AND a.isDeleted = false")
boolean existsActiveUserName(@Param("userName") String userName);

/**
 * 사용자명으로 활성 계정 조회
 */
Optional<Account> findByUserNameAndIsDeleted(String userName, Boolean isDeleted);
```

### Service: 비즈니스 로직에서 활성 계정 중복 체크

```java
@Transactional
public void signup(SignupRequest request) {
    // 활성 계정 중복 체크
    if (accountRepository.existsActiveUserName(request.getUserName())) {
        throw new DuplicateUserNameException("이미 사용 중인 사용자명입니다");
    }
    
    Account account = Account.builder()
        .userName(request.getUserName())
        .build();
    
    accountRepository.save(account);
}
```

---

## 결과

- ✅ Soft Delete된 계정의 userName 재사용 가능
- ✅ 활성 계정끼리만 userName 중복 방지
- ✅ email, phoneNumber는 여전히 unique 제약 유지 (재가입 방지)

---

## 주의사항

### Race Condition 최소화
- `@Transactional` 사용으로 동시성 제어
- 필요 시 비관적 락(`@Lock`) 또는 낙관적 락(`@Version`) 추가 고려

### DB별 Partial Index 지원 (선택사항)
MySQL 8.0+ / PostgreSQL에서 지원:
```sql
CREATE UNIQUE INDEX uk_account_username_active 
ON account(userName) 
WHERE isDeleted = false;
```

JPA `@Index`로는 표현 불가 - Flyway/Liquibase로 관리 필요

---

**작성일**: 2025-10-29  
**적용 범위**: Account 엔티티 userName 필드
