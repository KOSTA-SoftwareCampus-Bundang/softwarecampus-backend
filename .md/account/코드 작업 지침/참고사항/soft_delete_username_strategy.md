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

### 3. GDPR '잊혀질 권리' 준수 (부분 준수)

> ⚠️ **현재 상태**: Soft Delete 방식으로 **부분 준수**  
> 완전한 GDPR 준수를 위해서는 추가 정책 및 구현이 필요합니다.

#### 현재 구현 (Soft Delete)
- ✅ 사용자 요청 시 즉시 논리적 삭제 (`isDeleted = true`)
- ✅ 삭제된 데이터는 일반 조회/검색에서 완전히 배제
- ✅ 개인정보 재사용 가능 (탈퇴 후 재가입)

#### GDPR 완전 준수를 위한 추가 요구사항

**1. 물리적 삭제 정책 (현재 미구현)**
- 🔴 **요구사항**: 사용자 요청 후 30일 이내 완전 삭제 보장
- 🔴 **구현 필요**: 스케줄러를 통한 자동 물리적 삭제
  ```java
  @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
  public void permanentlyDeleteExpiredAccounts() {
      LocalDateTime threshold = LocalDateTime.now().minusDays(30);
      List<Account> expiredAccounts = accountRepository
          .findByIsDeletedTrueAndDeletedAtBefore(threshold);
      accountRepository.deleteAll(expiredAccounts); // 물리적 삭제
  }
  ```

**2. 제3자 데이터 처리자 삭제 전파 (현재 미구현)**
- 🔴 **요구사항**: 이메일 서비스, 클라우드 스토리지 등 제3자에게 삭제 요청
- 🔴 **구현 필요**: 외부 서비스 API 호출 및 삭제 확인 로깅
  ```java
  emailService.deleteUserData(account.getEmail());
  cloudStorageService.deleteUserFiles(account.getId());
  auditLog.recordDeletionPropagation(account.getId(), "EMAIL_SERVICE", "SUCCESS");
  ```

**3. 감사 로그 및 백업 처리 (현재 미구현)**
- 🔴 **요구사항**: 감사 로그에서 개인정보 익명화 또는 삭제
- 🔴 **요구사항**: 백업 데이터에서도 삭제 또는 익명화
- 🔴 **구현 필요**: 
  ```java
  // 감사 로그 익명화
  auditLogRepository.anonymizeByAccountId(account.getId());
  
  // 백업 정책: 30일 이후 백업에서도 제외
  backupService.markForExclusion(account.getId());
  ```

**4. 법적 보존 의무 예외 처리 (현재 미구현)**
- 🟡 **요구사항**: 법적 분쟁, 회계 감사 등 보존 의무 기간 준수
- 🔴 **구현 필요**: 
  ```java
  if (account.hasLegalHold()) {
      throw new LegalHoldException("법적 보존 의무로 삭제 불가");
  }
  ```

**5. 삭제 확인 및 증명 (현재 미구현)**
- 🔴 **요구사항**: 사용자에게 삭제 완료 통지
- 🔴 **요구사항**: 삭제 증명서 발급 가능
- 🔴 **구현 필요**: 
  ```java
  emailService.sendDeletionConfirmation(account.getEmail());
  deletionCertificateService.generate(account.getId(), LocalDateTime.now());
  ```

#### 향후 작업 계획
- [ ] 물리적 삭제 스케줄러 구현 (30일 후 자동 삭제)
- [ ] 제3자 삭제 전파 API 연동
- [ ] 감사 로그 익명화 정책 수립
- [ ] 백업 데이터 삭제/익명화 절차 문서화
- [ ] 법적 보존 의무 예외 처리 로직
- [ ] 삭제 확인 통지 및 증명서 발급

#### 참고 문서
- **GDPR 전체 준수 가이드**: (작성 예정) `.md/account/정책/GDPR_compliance.md`
- **물리적 삭제 정책**: (작성 예정) `.md/account/정책/physical_deletion_policy.md`
- **데이터 보존 정책**: (작성 예정) `.md/account/정책/data_retention_policy.md`

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
/**
 * 활성 계정 검색 (키워드 기반)
 * 
 * 대소문자 구분:
 * - userName, email: LOWER() 적용 (대소문자 무시)
 * - phoneNumber: LOWER() 미적용 (숫자 형식으로 대소문자 개념 없음)
 */
@Query("SELECT a FROM Account a " +
       "WHERE a.isDeleted = false AND " +
       "(:keyword IS NULL OR " +
       "LOWER(a.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
       "LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
       "a.phoneNumber LIKE CONCAT('%', :keyword, '%'))")
Page<Account> searchActiveAccounts(@Param("keyword") String keyword, Pageable pageable);
```

**설계 근거:**
- `userName`, `email`: 대소문자 혼용 가능 → `LOWER()` 적용
- `phoneNumber`: 숫자 형식 (010-1234-5678) → 대소문자 개념 없음, `LOWER()` 불필요

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

## 📝 데이터베이스 제약 및 실행 방침

### ⚠️ 현재 상태 분석

**문제점:**
- JPA Entity에 `unique=true` 제약이 남아있음
- 애플리케이션 레벨에서는 `isDeleted=false`만 체크
- **DB와 애플리케이션 레벨 정책 불일치** → 데이터 무결성 위험

**상충 시나리오:**
```sql
-- 1. 사용자 A가 test@example.com으로 가입
INSERT INTO account (email, is_deleted) VALUES ('test@example.com', false);

-- 2. 사용자 A 탈퇴 (Soft Delete)
UPDATE account SET is_deleted = true WHERE email = 'test@example.com';

-- 3. 사용자 B가 동일 이메일로 재가입 시도
INSERT INTO account (email, is_deleted) VALUES ('test@example.com', false);
-- ❌ ERROR: duplicate key value violates unique constraint "uk_account_email"
```

### 🎯 **필수 실행 방침: Partial Index 적용**

> ✅ **팀 결정**: Partial Index를 **필수**로 적용하여 DB와 애플리케이션 정책 일치  
> 이는 데이터 무결성을 DB 레벨에서 보장하고 Race Condition을 완전히 제거합니다.

#### 지원 DBMS
- ✅ **PostgreSQL** (모든 버전) - **권장**
- ✅ MySQL 8.0.13+ (Partial Index 지원)
- ❌ MySQL 5.7 이하 - 대안 방식 적용 필요 (아래 참조)

#### 적용 이유
1. **DB-App 정책 일치**: DB 제약과 애플리케이션 로직 동기화
2. **Race Condition 제거**: 동시 가입 시도 시 DB 레벨 보호
3. **데이터 무결성 보장**: unique 제약 위반 원천 차단
4. **성능 최적화**: 활성 계정만 인덱싱하여 조회 성능 향상

---

## 🔧 Partial Index 마이그레이션 절차 (필수)

### 사전 준비

#### 1. 환경 확인
```bash
# PostgreSQL 버전 확인
psql --version

# MySQL 버전 확인
mysql --version

# 현재 제약 조건 확인
\d account  # PostgreSQL
SHOW CREATE TABLE account;  # MySQL
```

#### 2. 데이터 정합성 검증
```sql
-- 활성 계정 중 중복 이메일 확인
SELECT email, COUNT(*) as cnt
FROM account
WHERE is_deleted = false
GROUP BY email
HAVING COUNT(*) > 1;

-- 활성 계정 중 중복 전화번호 확인
SELECT phone_number, COUNT(*) as cnt
FROM account
WHERE is_deleted = false
GROUP BY phone_number
HAVING COUNT(*) > 1;

-- 중복 발견 시 수동 정리 필요
```

#### 3. 백업
```bash
# PostgreSQL 백업
pg_dump -U postgres -d softwarecampus -F c -b -v -f "backup_$(date +%Y%m%d_%H%M%S).dump"

# MySQL 백업
mysqldump -u root -p softwarecampus > "backup_$(date +%Y%m%d_%H%M%S).sql"
```

---

### 마이그레이션 실행

#### Step 1: Partial Index 생성 (무중단)

**PostgreSQL:**
```sql
-- CONCURRENTLY 옵션으로 서비스 중단 없이 생성
CREATE UNIQUE INDEX CONCURRENTLY uk_account_email_active 
ON account(email) 
WHERE is_deleted = false;

CREATE UNIQUE INDEX CONCURRENTLY uk_account_phone_active 
ON account(phone_number) 
WHERE is_deleted = false;

CREATE UNIQUE INDEX CONCURRENTLY uk_account_username_active 
ON account(user_name) 
WHERE is_deleted = false;
```

**MySQL 8.0+:**
```sql
-- MySQL은 CONCURRENTLY 미지원 (짧은 락 발생 주의)
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

#### Step 2: Index 생성 검증
```sql
-- PostgreSQL
SELECT 
    indexname, 
    indexdef 
FROM pg_indexes 
WHERE tablename = 'account' 
  AND indexname LIKE '%_active';

-- MySQL
SHOW INDEX FROM account WHERE Key_name LIKE '%_active';
```

#### Step 3: 기능 테스트 (중복 방지 확인)
```sql
-- 테스트 1: 활성 계정 중복 방지 (실패해야 정상)
INSERT INTO account (email, is_deleted, user_name, password, phone_number, account_type) 
VALUES ('test@example.com', false, 'testuser', 'hashed_pw', '010-1234-5678', 'USER');

INSERT INTO account (email, is_deleted, user_name, password, phone_number, account_type) 
VALUES ('test@example.com', false, 'testuser2', 'hashed_pw2', '010-9999-9999', 'USER');
-- ❌ ERROR: duplicate key value violates unique constraint "uk_account_email_active"

-- 테스트 2: 삭제된 계정 + 활성 계정 중복 허용 (성공해야 정상)
UPDATE account SET is_deleted = true WHERE email = 'test@example.com';

INSERT INTO account (email, is_deleted, user_name, password, phone_number, account_type) 
VALUES ('test@example.com', false, 'testuser3', 'hashed_pw3', '010-8888-8888', 'USER');
-- ✅ SUCCESS

-- 테스트 데이터 정리
DELETE FROM account WHERE email = 'test@example.com';
```

#### Step 4: JPA Entity 수정
```java
@Entity
@Table(
    name = "account",
    indexes = {
        // ❌ 기존 unique index 제거 (Partial Index로 대체)
        // @Index(name = "uk_account_email", columnList = "email", unique = true),
        // @Index(name = "uk_account_phone", columnList = "phone_number", unique = true),
        
        // ✅ 일반 index로 변경 (조회 성능용)
        @Index(name = "idx_account_email", columnList = "email"),
        @Index(name = "idx_account_phone", columnList = "phone_number"),
        @Index(name = "idx_account_username", columnList = "user_name"),
        @Index(name = "idx_account_deleted", columnList = "is_deleted"),
        
        // Partial Index는 Flyway/Liquibase로 관리
        // (JPA @Index로는 WHERE 조건 표현 불가)
    }
)
public class Account extends BaseSoftDeleteSupportEntity {
    // ...
}
```

#### Step 5: 기존 Unique Index 제거
```sql
-- Partial Index가 정상 작동 확인 후 제거
DROP INDEX uk_account_email;
DROP INDEX uk_account_phone;

-- 참고: userName은 원래 unique index 없었음
```

#### Step 6: 애플리케이션 재배포 및 검증
```bash
# 1. 애플리케이션 빌드
mvn clean package -DskipTests

# 2. 통합 테스트 실행
mvn test -Dtest=SignupIntegrationTest

# 3. 스테이징 환경 배포
# 4. 회원가입/탈퇴/재가입 시나리오 테스트
# 5. 프로덕션 배포
```

---

### 마이그레이션 체크리스트

#### 사전 준비
- [ ] DB 버전 확인 (PostgreSQL 또는 MySQL 8.0+)
- [ ] 중복 데이터 검증 및 정리
- [ ] 전체 DB 백업 완료
- [ ] 롤백 절차 문서화

#### 마이그레이션 실행
- [ ] Partial Index 생성 (CONCURRENTLY)
- [ ] Index 생성 확인 (pg_indexes 또는 SHOW INDEX)
- [ ] 기능 테스트 (중복 방지 확인)
- [ ] JPA Entity 수정 (unique=true 제거)
- [ ] 애플리케이션 빌드 성공
- [ ] 통합 테스트 통과

#### 배포 및 검증
- [ ] 스테이징 환경 배포
- [ ] 회원가입 시나리오 테스트
- [ ] 탈퇴 후 재가입 테스트
- [ ] 동시 가입 테스트 (Race Condition)
- [ ] 프로덕션 배포
- [ ] 모니터링 (오류율, 응답 시간)

#### 사후 정리
- [ ] 기존 Unique Index 제거
- [ ] 마이그레이션 완료 문서화
- [ ] 백업 파일 보관 (30일)

---

### 롤백 절차 (문제 발생 시)

#### 1. 즉시 롤백 (Index 생성 실패 시)
```sql
-- Partial Index 제거
DROP INDEX CONCURRENTLY uk_account_email_active;
DROP INDEX CONCURRENTLY uk_account_phone_active;
DROP INDEX CONCURRENTLY uk_account_username_active;
```

#### 2. 애플리케이션 롤백 (배포 후 문제 발생 시)
```bash
# 1. 이전 버전 재배포
git checkout <이전_커밋_해시>
mvn clean package -DskipTests

# 2. JPA Entity 원상복구 (unique=true 복원)

# 3. 재배포
```

#### 3. 데이터 복구 (데이터 손상 시)
```bash
# PostgreSQL
pg_restore -U postgres -d softwarecampus backup_YYYYMMDD_HHMMSS.dump

# MySQL
mysql -u root -p softwarecampus < backup_YYYYMMDD_HHMMSS.sql
```

---

## 🔄 대안 방식: Unique 제약 완전 제거 (권장하지 않음)

> ⚠️ **비권장**: DB 레벨 보호 없이 애플리케이션 로직에만 의존  
> Race Condition 위험이 있으므로 Partial Index 적용을 강력히 권장합니다.

### 적용 시나리오
- MySQL 5.7 이하 사용 (Partial Index 미지원)
- 단일 서버 환경 (동시성 낮음)
- 빠른 프로토타이핑 필요

### 구현 방법

#### 1. JPA Entity에서 Unique 제거
```java
@Table(
    name = "account",
    indexes = {
        // unique=true 제거
        @Index(name = "idx_account_email", columnList = "email"),
        @Index(name = "idx_account_phone", columnList = "phone_number"),
        @Index(name = "idx_account_username", columnList = "user_name"),
        @Index(name = "idx_account_deleted", columnList = "is_deleted")
    }
)
```

#### 2. 기존 Unique Index 제거
```sql
DROP INDEX uk_account_email;
DROP INDEX uk_account_phone;
```

#### 3. 비관적 락 적용 (Race Condition 방지)
```java
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.email = :email FOR UPDATE")
    Optional<Account> findByEmailForUpdate(@Param("email") String email);
}

@Service
@Transactional
public class SignupServiceImpl implements SignupService {
    
    @Override
    public void signup(SignupRequest request) {
        // 1. 행 잠금 (다른 트랜잭션 대기)
        accountRepository.findByEmailForUpdate(request.getEmail());
        
        // 2. 중복 체크
        if (accountRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다");
        }
        
        // 3. 저장
        accountRepository.save(Account.builder()
            .email(request.getEmail())
            .build());
    }
}
```

### 위험 요소 및 대응

| 위험 | 영향 | 대응 방안 |
|------|------|-----------|
| Race Condition | 동시 가입 시 중복 데이터 생성 | 비관적 락 적용 (필수) |
| 성능 저하 | 동시 요청 시 대기 시간 증가 | 트랜잭션 범위 최소화 |
| 데이터 무결성 | DB 레벨 보호 없음 | 철저한 테스트 및 모니터링 |

### 테스트 케이스 (필수)

```java
@Test
@DisplayName("동시 가입 시도 - Race Condition 방지 검증")
void 동시가입_RaceCondition방지() throws Exception {
    String email = "concurrent@test.com";
    
    // 100개 스레드로 동시 가입 시도
    ExecutorService executor = Executors.newFixedThreadPool(100);
    CountDownLatch latch = new CountDownLatch(100);
    
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);
    
    for (int i = 0; i < 100; i++) {
        executor.submit(() -> {
            try {
                signupService.signup(new SignupRequest(email, "pw", ...));
                successCount.incrementAndGet();
            } catch (DuplicateEmailException e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await();
    executor.shutdown();
    
    // 검증: 정확히 1개만 성공, 99개는 중복 오류
    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failCount.get()).isEqualTo(99);
    
    // DB 확인: 1개만 존재
    long count = accountRepository.countByEmailAndIsDeletedFalse(email);
    assertThat(count).isEqualTo(1);
}
```

---

## 🎯 최종 권장 사항

### 환경별 적용 방침

| 환경 | 권장 방식 | 이유 |
|------|-----------|------|
| **PostgreSQL** | ✅ **Partial Index (필수)** | 완전한 지원, 최고 성능 |
| **MySQL 8.0+** | ✅ **Partial Index (필수)** | 지원 가능, DB 레벨 보호 |
| **MySQL 5.7** | ⚠️ **비관적 락 + App 검증** | Partial Index 미지원 |
| **개발 환경** | ✅ **Partial Index (권장)** | 프로덕션과 동일 환경 |

### 구현 우선순위
1. ✅ **1순위**: Partial Index 적용 (PostgreSQL/MySQL 8.0+)
2. ⚠️ **2순위**: 비관적 락 + 애플리케이션 검증 (MySQL 5.7 이하)
3. ❌ **비권장**: Unique 제약 없이 애플리케이션만 의존

---

## 📋 운영 배포 체크리스트

### Phase 1: 계획 및 준비 (D-7)
- [ ] DB 버전 및 Partial Index 지원 확인
- [ ] 마이그레이션 계획 수립 (배포 시간, 롤백 계획)
- [ ] 스테이징 환경 마이그레이션 테스트
- [ ] 성능 테스트 (인덱스 생성 시간, 조회 성능)
- [ ] 모니터링 대시보드 준비 (Grafana, CloudWatch 등)

### Phase 2: 사전 검증 (D-3)
- [ ] 프로덕션 데이터 중복 확인
- [ ] 중복 데이터 정리 계획 (있을 경우)
- [ ] 전체 DB 백업 (최소 2개 백업 보관)
- [ ] 롤백 절차 시뮬레이션
- [ ] 팀 공지 및 긴급 연락망 확인

### Phase 3: 마이그레이션 실행 (D-Day)
- [ ] 서비스 트래픽 모니터링 시작
- [ ] Partial Index 생성 (CONCURRENTLY)
- [ ] Index 생성 완료 확인 (pg_stat_progress_create_index)
- [ ] 기능 테스트 (중복 방지 확인)
- [ ] 애플리케이션 재배포
- [ ] 헬스체크 통과 확인

### Phase 4: 검증 및 모니터링 (D+1 ~ D+7)
- [ ] 회원가입/탈퇴/재가입 시나리오 테스트
- [ ] 오류율 모니터링 (목표: <0.01%)
- [ ] 응답 시간 모니터링 (목표: p95 < 200ms)
- [ ] 중복 데이터 발생 여부 확인
- [ ] 사용자 피드백 수집

### Phase 5: 정리 (D+7)
- [ ] 기존 Unique Index 제거
- [ ] 마이그레이션 완료 보고서 작성
- [ ] 백업 파일 장기 보관 (30일)
- [ ] 문서 업데이트 (운영 가이드, 장애 대응 매뉴얼)

---

## 🔍 모니터링 및 알림

### 핵심 지표

| 지표 | 목표 | 알림 조건 |
|------|------|-----------|
| 회원가입 성공률 | >99% | <95% 시 Critical |
| 회원가입 응답 시간 (p95) | <200ms | >500ms 시 Warning |
| 중복 오류 발생률 | 0% | >0% 시 Critical |
| DB 연결 풀 사용률 | <80% | >90% 시 Warning |
| Index 스캔 비율 | >95% | <80% 시 Warning (Seq Scan 증가) |

### 모니터링 쿼리

```sql
-- Partial Index 사용 확인 (PostgreSQL)
EXPLAIN ANALYZE
SELECT * FROM account 
WHERE email = 'test@example.com' 
  AND is_deleted = false;
-- "Index Scan using uk_account_email_active" 확인

-- Index 크기 및 통계 (PostgreSQL)
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'account'
  AND indexname LIKE '%_active';

-- 중복 데이터 모니터링 (매일 실행)
SELECT email, COUNT(*) as cnt
FROM account
WHERE is_deleted = false
GROUP BY email
HAVING COUNT(*) > 1;
```

---

## 📚 관련 문서

- **마이그레이션 상세 가이드**: (위 "🔧 Partial Index 마이그레이션 절차" 섹션 참조)
- **Fallback 전략**: (위 "🔄 대안 방식" 섹션 참조)
- **GDPR 준수**: (위 "GDPR '잊혀질 권리' 준수" 섹션 참조)
- **Race Condition 테스트**: (위 "테스트 케이스 (필수)" 섹션 참조)

---

## ❓ FAQ

### Q1. Partial Index 생성 시 서비스 중단이 발생하나요?
A: PostgreSQL의 경우 `CONCURRENTLY` 옵션으로 무중단 생성 가능. MySQL은 짧은 락 발생 (일반적으로 < 1초).

### Q2. 마이그레이션 실패 시 롤백은 어떻게 하나요?
A: 위 "롤백 절차" 섹션 참조. Index 제거 → 애플리케이션 롤백 → 필요시 백업 복구 순서.

### Q3. MySQL 5.7에서는 어떻게 대응하나요?
A: 비관적 락(`@Lock`) + 애플리케이션 검증 방식 적용. 위 "대안 방식" 섹션 참조.

### Q4. Partial Index vs 비관적 락 성능 차이는?
A: Partial Index가 약 10-20% 더 빠름 (락 대기 없음). 동시성이 높을수록 차이 증가.

### Q5. 기존 unique index를 언제 제거해야 하나요?
A: Partial Index 생성 후 최소 7일 모니터링 → 문제 없으면 제거. 급하지 않음 (양쪽 공존 가능).

---

## 🔒 보안 고려사항
- Race Condition 완전 제거
- 애플리케이션 로직 단순화

#### SQL 스크립트
```sql
-- 활성 계정만 unique 보장 (PostgreSQL / MySQL 8.0+)
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

## 🔧 마이그레이션 절차 (Partial Index 적용 시)

### 1단계: 백업 및 사전 검증
```sql
-- 현재 중복 데이터 확인
SELECT email, COUNT(*) 
FROM account 
WHERE is_deleted = false 
GROUP BY email 
HAVING COUNT(*) > 1;

-- 백업 (권장)
pg_dump -U postgres -d softwarecampus > backup_before_migration.sql
```

### 2단계: 새 Partial Index 생성
```sql
-- 기존 unique index는 유지한 채 새 index 생성
CREATE UNIQUE INDEX CONCURRENTLY uk_account_email_active 
ON account(email) 
WHERE is_deleted = false;

CREATE UNIQUE INDEX CONCURRENTLY uk_account_phone_active 
ON account(phone_number) 
WHERE is_deleted = false;

CREATE UNIQUE INDEX CONCURRENTLY uk_account_username_active 
ON account(user_name) 
WHERE is_deleted = false;
```

### 3단계: 검증
```sql
-- Index 생성 확인
\d account  -- PostgreSQL
SHOW INDEX FROM account;  -- MySQL

-- 중복 테스트 (실패해야 정상)
INSERT INTO account (email, is_deleted) VALUES ('test@example.com', false);
INSERT INTO account (email, is_deleted) VALUES ('test@example.com', false);
-- ERROR: duplicate key value violates unique constraint
```

### 4단계: JPA Entity 수정
```java
@Table(
    name = "account",
    indexes = {
        // ❌ 기존 unique index 제거
        // @Index(name = "uk_account_email", columnList = "email", unique = true),
        // @Index(name = "uk_account_phone", columnList = "phone_number", unique = true),
        
        // ✅ 일반 index로 변경 (Partial Index는 직접 SQL로 관리)
        @Index(name = "idx_account_email", columnList = "email"),
        @Index(name = "idx_account_phone", columnList = "phone_number"),
        @Index(name = "idx_account_username", columnList = "user_name"),
        @Index(name = "idx_account_deleted", columnList = "is_deleted")
    }
)
```

### 5단계: 기존 UNIQUE Index 제거 (선택사항)
```sql
-- 새 Partial Index가 정상 작동 확인 후 제거
DROP INDEX uk_account_email;
DROP INDEX uk_account_phone;

-- 참고: userName은 원래 unique index가 없었음
```

---

## 🛡️ Fallback 전략 (Partial Index 미지원 환경)

### MySQL 5.7 이하 또는 기타 DBMS

#### 방법 1: 애플리케이션 레벨 검증 (현재 적용 중)
```java
// 현재 구현 - 트랜잭션 + 애플리케이션 레벨 중복 체크
@Transactional
public void signup(SignupRequest request) {
    if (accountRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
        throw new DuplicateEmailException("이미 사용 중인 이메일입니다");
    }
    
    Account account = Account.builder()
        .email(request.getEmail())
        .build();
    
    accountRepository.save(account);
}
```

**장점**: 모든 DBMS에서 동작  
**단점**: Race Condition 가능성 (동시 요청 시)

#### 방법 2: 비관적 락 (Pessimistic Lock)
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.email = :email")
Optional<Account> findByEmailForUpdate(@Param("email") String email);

@Transactional
public void signup(SignupRequest request) {
    // 테이블 행 잠금
    accountRepository.findByEmailForUpdate(request.getEmail());
    
    if (accountRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
        throw new DuplicateEmailException("이미 사용 중인 이메일입니다");
    }
    
    accountRepository.save(Account.builder().email(request.getEmail()).build());
}
```

**장점**: Race Condition 완전 제거  
**단점**: 성능 저하 (동시성 감소)

#### 방법 3: Unique Index + 예외 처리
```java
// unique index 유지하고 예외 처리
@Transactional
public void signup(SignupRequest request) {
    try {
        accountRepository.save(Account.builder()
            .email(request.getEmail())
            .build());
    } catch (DataIntegrityViolationException e) {
        if (e.getMessage().contains("uk_account_email")) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다");
        }
        throw e;
    }
}
```

**장점**: DB 레벨 보장, Race Condition 없음  
**단점**: 탈퇴 후 재가입 불가능 (정책 위배)

#### 권장 전략
- **Partial Index 지원**: 방법 1 (애플리케이션 레벨) + Partial Index (DB 레벨 이중 보호)
- **Partial Index 미지원**: 방법 1 (애플리케이션 레벨) 단독 사용
- **높은 동시성 환경**: 방법 2 (비관적 락) 고려

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
