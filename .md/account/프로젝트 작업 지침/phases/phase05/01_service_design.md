# 1. Service Layer 설계 원칙

## 📂 생성/수정 파일

### 새로 생성된 파일:
```text
src/main/java/com/softwarecampus/backend/
├─ service/user/
│  ├─ signup/
│  │  ├─ SignupService.java              ✅ 회원가입 인터페이스
│  │  └─ SignupServiceImpl.java          ✅ 회원가입 구현
│  └─ profile/
│     ├─ ProfileService.java             ✅ 프로필 인터페이스
│     └─ ProfileServiceImpl.java         ✅ 프로필 구현
├─ exception/user/
│  ├─ InvalidInputException.java         ✅ 잘못된 입력 예외
│  ├─ DuplicateEmailException.java       ✅ 이메일 중복 예외
│  └─ AccountNotFoundException.java      ✅ 계정 미존재 예외
├─ validation/
│  ├─ ValidAccountType.java              ✅ 커스텀 검증 애노테이션
│  └─ AccountTypeValidator.java          ✅ ACADEMY academyId 검증 로직
└─ util/
   └─ EmailUtils.java                    ✅ 이메일 검증/마스킹 유틸

.md/account/시나리오/
├─ README.md                             ✅ 시나리오 목록
├─ signup_scenarios.md                   ✅ 회원가입 시나리오
└─ profile_scenarios.md                  ✅ 프로필 조회 시나리오
```

### 수정된 파일:
```text
src/main/java/com/softwarecampus/backend/
├─ exception/
│  └─ GlobalExceptionHandler.java        ✅ InvalidInputException, Bean Validation 핸들러 추가
├─ dto/user/
│  ├─ MessageResponse.java               ✅ Status 필드 제거 (RESTful)
│  └─ SignupRequest.java                 ✅ Bean Validation 애노테이션 + @ValidAccountType 추가
```

---

## 🎯 설계 결정 사항

### 0. 입력 유효성 검사 전략

**결정:** 3계층 검증 구조 (Controller → Service → Database)

#### (1) Controller 계층 - 형식 검증 (Syntactic Validation)

**Bean Validation 애노테이션 사용:**
```java
@ValidAccountType  // 클래스 레벨 커스텀 검증
public record SignupRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20)
    String password,
    
    @NotNull(message = "계정 타입은 필수입니다")
    AccountType accountType,
    
    Long academyId  // ACADEMY 타입일 때만 필수 (USER/ADMIN은 null 허용)
) {}

// Controller에서 @Valid 사용
@PostMapping("/signup")
public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request) {
    // Bean Validation이 자동으로 검증
}
```

**커스텀 검증 애노테이션 (`@ValidAccountType`):**

`@ValidAccountType`은 클래스 레벨에서 적용되는 커스텀 Bean Validation 애노테이션으로, 계정 타입에 따른 조건부 필수 필드 검증을 수행합니다.

**구현 파일:**
- `validation/ValidAccountType.java`: 커스텀 애노테이션 정의
- `validation/AccountTypeValidator.java`: 실제 검증 로직 구현

**검증 규칙:**
1. **ACADEMY 타입** → `academyId != null` (필수)
   - ACADEMY 계정은 반드시 소속 학원 ID를 제공해야 함
   - `academyId`가 null이면 검증 실패
   
2. **USER/ADMIN 타입** → `academyId` 무시 (null 허용)
   - USER, ADMIN 계정은 학원과 무관하므로 `academyId` 검증하지 않음
   - `academyId`가 제공되어도 무시됨 (Service에서 null로 설정)

**에러 메시지:**
- 기본 메시지: `"ACADEMY 타입일 경우 academyId는 필수입니다"`
- HTTP 응답: GlobalExceptionHandler가 400 Bad Request로 변환

**구현 예시:**
```java
// ValidAccountType.java (애노테이션 정의)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AccountTypeValidator.class)
public @interface ValidAccountType {
    String message() default "ACADEMY 타입일 경우 academyId는 필수입니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// AccountTypeValidator.java (검증 로직)
public class AccountTypeValidator implements ConstraintValidator<ValidAccountType, SignupRequest> {
    @Override
    public boolean isValid(SignupRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;  // null 요청은 @NotNull에서 처리
        }
        
        // ACADEMY 타입일 때만 academyId 필수 검증
        if (request.accountType() == AccountType.ACADEMY) {
            return request.academyId() != null;
        }
        
        // USER, ADMIN 타입은 academyId 검증 안 함
        return true;
    }
}
```

**검증 시점:**
- Controller 진입 전 (`@Valid` 애노테이션에 의해 자동 실행)
- `MethodArgumentNotValidException` 발생 시 GlobalExceptionHandler가 400 응답 생성

**장점:**
- 선언적 검증: 코드가 깔끔하고 재사용 가능
- 일관된 에러 처리: GlobalExceptionHandler와 자동 통합
- 테스트 용이: 단위 테스트로 검증 로직 독립 검증 가능

#### (2) EmailUtils 역할 명확화

**EmailUtils가 담당하는 검증:**
- RFC 5322/5321/1035 복잡한 정규식 검증 (`isValidFormat()`)
- 로컬 파트 길이 검증 (RFC 5321: 최대 64자)
- 연속 점(`.`) 검증 (Bean Validation의 `@Email`이 놓치는 부분)

**EmailUtils가 담당하지 않는 검증:**
- 필수 여부 검증 → `@NotBlank`
- 기본 이메일 형식 검증 → `@Email`

**EmailUtils 추가 기능:**
- PII 마스킹: `maskEmail()` (로깅용)

**사용 예시:**
```java
// Controller: @Email로 기본 형식 검증 (간단한 케이스)
@Email String email;

// Service: EmailUtils로 RFC 정밀 검증 (복잡한 케이스)
if (!EmailUtils.isValidFormat(email)) {
    throw new InvalidInputException("올바른 이메일 형식이 아닙니다.");
}
```

#### (3) Service 계층 - 비즈니스 규칙 검증

**Service가 담당하는 검증:**
- 이메일 중복 검증 → `DuplicateEmailException`
- ADMIN 계정 차단 → `InvalidInputException`
- 복잡한 이메일 형식 (EmailUtils 사용) → `InvalidInputException`
- 도메인 로직 검증 (권한, 상태 등)

**Service가 담당하지 않는 검증:**
- 필수 필드, 길이, 형식 등 → Bean Validation

#### (4) GlobalExceptionHandler - 일관된 에러 응답

**Bean Validation 실패 처리:**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ProblemDetail> handleValidationException(
    MethodArgumentNotValidException ex) {
    
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "요청 본문에 유효하지 않은 필드가 있습니다."
    );
    
    // 필드별 에러 메시지 수집
    Map<String, String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            FieldError::getDefaultMessage
        ));
    
    problemDetail.setProperty("errors", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
}
```

**도메인 예외 처리:**
```java
@ExceptionHandler(InvalidInputException.class)
public ResponseEntity<ProblemDetail> handleInvalidInput(InvalidInputException ex) {
    // 400 Bad Request
}

@ExceptionHandler(DuplicateEmailException.class)
public ResponseEntity<ProblemDetail> handleDuplicateEmail(DuplicateEmailException ex) {
    // 409 Conflict
}
```

**검증 실패 응답 예시:**
```json
{
  "type": "https://api.프로젝트주소/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "요청 본문에 유효하지 않은 필드가 있습니다.",
  "errors": {
    "email": "유효한 이메일 형식이 아닙니다",
    "password": "비밀번호는 8~20자여야 합니다"
  }
}
```

### 1. 기능별 독립 패키지
**결정:** signup/login/profile 별도 패키지로 분리

**이유:**
- 각 기능이 명확히 분리 (회원가입/로그인/프로필)
- Phase별 독립적 작업 가능
- 폴더 구조만 봐도 기능 파악 가능
- 테스트 파일도 같은 구조로 분리 가능

### 2. 인터페이스 + 구현체 쌍
**결정:** 각 Service는 인터페이스와 구현체로 구성

**이유:**
- 테스트 시 Mock 객체 주입 용이
- 명확한 계약(Contract) 정의
- 향후 다른 구현체로 교체 가능 (유연성)
- Spring 권장 패턴

### 3. Facade 패턴 제거
**결정:** Controller가 각 Service를 직접 주입

**이유:**
- 불필요한 중간 계층 제거 (단순화)
- 각 Service가 독립적이므로 Facade 불필요
- Controller 코드가 더 명확해짐
- 파일 개수 감소

### 4. 계정 타입 기본값
**결정:** `accountType = USER`, `accountApproved = APPROVED`

**이유:**
- 일반 사용자는 즉시 승인
- 학원 계정은 별도 API로 처리 (관리자 승인 필요)
- Phase 5에서는 일반 회원가입만 처리
- 실제 Entity 필드명 `accountApproved` 사용

### 5. DTO 변환 위치
**결정:** Service Layer에서 Entity ↔ DTO 변환

**이유:**
- Controller는 HTTP 처리에만 집중
- Repository는 Entity만 다룸
- Service가 비즈니스 로직 + 변환 담당

**매핑 방식:**
- **수동 매핑** (MapStruct 미사용)
- Builder 패턴 또는 생성자를 사용한 명시적 변환
- 타입 안전성과 가독성 우선

**변환 예시:**
```java
// Entity → DTO
private AccountResponse toAccountResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getEmail(),
        account.getUserName(),
        // ... 모든 필드 명시적 매핑
    );
}
```

**예외 처리 및 유효성 검사:**

1. **입력 유효성 검사 (Controller → Service)**
   - Controller: `@Valid` Bean Validation (형식 검증)
   - Service: 비즈니스 로직 검증 (중복, 권한, 상태 등)
   - Null 검증: `Objects.requireNonNull(request, "message")`

2. **변환 중 Null 안전성**
   - Entity getter는 null 가능 (address, affiliation 등)
   - DTO 생성 시 그대로 전달 (null 허용 필드)
   - 필수 필드(id, email 등)는 DB NOT NULL 제약으로 보장

3. **변환 실패 처리**
   - 타입 불일치: 컴파일 타임 검증 (수동 매핑의 장점)
   - Null 참조: 필수 필드는 `Objects.requireNonNull()` 사용
   - 로깅: `log.error("Entity to DTO conversion failed", exception)`
   - 예외 전파: `IllegalStateException` → 500 Internal Server Error

4. **변환 일관성 규칙**
   - private 메서드로 변환 로직 캡슐화
   - 메서드명: `toXxxResponse()`, `fromXxxRequest()`
   - Entity → DTO: 모든 필드 명시적 나열
   - DTO → Entity: Builder 패턴 사용

### 6. 트랜잭션 전략
**결정:** 클래스 레벨 `readOnly=true`, 쓰기 메서드만 `@Transactional`

**이유:**
- 읽기 작업이 대부분 → 기본값 읽기 전용
- 쓰기 작업만 명시적으로 `@Transactional` 선언
- 불필요한 트랜잭션 오버헤드 최소화

### 7. 예외 타입
**결정:** RuntimeException (Unchecked Exception)

**이유:**
- Spring은 RuntimeException만 자동 롤백
- 비즈니스 예외는 필수 처리 불필요
- GlobalExceptionHandler에서 일괄 처리

**도메인 예외와 HTTP 상태 코드 매핑:**

GlobalExceptionHandler가 다음 규칙에 따라 자동으로 HTTP 응답을 생성합니다 (참조: `GlobalExceptionHandler.java` 라인 40-142):

| 예외 타입 | HTTP 상태 코드 | 설명 | Handler 라인 |
|-----------|---------------|------|-------------|
| `MethodArgumentNotValidException` | 400 Bad Request | Bean Validation 실패 (@Valid 어노테이션) | 40-62 |
| `ConstraintViolationException` | 400 Bad Request | Request Parameter/Path Variable Validation 실패 | 68-88 |
| `InvalidInputException` | 400 Bad Request | 이메일 형식 오류, ADMIN 차단, ACADEMY academyId 누락 등 비즈니스 규칙 위반 | 120-135 |
| `DuplicateEmailException` | 409 Conflict | 이메일 중복 (UNIQUE 제약 위반) | 141-156 |
| `DataIntegrityViolationException` | 409 Conflict | 데이터베이스 제약 조건 위반 (UNIQUE, FOREIGN KEY 등) - 필요시 추가 |
| `AccountNotFoundException` | 404 Not Found | 계정을 찾을 수 없음 | 162-177 |
| `OptimisticLockingFailureException` | 409 Conflict | 낙관적 잠금 실패 (동시성 충돌) - 필요시 추가 | - |
| `AuthenticationException` | 401 Unauthorized | 인증 실패 (Phase 16 JWT 로그인에서 추가 예정) | - |
| `AccessDeniedException` | 403 Forbidden | 권한 부족 (Phase 16 이후 추가 예정) | - |
| `Exception` (fallback) | 500 Internal Server Error | 예상치 못한 서버 오류 | 94-106 |

**매핑 우선순위 및 폴백 규칙:**
1. **구체적인 예외부터 처리**: Spring은 `@ExceptionHandler`를 상속 관계 기준으로 가장 구체적인 핸들러를 먼저 적용
2. **도메인 예외 우선**: 프로젝트의 커스텀 예외(`InvalidInputException` 등)가 Spring 기본 예외보다 먼저 처리
3. **폴백 핸들러**: 매핑되지 않은 모든 예외는 `Exception` 핸들러가 500 응답 생성 (라인 94-106)
4. **데이터베이스 예외**: `DataIntegrityViolationException`은 필요시 명시적으로 추가 가능 (현재는 도메인 예외로 변환하여 처리)

**RFC 9457 ProblemDetail 응답 형식:**
- 모든 예외 응답은 표준 RFC 9457 형식을 따름
- **필수 필드**:
  - `type`: 문제 유형 식별 URI (예: `https://api.softwarecampus.com/problems/invalid-input`)
  - `title`: 사람이 읽을 수 있는 짧은 요약 (영문, 예: "Invalid Input")
  - `status`: HTTP 상태 코드 (예: 400)
  - `detail`: 구체적인 오류 설명 (한글, 예: "이메일 형식이 올바르지 않습니다.")
- **선택 필드**:
  - `errors`: Bean Validation 실패 시 필드별 오류 맵 (MethodArgumentNotValidException에서 사용)

**PII 보호:**
- DEBUG 로그에서 개인정보(이메일 등) 노출 방지
- `EmailUtils.maskEmail()`을 사용하여 민감 정보 마스킹
- 프로덕션 환경에서는 상세 예외 메시지 숨김

### 8. 예외 패키지 구조
**결정:** 도메인별 예외 패키지 분리 (`exception/user/`)

**이유:**
- 도메인별 예외 관리 용이
- 확장성 (course, board 등 추가 예정)
- 예외 파일이 많아져도 정리된 구조 유지

---

## 📈 Phase별 확장 계획

- **Phase 5 (현재)**: Signup + Profile (조회만)
- **Phase 16**: `login/LoginService.java` + `login/LoginServiceImpl.java` 추가
- **Phase 18**: ProfileService 확장 (수정/삭제 기능 추가)
