# 코드 구현 가이드 - Phase별 기록 방식

이 문서는 Account 도메인 개발 시 각 Phase별 작업 내용을 기록하는 방법을 설명합니다.

## 📁 파일 구조

```
.md/account/
  ├─ 코드 작업 지침/
  │   └─ implementation_guide.md (이 파일)
  └─ 작업 기록/
      ├─ phase2_global_exception_handler.md
      ├─ phase3_security_config.md
      ├─ phase4_dto_layer.md
      ├─ phase5_service_layer.md
      └─ phase6_service_test.md
      ... (Phase별로 계속)
```

## 📝 기록 원칙

### 1. Phase별 독립 파일
- 각 Phase마다 별도의 마크다운 파일 생성
- 파일명: `phase{번호}_{간단한_설명}.md`
- 예: `phase4_dto_layer.md`, `phase5_service_layer.md`

### 2. 성공은 간단히, 실패는 상세히
- ✅ **성공 시**: 체크 표시와 한 줄 요약
- ❌ **실패 시**: 상세 로그, 원인 분석, 해결 방법

### 3. 한글로 작성
- 모든 설명, 로그, 메시지는 한글로
- 예: "✅ signup() 메서드 정상 작동" (O)
- 예: "signup method works" (X)

### 4. 메서드 단위 추적
- 메서드 이름 명시
- 예: `AccountService.signup()`, `AccountRepository.save()`

## 📋 Phase 파일 템플릿

각 Phase 파일은 다음 구조를 따릅니다:

```markdown
# Phase X: [Phase 이름]

**작업 기간:** 2025-XX-XX ~ 2025-XX-XX  
**담당자:** 태윤  
**상태:** ✅ 완료 / 🚧 진행중 / ❌ 실패

---

## 📌 작업 목표
- 목표 1
- 목표 2

## 📂 생성/수정 파일
- ✅ `파일경로/FileName.java`
- ✅ `파일경로/AnotherFile.java`

---

## 🔨 작업 내용

### 1. [작업명]

**최종 상태:** ✅ 정상 작동

#### 작성한 코드
```java
// 핵심 코드 스니펫
```

#### 실행 및 테스트
```bash
./mvnw test -Dtest=TestClassName
```

**결과:** ✅ 테스트 통과

---

## ⚠️ 트러블슈팅

### Issue #1: [문제 제목]

**발생 시각:** 2025-XX-XX HH:MM

#### 📋 오류 로그
```log
상세한 오류 로그
스택 트레이스
```

#### 🔍 원인 분석
- 원인 1
- 원인 2

#### 🔧 해결 방법
```java
// 수정한 코드
```

**결과:** ✅ 해결 완료

---

## ✅ 최종 체크리스트
- [x] 모든 파일 생성 완료
- [x] 컴파일 성공
- [x] 테스트 통과
- [x] 다음 Phase 준비 완료

## 📝 주요 변경 이력
- **2025-XX-XX HH:MM**: 최초 작성
- **2025-XX-XX HH:MM**: 로직 수정 (이유)

## 🔜 다음 단계
- Phase X+1: [다음 Phase 이름]
```

---

## 💡 실전 작업 플로우

### Phase 시작 시
1. `작업 기록/` 폴더에 새 Phase 파일 생성
2. 템플릿 복사하여 기본 정보 입력
3. 상태: 🚧 진행중

### 개발 중
1. 코드 작성
2. 테스트 실행
3. **성공 시**: 체크 표시만 (✅)
4. **실패 시**: 트러블슈팅 섹션에 상세 기록

### Phase 완료 시
1. 최종 체크리스트 작성
2. 상태: ✅ 완료로 변경
3. 다음 Phase 파일 생성 준비

---

## 📊 기록 레벨 가이드

### Level 1: 간단 (성공 케이스)
```markdown
✅ AccountService.signup() 구현 완료
✅ 테스트 통과
```

### Level 2: 보통 (약간의 이슈)
```markdown
✅ AccountService.signup() 구현 완료

**참고:**
- 최초 테스트 실패했으나 @Transactional 추가로 해결
```

### Level 3: 상세 (복잡한 문제)
```markdown
### ⚠️ 트러블슈팅: PasswordEncoder Null 오류

#### � 오류 로그
```
NullPointerException at line 23
...
```

#### 🔍 원인
- Mock 객체 설정 누락

#### 🔧 해결
```java
@Mock
private PasswordEncoder passwordEncoder;
```

✅ 해결 완료
```

---

## ⚡ 빠른 참조

### 자주 사용하는 명령어
```bash
# 컴파일
mvn clean compile

# 특정 테스트 실행
./mvnw test -Dtest=ClassName#methodName

# 전체 테스트
./mvnw test

# 빌드 + 검증
./mvnw clean verify
```

### 자주 사용하는 표시
- ✅ 성공/완료
- ❌ 실패
- 🚧 진행중
- ⚠️ 주의/경고
- 🔍 분석
- 🔧 수정/해결
- 📋 로그
- 💡 팁
- 🔜 다음 단계

---

**작성일:** 2025-10-29  
**최종 수정:** 2025-10-29

## 예시: Account 엔티티 생성 및 테스트

### 작업: Account 엔티티 정의
📝 사용자 계정을 표현하는 JPA 엔티티를 생성합니다.

#### ⌨️ 실행할 코드
```java
@Entity
@Table(name = "account")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;
    
    // ... 나머지 필드와 메서드
}
```

#### 📋 실행 로그
```log
Hibernate: create table account (
    id bigint not null auto_increment,
    email varchar(255) not null,
    ...
    primary key (id)
)
```

#### ✅ Account 엔티티 생성 성공
- `Account.java` 생성 완료
- `@Table(name = "account")` 매핑으로 테이블 자동 생성됨
- `email` 필드에 유니크 제약조건 정상 추가
- 다음: `AccountRepository` 인터페이스에 `findByEmail()` 메서드 구현 예정

---

### 작업: AccountRepository 생성
📝 Account 엔티티에 대한 Spring Data JPA 리포지토리를 정의합니다.

#### ⌨️ 실행할 코드
```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);
}
```

#### 📋 실행 로그
```log
// 스프링 부트 시작 시
Repository interface AccountRepository - Spring Data JPA will create implementation
```

#### ✅ findByEmail() 메서드 구현 성공
- `AccountRepository.findByEmail()` 메서드 자동 구현됨
- 스프링 데이터 JPA가 적절한 쿼리 생성 확인
- 다음: `findByEmail()` 메서드에 대한 단위 테스트 작성 예정

---

## 작업 실패 시 트러블슈팅 예시

### 작업: 이메일 중복 체크 테스트
📝 이메일 중복 확인 기능을 테스트합니다.

#### ⌨️ 실행할 코드
```java
@Test
void duplicateEmailTest() {
    String email = "test@example.com";
    accountRepository.save(new Account(email));
    
    assertThrows(DataIntegrityViolationException.class, () -> {
        accountRepository.save(new Account(email));
    });
}
```

#### 📋 실행 로그
```log
org.springframework.dao.DataIntegrityViolationException: could not execute statement; SQL [n/a]; constraint [UK_q0uja26qgu1atulenwup9rxyr]
```

#### ❌ AccountRepository.save() 메서드 실행 실패
- 실패한 메서드: `accountRepository.save(new Account(email))`
- 원인: 동일 이메일로 `save()` 호출 시 유니크 제약조건 위반
- 오류 내용: DataIntegrityViolationException (UK_q0uja26qgu1atulenwup9rxyr)
- 해결 방법: 
  1. 테스트 클래스에 `@Transactional` 추가
  2. 또는 `@BeforeEach`에서 `accountRepository.deleteAll()` 실행
- 다음 단계: 데이터 정리 후 `save()` 메서드 재테스트 예정

---

## 참고: 자주 쓰는 확인 포인트

### 빌드/테스트
```bash
# 전체 빌드 (테스트 포함)
./mvnw clean verify

# 테스트만 실행
./mvnw test

# 특정 테스트만 실행
./mvnw test -Dtest=AccountRepositoryTest
```

### 로그 레벨 조정
```properties
# application.properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

---

🔍 **팁:**
- 실행 로그는 가능한 한 전체를 포함하되, 민감정보는 마스킹 처리
- 실패한 경우 원인과 해결 방법을 명확히 기록
- 다음 단계로 넘어가기 전에 현재 상태가 정상인지 확인