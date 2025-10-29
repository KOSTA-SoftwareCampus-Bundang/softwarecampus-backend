# 코드 구현 가이드 및 실행 콘솔 로그

이 문서는 구현 과정의 각 단계와 실행 결과를 추적합니다.
실제 서비스용 로그가 아닌 개발할때 콘솔에서 확인할 콘솔 로그 전용

## 문서 작성 원칙

1. 로그 작성은 반드시 한글로 합니다
   - 실행 결과, 오류 메시지, 다음 단계 등 모든 설명은 한글로 작성
   - 예시: "회원가입 메서드 실행 성공" (O), "signup method success" (X)
   - 예시: "findByEmail() 메서드 실행 실패" (O), "findByEmail failed" (X)

2. 메소드 단위로 성공/실패를 명확히 표시
   - 성공 시: `✅ [메소드명] 성공`
     예시: "✅ AccountService.signup() 메서드 성공"
   - 실패 시: `❌ [메소드명] 실패`
     예시: "❌ AccountRepository.findByEmail() 메서드 실패"

3. 실행 결과는 재현 가능하도록 상세히
   - 실행한 메소드와 파라미터
   - 성공: 처리된 결과와 다음 단계
   - 실패: 구체적인 예외 타입과 메시지, 해결 방법
   - 모든 후속 단계는 구체적인 메소드명 포함

## 문서 사용법

각 구현 단계는 다음 형식을 따릅니다:

```
### 작업: [작업명]
📝 [작업 설명 및 목표]

#### ⌨️ 실행할 코드
```java
// 실행할 코드 블록
```

#### 📋 실행 로그
```log
실행 중 발생한 로그나 출력
```

#### ✅ 실행 결과
- 성공: [성공 시 결과 설명]
- 실패: [실패 시 원인과 다음 단계]
```

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