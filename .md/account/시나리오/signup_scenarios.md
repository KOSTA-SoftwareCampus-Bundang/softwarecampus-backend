# 회원가입 시나리오

회원가입 기능의 전체 테스트 시나리오를 정의합니다.

## 목차
- [Phase 5: Service Layer 테스트](#phase-5-service-layer-테스트)
- [Phase 6: 단위 테스트 (Mockito)](#phase-6-단위-테스트-mockito)
- [Phase 7: API 통합 테스트](#phase-7-api-통합-테스트)

---

## Phase 5: Service Layer 테스트

Service 메서드 동작 검증 시나리오

### 시나리오 1: 일반 사용자 회원가입 성공

**Given:**
- 새로운 이메일 (DB에 없음)
- 유효한 입력값
- `affiliation` 필드 없음

**When:**
```java
SignupRequest request = new SignupRequest(
    "user@example.com",
    "password123",
    "홍길동",
    "010-1234-5678",
    "서울시 강남구",
    null,  // affiliation 없음
    null
);
AccountResponse response = signupService.signup(request);
```

**Then:**
- `AccountResponse` 반환
- `accountType` = `USER`
- `accountApproved` = `APPROVED`
- `password`가 암호화되어 저장됨
- `accountId`가 생성됨

---

### 시나리오 2: 기관 회원가입 성공

**Given:**
- 새로운 이메일
- 유효한 입력값
- `affiliation` 필드 존재

**When:**
```java
SignupRequest request = new SignupRequest(
    "academy@example.com",
    "password123",
    "소프트웨어캠퍼스",
    "010-9876-5432",
    "서울시 서초구",
    "교육기관",  // affiliation 있음
    "대표"
);
AccountResponse response = signupService.signup(request);
```

**Then:**
- `AccountResponse` 반환
- `accountType` = `ACADEMY`
- `accountApproved` = `PENDING` (관리자 승인 대기)
- `password`가 암호화되어 저장됨

---

### 시나리오 3: 이메일 중복 (DB UNIQUE 제약)

**Given:**
- 이미 DB에 존재하는 이메일
- 유효한 입력값

**When:**
```java
SignupRequest request = new SignupRequest(
    "existing@example.com",  // 이미 존재
    "password123",
    "홍길동",
    "010-1234-5678",
    null, null, null
);
signupService.signup(request);
```

**Then:**
- `DuplicateEmailException` 발생
- 예외 메시지: "이미 사용 중인 이메일입니다."
- 로그: "Email duplicate detected during database insert" (이메일 노출 없음)

---

### 시나리오 4: 전화번호 중복 (DB UNIQUE 제약)

**Given:**
- 새로운 이메일
- 이미 존재하는 전화번호

**When:**
```java
SignupRequest request = new SignupRequest(
    "new@example.com",
    "password123",
    "홍길동",
    "010-1234-5678",  // 이미 존재
    null, null, null
);
signupService.signup(request);
```

**Then:**
- `InvalidInputException` 발생
- 예외 메시지: "이미 사용 중인 전화번호입니다."
- 로그: "Phone number duplicate detected during database insert"

---

### 시나리오 5: 잘못된 이메일 형식

**Given:**
- 유효하지 않은 이메일 형식

**When:**
```java
SignupRequest request = new SignupRequest(
    "invalid-email",  // @ 없음
    "password123",
    "홍길동",
    "010-1234-5678",
    null, null, null
);
signupService.signup(request);
```

**Then:**
- `InvalidInputException` 발생
- 예외 메시지: "올바른 이메일 형식이 아닙니다."
- 로그: "Invalid email format detected: i***" (마스킹)

---

### 시나리오 6: 이메일 null/blank

**Given:**
- 이메일이 null 또는 빈 문자열

**When:**
```java
SignupRequest request = new SignupRequest(
    null,  // 또는 ""
    "password123",
    "홍길동",
    "010-1234-5678",
    null, null, null
);
signupService.signup(request);
```

**Then:**
- `InvalidInputException` 발생
- 예외 메시지: "이메일을 입력해주세요."
- 로그: "Invalid email input: null or blank"

---

### 시나리오 7: 동시성 테스트 (Race Condition)

**Given:**
- 동일한 이메일로 동시에 2개 요청

**When:**
```java
// Thread 1, 2가 동시에 실행
CompletableFuture<AccountResponse> future1 = 
    CompletableFuture.supplyAsync(() -> signupService.signup(request));
CompletableFuture<AccountResponse> future2 = 
    CompletableFuture.supplyAsync(() -> signupService.signup(request));
```

**Then:**
- 하나는 성공 (201 Created)
- 하나는 `DuplicateEmailException` (409 Conflict)
- DB에는 1개만 저장됨 (UNIQUE 제약 보장)

---

## Phase 6: 단위 테스트 (Mockito)

Mockito를 사용한 단위 테스트 케이스

### Test 1: `signup_성공_일반사용자()`

**Mock 설정:**
```java
when(accountRepository.existsByEmail(anyString())).thenReturn(false);
when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
```

**실행:**
```java
AccountResponse response = signupService.signup(request);
```

**검증:**
```java
assertThat(response.accountType()).isEqualTo(AccountType.USER);
assertThat(response.accountApproved()).isEqualTo(ApprovalStatus.APPROVED);
verify(accountRepository).save(any(Account.class));
verify(passwordEncoder).encode(anyString());
```

---

### Test 2: `signup_이메일중복_예외발생()`

**Mock 설정:**
```java
when(accountRepository.save(any(Account.class)))
    .thenThrow(new DataIntegrityViolationException("uk_account_email"));
```

**실행 & 검증:**
```java
assertThatThrownBy(() -> signupService.signup(request))
    .isInstanceOf(DuplicateEmailException.class)
    .hasMessage("이미 사용 중인 이메일입니다.");
```

---

### Test 3: `signup_잘못된이메일형식_예외발생()`

**입력:**
```java
SignupRequest request = new SignupRequest(
    "invalid-email", ...
);
```

**검증:**
```java
assertThatThrownBy(() -> signupService.signup(request))
    .isInstanceOf(InvalidInputException.class)
    .hasMessage("올바른 이메일 형식이 아닙니다.");
```

---

## Phase 7: API 통합 테스트

REST API 엔드포인트 테스트

### API Test 1: POST /api/v1/auth/signup (성공)

**Request:**
```http
POST /api/v1/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구"
}
```

**Expected Response:**
```http
HTTP/1.1 201 Created
Location: /api/v1/accounts/1
Content-Type: application/json

{
  "id": 1,
  "email": "user@example.com",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "accountType": "USER",
  "accountApproved": "APPROVED",
  "address": "서울시 강남구",
  "affiliation": null,
  "position": null
}
```

---

### API Test 2: POST /api/v1/auth/signup (이메일 중복)

**Request:**
```http
POST /api/v1/auth/signup
Content-Type: application/json

{
  "email": "existing@example.com",
  ...
}
```

**Expected Response:**
```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/duplicate-email",
  "title": "Duplicate Email",
  "status": 409,
  "detail": "이미 사용 중인 이메일입니다."
}
```

---

### API Test 3: POST /api/v1/auth/signup (입력 검증 실패)

**Request:**
```http
POST /api/v1/auth/signup
Content-Type: application/json

{
  "email": "invalid-email",
  "password": "123",
  "userName": "",
  "phoneNumber": "invalid"
}
```

**Expected Response:**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "입력값이 올바르지 않습니다.",
  "errors": {
    "email": "올바른 이메일 형식이 아닙니다.",
    "password": "비밀번호는 8자 이상이어야 합니다.",
    "userName": "사용자명을 입력해주세요.",
    "phoneNumber": "올바른 전화번호 형식이 아닙니다."
  }
}
```

---

## 테스트 데이터

### 유효한 이메일 예시
```
user@example.com
test@test.co.kr
name+tag@domain.com
user@example.technology      (긴 TLD)
user@example.xn--3e0b707e    (국제화 도메인)
```

### 유효하지 않은 이메일 예시
```
invalid-email              (@ 없음)
@example.com               (local part 없음)
user@                      (domain 없음)
user@.com                  (도메인 시작이 .)
user@domain                (TLD 없음)
```

---

**최종 수정:** 2025-11-05
