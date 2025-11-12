# 4. Mockito 패턴 및 테스트 실행

**테스트 프레임워크 종합 가이드**

---

## 📦 의존성 설정

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Test (Mockito, AssertJ, JUnit 5 포함) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**포함된 라이브러리:**
- JUnit 5 (Jupiter)
- Mockito
- AssertJ
- Spring Test

---

## 🎯 Mockito 핵심 패턴

### 1. Mock 객체 생성

```java
@ExtendWith(MockitoExtension.class)  // JUnit 5
class ServiceTest {
    
    @Mock
    private AccountRepository accountRepository;  // Mock 객체
    
    @InjectMocks
    private SignupServiceImpl signupService;  // Mock 주입 대상
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
    }
}
```

**어노테이션:**
- `@Mock`: Mock 객체 생성
- `@InjectMocks`: Mock을 주입받는 실제 테스트 대상
- `@ExtendWith(MockitoExtension.class)`: Mockito 활성화 (JUnit 5)

---

### 2. Stubbing (행위 정의)

```java
// When-ThenReturn: 메서드 호출 시 반환값 정의
when(accountRepository.findById(1L))
    .thenReturn(Optional.of(account));

// When-ThenThrow: 예외 발생
when(accountRepository.save(any(Account.class)))
    .thenThrow(new DataIntegrityViolationException("중복"));

// ArgumentMatchers: 인자 매칭
when(passwordEncoder.encode(anyString())).thenReturn("encoded");
when(accountRepository.save(any(Account.class))).thenReturn(account);
```

**주요 ArgumentMatchers:**
- `any(Class.class)`: 해당 타입의 모든 객체
- `anyString()`: 모든 문자열
- `anyLong()`, `anyInt()`: 모든 숫자
- `eq(value)`: 특정 값과 일치

---

### 3. Verification (행위 검증)

```java
// 메서드 호출 확인
verify(accountRepository).save(any(Account.class));
verify(passwordEncoder).encode("password123");

// 호출 횟수 검증
verify(accountRepository, times(1)).findById(1L);
verify(accountRepository, never()).delete(any());

// 인자 검증 (ArgumentCaptor)
verify(accountRepository).save(argThat(account ->
    account.getEmail().equals("user@example.com") &&
    account.getAccountType() == AccountType.USER
));
```

**검증 메서드:**
- `verify(mock).method()`: 1번 호출 확인
- `verify(mock, times(n))`: n번 호출 확인
- `verify(mock, never())`: 호출되지 않음 확인
- `argThat(Predicate)`: 커스텀 조건 검증

---

### 4. ArgumentCaptor (인자 캡처)

```java
@Test
void testArgumentCapture() {
    // Given
    ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
    
    // When
    signupService.signup(request);
    
    // Then
    verify(accountRepository).save(accountCaptor.capture());
    Account capturedAccount = accountCaptor.getValue();
    
    assertThat(capturedAccount.getEmail()).isEqualTo("user@example.com");
    assertThat(capturedAccount.getPassword()).startsWith("encoded");
}
```

---

## ✅ AssertJ 검증 패턴

### 1. 기본 검증

```java
// 객체 검증
assertThat(response).isNotNull();
assertThat(response.id()).isEqualTo(1L);
assertThat(response.email()).isEqualTo("user@example.com");

// Boolean 검증
assertThat(EmailUtils.isValidEmail(email)).isTrue();
assertThat(EmailUtils.isValidEmail(invalid)).isFalse();

// 문자열 검증
assertThat(maskedEmail).startsWith("u***");
assertThat(maskedEmail).contains("@");
assertThat(maskedEmail).endsWith("example.com");
```

---

### 2. 예외 검증

```java
// 예외 타입 + 메시지 검증
assertThatThrownBy(() -> service.doSomething())
    .isInstanceOf(InvalidInputException.class)
    .hasMessage("올바른 이메일 형식이 아닙니다.");

// 예외만 검증
assertThatThrownBy(() -> service.doSomething())
    .isInstanceOf(AccountNotFoundException.class);
```

---

### 3. 컬렉션 검증

```java
// 리스트 크기
assertThat(list).hasSize(3);

// 포함 여부
assertThat(list).contains(item1, item2);
assertThat(list).containsExactly(item1, item2, item3);

// 조건 검증
assertThat(list).allMatch(item -> item.getId() > 0);
```

---

## 🧪 테스트 실행 명령어

### Maven

```powershell
# 모든 테스트 실행
mvn test

# 특정 클래스 테스트
mvn test -Dtest=SignupServiceImplTest

# 특정 메서드 테스트
mvn test -Dtest=SignupServiceImplTest#signup_성공_USER

# 테스트 건너뛰기
mvn clean install -DskipTests

# 병렬 실행 (성능 향상)
mvn test -T 4

# 상세 로그 출력
mvn test -X
```

---

### 테스트 커버리지 (JaCoCo)

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.10</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**커버리지 확인:**
```powershell
mvn clean test

# 리포트 생성: target/site/jacoco/index.html
```

---

## 📝 Given-When-Then 패턴

```java
@Test
@DisplayName("회원가입 성공 - USER")
void signup_성공() {
    // Given: 테스트 준비
    SignupRequest request = new SignupRequest(...);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
    
    // When: 실제 실행
    AccountResponse response = signupService.signup(request);
    
    // Then: 검증
    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(1L);
    verify(accountRepository).save(any(Account.class));
}
```

---

## 🔍 테스트 격리 원칙

### 1. 각 테스트는 독립적
```java
@BeforeEach
void setUp() {
    // 매 테스트마다 초기화
    testAccount = new Account(...);
}
```

### 2. 외부 의존성 Mock
```java
// DB 접근 → Mock
@Mock
private AccountRepository accountRepository;

// 암호화 → Mock
@Mock
private PasswordEncoder passwordEncoder;
```

### 3. 테스트 순서 무관
```java
// @TestMethodOrder 사용 금지
// 각 테스트가 독립적으로 실행 가능해야 함
```

---

## 📊 테스트 네이밍 컨벤션

```java
// 형식: 메서드명_시나리오_예상결과
signup_성공_USER()
signup_이메일형식오류_골뱅이없음()
getAccountById_존재하지않음()

// @DisplayName: 한글 설명
@DisplayName("회원가입 성공 - USER 타입")
```

---

## ✅ 단위 테스트 체크리스트

- [ ] **Mock 설정**: @Mock, @InjectMocks 사용
- [ ] **Stubbing**: when-thenReturn 정의
- [ ] **실행**: 테스트 대상 메서드 호출
- [ ] **검증**: assertThat, verify 사용
- [ ] **예외 처리**: assertThatThrownBy
- [ ] **독립성**: @BeforeEach 초기화
- [ ] **네이밍**: 메서드명_시나리오_결과
- [ ] **@DisplayName**: 한글 설명

---

## 🎯 자주 사용하는 패턴

### Optional.empty() 테스트
```java
when(repository.findById(999L)).thenReturn(Optional.empty());

assertThatThrownBy(() -> service.getById(999L))
    .isInstanceOf(NotFoundException.class);
```

### DataIntegrityViolationException (중복)
```java
when(repository.save(any())).thenThrow(
    new DataIntegrityViolationException("Duplicate entry")
);

assertThatThrownBy(() -> service.signup(request))
    .isInstanceOf(DuplicateEmailException.class);
```

### 비밀번호 암호화 검증
```java
verify(passwordEncoder).encode("password123");

verify(repository).save(argThat(account ->
    account.getPassword().equals("encodedPassword")
));
```

---

## 🔗 관련 문서

- [SignupServiceImplTest](01_signup_service_test.md) - 회원가입 테스트 예시
- [ProfileServiceImplTest](02_profile_service_test.md) - 조회 테스트 예시
- [EmailUtilsTest](03_email_utils_test.md) - @ParameterizedTest 예시

---

## 📚 참고 자료

- [Mockito 공식 문서](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ 공식 문서](https://assertj.github.io/doc/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/reference/testing/index.html)
