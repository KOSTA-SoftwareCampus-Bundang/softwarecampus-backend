# Account 엔티티 및 레포지토리 설계

> 📌 **작성 원칙**: JPA Entity-First 방식 - 엔티티 코드를 먼저 작성하고, DDL은 나중에 생성  
> 📌 **작성 범위**: Account 도메인(로그인/회원가입/마이페이지/보안) 담당 부분만 작성

---

## 📋 문서 메타데이터

| 항목 | 값 |
|------|------|
| **문서 상태** | 🗂️ **SUPERSEDED** (대체됨) |
| **최초 작성일** | 2025-10-29 |
| **마지막 수정일** | 2025-12-01 |
| **보관 위치** | `.md/account/끝난거/` (아카이브) |
| **대체 문서** | `soft_delete_username_strategy.md` (2025-12-01) |
| **보존 이유** | 초기 설계 기록 및 변경 이력 추적 |

### ⚠️ 중요 공지

**이 문서는 2025-12-01부로 공식 참조 문서에서 제외되었습니다.**

- ✅ **초기 설계 (2025-10-29)**: userName만 Soft Delete 후 재사용 가능
- 🔄 **변경된 정책 (2025-12-01)**: 모든 개인정보(email, phoneNumber, userName) 재사용 가능
- 📘 **최신 문서**: `코드 작업 지침/참고사항/soft_delete_username_strategy.md`

**이 문서의 용도:**
- 초기 설계 과정 이해
- 변경 전후 비교
- 히스토리 추적

**실제 구현 참조 시:**
- 반드시 `soft_delete_username_strategy.md` 참조
- 본 문서는 참고용으로만 사용

---

## 1. 현재 Account 엔티티 구조

### 1.1 최종 수정된 필드 구조

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "account")
public class Account extends BaseSoftDeleteSupportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ===== 기존 필드 (유지) =====
    @Column
    private String userName;              // 사용자명 (표시명)
    
    @Column(nullable = false)
    private String password;              // 암호화된 비밀번호
    
    @Column(nullable = false, unique = true)
    private String email;                 // 이메일 (로그인 ID)
    
    @Column(nullable = false, unique = true)
    private String phoneNumber;           // 전화번호
    
    // ===== 수정된 필드 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;      // 계정 유형 (USER/ACADEMY/ADMIN) - 기존 role에서 변경
    
    private String affiliation;           // 소속 (회사/학교 등) - 기존 company에서 변경
    
    private String position;              // 직책/역할 - 기존 department에서 변경
    
    // ===== 새로 추가된 필드 =====
    private String address;               // 주소
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_approved")
    private ApprovalStatus accountApproved;  // 승인 상태 (기관 계정용)
    
    // ===== 향후 추가 예정 (다른 도메인 작업 후) =====
    // academy_id는 추후 Academy 엔티티 생성 시 추가 예정
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "academy_id")
    // private Academy academy;
}
```

### 1.2 필드 변경 내역

| 기존 필드명 | 변경 후 | 변경 이유 |
|------------|---------|-----------|
| `role` | `accountType` (Enum) | USER/ACADEMY/ADMIN 구분을 위한 Enum 타입 |
| `company` | `affiliation` | 회사뿐 아니라 학교, 기관 등도 포함 |
| `department` | `position` | 부서보다는 직책/역할이 더 정확한 표현 |
| - | `address` | 사용자 주소 추가 |
| - | `accountApproved` (Enum) | 기관 계정 승인 상태 관리 |

### 1.3 필요한 Enum 클래스

#### AccountType.java
```java
package com.softwarecampus.backend.domain.common;

public enum AccountType {
    USER,      // 일반 사용자
    ACADEMY,   // 기관 계정
    ADMIN      // 관리자
}
```

#### ApprovalStatus.java
```java
package com.softwarecampus.backend.domain.common;

public enum ApprovalStatus {
    PENDING,   // 승인 대기
    APPROVED,  // 승인 완료
    REJECTED   // 승인 거부
}
```

> ⚠️ **주의**: 이 Enum 클래스들이 아직 없다면 `domain/common/` 폴더에 생성해야 합니다.

### 1.4 상속받는 필드 (BaseSoftDeleteSupportEntity)

```java
// BaseTimeEntity로부터 상속
private LocalDateTime createdAt;      // 생성일시
private LocalDateTime updatedAt;      // 수정일시

// BaseSoftDeleteSupportEntity
private Boolean isDeleted;            // 삭제 여부
private LocalDateTime deletedAt;      // 삭제일시
```

---

## 2. SQL 스키마와의 비교 분석

### 2.1 SQL 스키마 (참고용)

```sql
CREATE TABLE `account` (
	`id`	int	NOT NULL,
	`email`	VARCHAR(255)	NULL,
	`account_type`	ENUM	NOT NULL,
	`user_name`	VARCHAR(255)	NULL,        -- 엔티티의 userName 필드 (SQL은 참고용)
	`password`	VARCHAR(255)	NOT NULL,
	`address`	VARCHAR(255)	NULL,
	`affiliation`	VARCHAR(255)	NULL,
	`position`	VARCHAR(255)	NULL,
	`created_at`	TIMESTAMP	NOT NULL,
	`is_deleted`	ENUM	NOT NULL,
	`updated_at`	TIMESTAMP	NOT NULL,
	`deleted_at`	TIMESTAMP	NOT NULL,
	`account_approved`	ENUM	NOT NULL,
	`academy_id`	INT	NULL
);
```

### 2.2 필드 매핑 비교 및 수정 사항

| SQL 컬럼 | 현재 엔티티 필드 | 최종 결정 | 변경 사항 |
|----------|------------------|-----------|-----------|
| `id` | `id` | ✅ 유지 | PK, AUTO_INCREMENT |
| `email` | `email` | ✅ 유지 | UNIQUE, NOT NULL |
| `password` | `password` | ✅ 유지 | NOT NULL |
| `created_at` | `createdAt` | ✅ 유지 | BaseTimeEntity 상속 |
| `updated_at` | `updatedAt` | ✅ 유지 | BaseTimeEntity 상속 |
| `is_deleted` | `isDeleted` | ✅ 유지 | BaseSoftDeleteSupportEntity 상속 |
| `deleted_at` | `deletedAt` | ✅ 유지 | BaseSoftDeleteSupportEntity 상속 |
| `user_name` | `userName` | ✅ 유지 | 사용자명 (표시명) - Entity-First 방식으로 유지 |
| `phone_number` | `phoneNumber` | ✅ 유지 | 전화번호 (Entity-First 방식으로 유지) |
| `account_type` | `role` | 🔄 수정 | **role → accountType** (Enum으로 관리) |
| `address` | - | ➕ 추가 | **address 필드 추가** (사용자 주소) |
| `affiliation` | `company` | 🔄 수정 | **company → affiliation** (회사 소속이 아닐 수도 있음) |
| `position` | `department` | 🔄 수정 | **department → position** (소속 개념) |
| `account_approved` | - | ➕ 추가 | **accountApproved 필드 추가** (기관 승인용 Enum) |
| `academy_id` | 주석 처리 | ⏳ 대기 | Academy 도메인 담당자 작업 대기 |

### 2.3 수정 필요 사항 정리

#### 🔄 필드명 변경
1. **role → accountType**
   - 이유: Enum(USER/ACADEMY/ADMIN)으로 관리
   - 타입: `AccountType` enum

2. **company → affiliation**
   - 이유: 회사 소속이 아닐 수도 있음 (학생, 프리랜서 등)
   - 타입: `String`

3. **department → position**
   - 이유: 소속 개념이 더 정확
   - 타입: `String`

#### ➕ 필드 추가
1. **address**
   - 용도: 사용자 주소
   - 타입: `String`
   - 제약조건: nullable

2. **accountApproved**
   - 용도: 기관 계정 승인 상태
   - 타입: `ApprovalStatus` enum (PENDING/APPROVED/REJECTED)
   - 제약조건: nullable (일반 사용자는 null)

#### ❌ 삭제되는 필드 (없음)
- 기존 필드는 모두 유지

### 2.4 최종 결론

**수정 방침:**
- ✅ **기존 필드 유지**: userName, password, email, phoneNumber
- 🔄 **필드명 변경**: role → accountType, company → affiliation, department → position
- ➕ **필드 추가**: address, accountApproved
- ⏳ **향후 추가**: academy (Academy 도메인 완성 후)

---

## 3. 필드 수정 작업 가이드

### 3.1 단계별 수정 절차

#### Step 1: Enum 클래스 확인/생성
```bash
# domain/common/ 폴더에 이미 존재하는지 확인
# 없다면 생성 필요:
AccountType.java      # USER, ACADEMY, ADMIN
ApprovalStatus.java   # PENDING, APPROVED, REJECTED
```

#### Step 2: Account 엔티티 수정
```java
// 1. import 추가
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;

// 2. 필드명 변경
private String role;         // ❌ 삭제
→ @Enumerated(EnumType.STRING)
  @Column(name = "account_type")
  private AccountType accountType;  // ✅ 추가

private String company;      // ❌ 삭제
→ private String affiliation;      // ✅ 추가

private String department;   // ❌ 삭제
→ private String position;          // ✅ 추가

// 3. 새 필드 추가
private String address;                      // ✅ 추가

@Enumerated(EnumType.STRING)
@Column(name = "account_approved")
private ApprovalStatus accountApproved;      // ✅ 추가
```

#### Step 3: AccountRepository 업데이트
```java
// accountType 관련 쿼리 메소드 추가 가능
Optional<Account> findByAccountTypeAndEmail(AccountType accountType, String email);
List<Account> findByAccountApproved(ApprovalStatus approved);
```

### 3.2 마이그레이션 주의사항

1. **기존 데이터가 있다면**
   - `role` → `accountType` 데이터 변환 필요
   - `company` → `affiliation` 컬럼명 변경
   - `department` → `position` 컬럼명 변경

2. **코드 검색 및 수정**
   ```bash
   # 기존 필드를 사용하는 모든 코드 검색
   grep -r "\.getRole()" 
   grep -r "\.getCompany()"
   grep -r "\.getDepartment()"
   
   # 새 필드명으로 변경
   .getRole() → .getAccountType()
   .getCompany() → .getAffiliation()
   .getDepartment() → .getPosition()
   ```

3. **프론트엔드 협의 필수**
   - API 응답 필드명 변경 사전 공지
   - 프론트엔드 코드 수정 일정 조율

---

## 4. AccountRepository 설계

### 4.1 현재 구현 (최종)

```java
package com.softwarecampus.backend.repository.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Account 엔티티에 대한 Repository
 * - 사용자 계정 CRUD 및 조회 기능
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    /**
     * 이메일로 계정 조회 (로그인)
     */
    Optional<Account> findByEmail(String email);
    
    /**
     * 이메일 중복 체크
     */
    boolean existsByEmail(String email);
    
    /**
     * 활성 사용자명 중복 체크 (Soft Delete 고려)
     * - isDeleted=false인 계정 중에서만 중복 체크
     */
    boolean existsByUserNameAndIsDeletedFalse(String userName);
    
    /**
     * 사용자명으로 활성 계정 조회 (Soft Delete 고려)
     */
    Optional<Account> findByUserNameAndIsDeletedFalse(String userName);
    
    /**
     * 전화번호 중복 체크
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    /**
     * 계정 타입별 활성 계정 조회
     */
    List<Account> findByAccountTypeAndIsDeletedFalse(AccountType accountType);
    
    /**
     * 계정 타입 및 승인 상태별 활성 계정 조회
     */
    List<Account> findByAccountTypeAndAccountApprovedAndIsDeletedFalse(
        AccountType accountType, 
        ApprovalStatus accountApproved
    );
}
```

### 4.2 메소드 설명

> ⚠️ **변경 이력 (2025-12-01)**: 이 섹션은 변경 전후 비교를 위해 보존되었습니다.  
> 아래 내용은 **2025-12-01 업데이트 사항을 요약**한 것이며, **본 문서에는 반영되지 않았습니다**.  
> 실제 구현 시에는 `soft_delete_username_strategy.md`를 참조하세요.

#### 변경 요약 (2025-12-01 커밋 기록)

**변경 전 (본 문서 작성 시점, 2025-10-29):**
- `existsByEmail(String)` - 삭제된 계정도 포함 체크
- `existsByPhoneNumber(String)` - 삭제된 계정도 포함 체크
- `existsByUserNameAndIsDeletedFalse(String)` - 활성 계정만 체크

**변경 후 (2025-12-01 적용됨):**
- `existsByEmailAndIsDeletedFalse(String)` - 활성 계정만 체크 ✅
- `existsByPhoneNumberAndIsDeletedFalse(String)` - 활성 계정만 체크 ✅
- `existsByUserNameAndIsDeletedFalse(String)` - 활성 계정만 체크 (유지)

**관련 커밋:**
- `303fb42` (2025-12-01) - Allow email reuse after soft delete
- `e284efe` (2025-12-01) - Allow phone number reuse after soft delete
- `49d982e` (2025-12-01) - Correct soft-deleted account test mock
- `cb047ff` (2025-12-01) - Update documentation for Soft Delete policy

#### 아래 표는 변경 후 상태 (참고용)

| 메소드명 | 반환 타입 | 용도 | 변경 상태 |
|----------|-----------|------|-----------|
| `findByEmailAndIsDeletedFalse(String)` | `Optional<Account>` | 로그인 시 이메일로 활성 계정 조회 | ✅ 변경됨 (2025-12-01) |
| `existsByEmailAndIsDeletedFalse(String)` | `boolean` | 회원가입 시 이메일 중복 체크 (활성만) | ✅ 변경됨 (2025-12-01) |
| `existsByUserNameAndIsDeletedFalse(String)` | `boolean` | 활성 사용자명 중복 체크 | ✅ Soft Delete 고려 |
| `findByUserNameAndIsDeletedFalse(String)` | `Optional<Account>` | 활성 계정 조회 (사용자명) | ✅ Soft Delete 고려 |
| `existsByPhoneNumberAndIsDeletedFalse(String)` | `boolean` | 회원가입 시 전화번호 중복 체크 (활성만) | ✅ 변경됨 (2025-12-01) |
| `findByAccountTypeAndIsDeletedFalse(AccountType)` | `List<Account>` | 계정 타입별 활성 계정 조회 | ✅ Soft Delete 고려 |
| `findByAccountTypeAndAccountApprovedAndIsDeletedFalse(...)` | `List<Account>` | 타입+승인 상태별 활성 계정 조회 | ✅ Soft Delete 고려 |

### 4.3 Soft Delete 처리 전략 (⚠️ 본 문서 작성 시점 - 2025-10-29)

> ⚠️ **주의**: 이 섹션은 초기 설계 내용이며, 2025-12-01에 정책이 변경되었습니다.  
> 변경된 정책은 아래 "변경된 정책" 섹션을 참조하세요.

**[본 문서 작성 시점] 초기 설계 (2025-10-29)**:
- ✅ userName: Soft Delete 고려 (재가입 가능)
- ❌ email, phoneNumber: Soft Delete 미고려 (재가입 불가)

**Repository 메서드 (초기 설계):**
```java
// userName만 Soft Delete 고려
boolean existsByUserNameAndIsDeletedFalse(String userName);

// email, phoneNumber는 Soft Delete 미고려
boolean existsByEmail(String email);
boolean existsByPhoneNumber(String phoneNumber);
```

### 4.4 변경된 정책 (2025-12-01 적용) - 요약

> 📘 **최신 정보**: 아래 내용은 변경 사항을 요약한 것입니다.  
> 전체 내용은 `soft_delete_username_strategy.md` (2025-12-01 업데이트)를 참조하세요.

**[신버전] 변경된 정책 (2025-12-01 적용)**:
- ✅ **email**: Soft Delete 고려 → 재사용 가능
- ✅ **userName**: Soft Delete 고려 → 재사용 가능 (유지)
- ✅ **phoneNumber**: Soft Delete 고려 → 재사용 가능

**변경된 Repository 메서드:**
```java
// 모든 개인정보 필드가 Soft Delete 고려
boolean existsByEmailAndIsDeletedFalse(String email);
boolean existsByUserNameAndIsDeletedFalse(String userName);
boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);
```

**변경 이유:**
1. 사용자 편의성 향상 (탈퇴 후 재가입 시 동일 정보 사용)
2. Soft Delete 정책 일관성 유지
3. GDPR 등 개인정보 보호 규정 준수

**상세 문서:**
- `코드 작업 지침/참고사항/soft_delete_username_strategy.md`
- 마이그레이션 절차, Partial Index 적용, Fallback 전략 포함

---

## 5. 향후 추가 예정 필드 (다른 도메인 작업 후)

### 5.1 Academy 연관관계 (Academy 도메인 담당자 작업 대기)

```java
// academy_id는 추후 Academy 엔티티 생성 시 추가 예정
// @ManyToOne(fetch = FetchType.LAZY)
// @JoinColumn(name = "academy_id")
// private Academy academy;
```

**추가 시점**: Academy 도메인 담당자가 Academy 엔티티를 완성한 후

---

## 6. 베이스 엔티티 (수정 금지)

### 6.1 BaseTimeEntity

```java
@MappedSuperclass
@Getter
public abstract class BaseTimeEntity {
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### 6.2 BaseSoftDeleteSupportEntity

```java
@Getter
@MappedSuperclass
public abstract class BaseSoftDeleteSupportEntity extends BaseTimeEntity {
    @Column(name="is_deleted", nullable = false)
    protected Boolean isDeleted = false;

    @Column(name="deleted_at")
    protected LocalDateTime deletedAt;

    public void markDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    public boolean isActive() {
        return !Boolean.TRUE.equals(this.isDeleted);
    }
}
```

---

## 7. 개발 가이드라인

### 7.1 엔티티 작성 규칙

1. **필드 변경 완료 사항**
   - ✅ **유지된 필드**: userName, password, email, phoneNumber (삭제/수정 금지)
   - 🔄 **변경된 필드**: 
     - `role` → `accountType` (Enum으로 변경 완료)
     - `company` → `affiliation` (명칭 변경 완료)
     - `department` → `position` (명칭 변경 완료)
   - ➕ **추가된 필드**: address, accountApproved (신규 추가 완료)

2. **새 필드 추가 시**
   - 팀원과 협의
   - 비즈니스 요구사항 확인
   - 기존 필드와 중복 여부 검토

3. **Lombok 어노테이션**
   - `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` 사용

4. **컬럼명 규칙**
   - Java: camelCase (userName)
   - DB: snake_case (user_name) - JPA가 자동 변환

5. **Enum 타입**
   - `@Enumerated(EnumType.STRING)` 사용 (ORDINAL 금지)

### 7.2 레포지토리 작성 규칙

1. **메소드 네이밍**
   - Spring Data JPA 규칙 준수
   - findBy-, existsBy-, countBy- 등

2. **복잡한 쿼리**
   - `@Query` 어노테이션 사용
   - JPQL 작성

3. **Soft Delete 고려**
   - isDeleted 필드 조건 추가
   - 예: `findByEmailAndIsDeleted(String email, Boolean isDeleted)`

---

## 8. 다음 단계

### 8.1 완료된 작업 ✅
- [x] Enum 클래스 생성 (AccountType, ApprovalStatus)
- [x] Account.java 엔티티 필드 수정
  - [x] role → accountType 변경 (Enum 타입)
  - [x] company → affiliation 변경
  - [x] department → position 변경
  - [x] address 필드 추가
  - [x] accountApproved 필드 추가 (Enum 타입)
- [x] AccountRepository.java 메소드 구현
  - [x] findByEmail(), existsByEmail() 등 7개 쿼리 메소드
  - [x] findByAccountApproved() 등 승인 관련 메소드

### 8.2 진행 예정 작업
- [ ] AccountService 인터페이스 작성
- [ ] AccountServiceImpl 구현
- [ ] DTO 클래스 작성 (SignupRequest, LoginRequest, LoginResponse 등)
- [ ] AuthController 작성 (회원가입/로그인 API)
- [ ] Spring Security + JWT 설정
- [ ] 단위 테스트 작성 (Repository, Service, Controller)

### 8.3 팀 협의 필요
- [ ] 프론트엔드 팀에 API 필드명 변경 공지
  - `role` → `accountType`
  - `company` → `affiliation`
  - `department` → `position`
  - 신규 필드: `address`, `accountApproved`
- [ ] 기존 DB 데이터가 있다면 마이그레이션 스크립트 작성
- [ ] Academy 도메인과 연관관계 설정 시기 논의

### 8.4 대기 중
- [ ] Academy 엔티티 생성 후 연관관계 매핑 (Academy 담당자 작업 대기)
- [ ] DDL 생성 및 DB 스키마 동기화
- [ ] 통합 테스트 작성

---

## 9. 참고 자료

- **프로젝트 가이드**: `.md/account/ACCOUNT_WORK_GUIDELINE.md`
- **JPA 가이드**: `.md/JPA_GUIDELINE.md`
- **API 가이드**: `.md/API_GUIDELINES.md`
- **SQL 참고**: `sql/softcampus.sql` (line 172-188)
- **📘 최신 Soft Delete 전략**: `코드 작업 지침/참고사항/soft_delete_username_strategy.md` (2025-12-01)

---

## 📝 문서 이력

### 버전 1.0 (2025-10-29)
- **상태**: 초기 작성
- **내용**: Account Entity 및 Repository 설계
- **Soft Delete 정책**: userName만 재사용 가능

### 버전 1.1 (2025-12-01) - 변경 요약 추가
- **상태**: 🗂️ SUPERSEDED (대체됨)
- **변경**: 2025-12-01 정책 변경 사항 요약 추가
- **주의**: 본 문서는 업데이트되지 않음, 요약만 추가
- **대체 문서**: `soft_delete_username_strategy.md`

### 관련 커밋
- `303fb42` (2025-12-01) - Allow email reuse after soft delete
- `e284efe` (2025-12-01) - Allow phone number reuse after soft delete
- `49d982e` (2025-12-01) - Correct soft-deleted account test mock
- `cb047ff` (2025-12-01) - Update documentation for Soft Delete policy

---

**최초 작성일**: 2025-10-29  
**마지막 수정일**: 2025-12-01 (변경 요약 추가, 본문 미변경)  
**작성 방식**: Entity-First (엔티티 코드 우선, SQL은 참고용)  
**문서 상태**: 🗂️ **SUPERSEDED** - 아카이브 (참고용)  
**현재 상태**: Domain Layer 완료 (필드 변환 완료: role→accountType, company→affiliation, department→position)  
**다음 단계**: ~~Service Layer 구현 예정~~ (완료됨, 최신 정보는 다른 문서 참조)
