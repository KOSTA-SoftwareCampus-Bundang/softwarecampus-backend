# 프로필 조회 시나리오

프로필 조회 기능의 전체 테스트 시나리오를 정의합니다.

## 목차
- [Phase 5: Service Layer 테스트](#phase-5-service-layer-테스트)
- [Phase 6: 단위 테스트 (Mockito)](#phase-6-단위-테스트-mockito)
- [Phase 7: API 통합 테스트](#phase-7-api-통합-테스트)

---

## Phase 5: Service Layer 테스트

Service 메서드 동작 검증 시나리오

### 시나리오 1: ID로 계정 조회 성공

**Given:**
- DB에 존재하는 계정 ID

**When:**
```java
AccountResponse response = profileService.getAccountById(1L);
```

**Then:**
- `AccountResponse` 반환
- 계정 정보 일치
- 로그: "계정 조회 시도: accountId=1"

---

### 시나리오 2: ID로 계정 조회 실패 (존재하지 않음)

**Given:**
- DB에 없는 계정 ID

**When:**
```java
profileService.getAccountById(999L);
```

**Then:**
- `AccountNotFoundException` 발생
- 예외 메시지: "계정을 찾을 수 없습니다."
- 로그: "계정 조회 시도: accountId=999"

---

### 시나리오 3: 이메일로 계정 조회 성공

**Given:**
- DB에 존재하는 이메일
- 유효한 이메일 형식

**When:**
```java
AccountResponse response = profileService.getAccountByEmail("user@example.com");
```

**Then:**
- `AccountResponse` 반환
- 계정 정보 일치
- 로그: "계정 조회 시도: email=u***@e***.com" (마스킹)

---

### 시나리오 4: 이메일로 계정 조회 실패 (존재하지 않음)

**Given:**
- DB에 없는 이메일
- 유효한 이메일 형식

**When:**
```java
profileService.getAccountByEmail("notfound@example.com");
```

**Then:**
- `AccountNotFoundException` 발생
- 예외 메시지: "계정을 찾을 수 없습니다."
- 로그: "계정 조회 시도: email=n***@e***.com" (마스킹)

---

### 시나리오 5: 이메일 null/blank

**Given:**
- 이메일이 null 또는 빈 문자열

**When:**
```java
profileService.getAccountByEmail(null);  // 또는 ""
```

**Then:**
- `InvalidInputException` 발생
- 예외 메시지: "이메일을 입력해주세요."
- 로그: "Invalid email input: null or blank"

---

### 시나리오 6: 잘못된 이메일 형식

**Given:**
- 유효하지 않은 이메일 형식

**When:**
```java
profileService.getAccountByEmail("invalid-email");
```

**Then:**
- `InvalidInputException` 발생
- 예외 메시지: "올바른 이메일 형식이 아닙니다."
- 로그: "Invalid email format: i***" (마스킹)

---

## Phase 6: 단위 테스트 (Mockito)

Mockito를 사용한 단위 테스트 케이스

### Test 1: `getAccountById_성공()`

**Mock 설정:**
```java
Account mockAccount = Account.builder()
    .id(1L)
    .email("user@example.com")
    .userName("홍길동")
    .phoneNumber("010-1234-5678")
    .accountType(AccountType.USER)
    .accountApproved(ApprovalStatus.APPROVED)
    .build();

when(accountRepository.findById(1L))
    .thenReturn(Optional.of(mockAccount));
```

**실행:**
```java
AccountResponse response = profileService.getAccountById(1L);
```

**검증:**
```java
assertThat(response.id()).isEqualTo(1L);
assertThat(response.email()).isEqualTo("user@example.com");
verify(accountRepository).findById(1L);
```

---

### Test 2: `getAccountById_존재하지않음_예외발생()`

**Mock 설정:**
```java
when(accountRepository.findById(999L))
    .thenReturn(Optional.empty());
```

**실행 & 검증:**
```java
assertThatThrownBy(() -> profileService.getAccountById(999L))
    .isInstanceOf(AccountNotFoundException.class)
    .hasMessage("계정을 찾을 수 없습니다.");
```

---

### Test 3: `getAccountByEmail_성공()`

**Mock 설정:**
```java
when(accountRepository.findByEmail("user@example.com"))
    .thenReturn(Optional.of(mockAccount));
```

**실행:**
```java
AccountResponse response = profileService.getAccountByEmail("user@example.com");
```

**검증:**
```java
assertThat(response.email()).isEqualTo("user@example.com");
verify(accountRepository).findByEmail("user@example.com");
```

---

### Test 4: `getAccountByEmail_잘못된형식_예외발생()`

**입력:**
```java
String invalidEmail = "invalid-email";
```

**검증:**
```java
assertThatThrownBy(() -> profileService.getAccountByEmail(invalidEmail))
    .isInstanceOf(InvalidInputException.class)
    .hasMessage("올바른 이메일 형식이 아닙니다.");
```

---

## Phase 7: API 통합 테스트

REST API 엔드포인트 테스트

### API Test 1: GET /api/v1/accounts/{id} (성공)

**Request:**
```http
GET /api/v1/accounts/1
Authorization: Bearer {token}
```

**Expected Response:**
```http
HTTP/1.1 200 OK
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

### API Test 2: GET /api/v1/accounts/{id} (존재하지 않음)

**Request:**
```http
GET /api/v1/accounts/999
Authorization: Bearer {token}
```

**Expected Response:**
```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/account-not-found",
  "title": "Account Not Found",
  "status": 404,
  "detail": "계정을 찾을 수 없습니다."
}
```

---

### API Test 3: GET /api/v1/accounts/email/{email} (성공)

**Request:**
```http
GET /api/v1/accounts/email/user@example.com
Authorization: Bearer {token}
```

**Expected Response:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 1,
  "email": "user@example.com",
  ...
}
```

---

### API Test 4: GET /api/v1/accounts/email/{email} (잘못된 형식)

**Request:**
```http
GET /api/v1/accounts/email/invalid-email
Authorization: Bearer {token}
```

**Expected Response:**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/invalid-input",
  "title": "Invalid Input",
  "status": 400,
  "detail": "올바른 이메일 형식이 아닙니다."
}
```

---

### API Test 5: GET /api/v1/accounts/{id} (인증 실패)

**Request:**
```http
GET /api/v1/accounts/1
(Authorization 헤더 없음)
```

**Expected Response:**
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "인증이 필요합니다."
}
```

---

## 테스트 데이터

### 존재하는 계정 (DB 준비)
```java
Account testUser = Account.builder()
    .id(1L)
    .email("user@example.com")
    .userName("홍길동")
    .phoneNumber("010-1234-5678")
    .accountType(AccountType.USER)
    .accountApproved(ApprovalStatus.APPROVED)
    .build();

Account testAcademy = Account.builder()
    .id(2L)
    .email("academy@example.com")
    .userName("소프트웨어캠퍼스")
    .phoneNumber("010-9876-5432")
    .accountType(AccountType.ACADEMY)
    .accountApproved(ApprovalStatus.PENDING)
    .affiliation("교육기관")
    .build();
```

---

**최종 수정:** 2025-11-05
