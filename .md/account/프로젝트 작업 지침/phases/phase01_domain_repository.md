# Phase 1: Domain & Repository ✅ (완료)

**작업 기간:** 2025-10-29  
**상태:** ✅ 완료

---

## 📌 작업 목표
- Account 엔티티 및 관련 Enum 정의
- AccountRepository 쿼리 메서드 구현

---

## 📂 생성된 파일

```
domain/
  ├─ common/
  │   ├─ AccountType.java
  │   └─ ApprovalStatus.java
  └─ user/
      └─ Account.java

repository/
  └─ user/
      └─ AccountRepository.java
```

---

## 🔨 완료된 작업

### ✅ `domain/common/AccountType.java` - Enum
계정 타입 정의 (USER, ACADEMY)

### ✅ `domain/common/ApprovalStatus.java` - Enum  
승인 상태 정의 (PENDING, APPROVED, REJECTED)

### ✅ `domain/user/Account.java` - 엔티티
회원 정보를 담는 JPA 엔티티 (Builder 패턴 포함)

### ✅ `repository/user/AccountRepository.java`
7개 쿼리 메서드:
- `findByEmail(String email)`
- `existsByEmail(String email)`
- `findByUserName(String userName)`
- `findByAccountType(AccountType type)`
- `findByApprovalStatus(ApprovalStatus status)`
- `findByAccountTypeAndApprovalStatus(AccountType type, ApprovalStatus status)`
- `countByAccountType(AccountType type)`

---

## ✅ 검증 방법
- JPA DDL 자동 생성으로 테이블 생성 확인
- Repository 쿼리 메서드 동작 확인

---

## 🔜 다음 단계
Phase 2: GlobalExceptionHandler 기본 틀 작성
