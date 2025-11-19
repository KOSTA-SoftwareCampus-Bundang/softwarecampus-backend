# 2. API 명세서

**회원가입 및 인증 API 명세**

---

## 📋 엔드포인트 목록

| 메서드 | 엔드포인트 | 설명 | 상태 코드 |
|--------|-----------|------|-----------|
| POST | /api/v1/auth/signup | 회원가입 | 201, 400, 409 |
| GET | /api/v1/auth/check-email | 이메일 중복 확인 | 200, 400 |

---

## 1. POST /api/v1/auth/signup (회원가입)

### 요청

```http
POST /api/v1/auth/signup HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123!",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구",
  "affiliation": null,
  "position": null,
  "accountType": "USER",
  "academyId": null
}
```

**Request Body:**
```json
{
  "email": "string (필수)",
  "password": "string (필수, 8자 이상)",
  "userName": "string (필수)",
  "phoneNumber": "string (필수, 010-XXXX-XXXX)",
  "address": "string (선택)",
  "affiliation": "string (선택)",
  "position": "string (선택)",
  "accountType": "USER | ACADEMY (필수)",
  "academyId": "number (ACADEMY 타입 필수)"
}
```

---

### 응답 (201 Created - USER)

```http
HTTP/1.1 201 Created
Location: /api/v1/accounts/1
Content-Type: application/json

{
  "id": 1,
  "email": "user@example.com",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구",
  "affiliation": null,
  "position": null,
  "accountType": "USER",
  "accountApproved": "APPROVED",
  "academyId": null,
  "createdAt": "2024-01-01T12:00:00"
}
```

**Response Body:**
- `id`: 생성된 계정 ID
- `accountType`: USER
- `accountApproved`: APPROVED (즉시 승인)
- `academyId`: null

---

### 응답 (201 Created - ACADEMY)

```http
POST /api/v1/auth/signup HTTP/1.1
Content-Type: application/json

{
  "email": "teacher@example.com",
  "password": "password123!",
  "userName": "김선생",
  "phoneNumber": "010-9876-5432",
  "address": "서울시 서초구",
  "affiliation": "ABC학원",
  "position": "수학 강사",
  "accountType": "ACADEMY",
  "academyId": 100
}
```

```http
HTTP/1.1 201 Created
Location: /api/v1/accounts/2
Content-Type: application/json

{
  "id": 2,
  "email": "teacher@example.com",
  "userName": "김선생",
  "phoneNumber": "010-9876-5432",
  "address": "서울시 서초구",
  "affiliation": "ABC학원",
  "position": "수학 강사",
  "accountType": "ACADEMY",
  "accountApproved": "PENDING",
  "academyId": 100,
  "createdAt": "2024-01-01T12:05:00"
}
```

**Response Body:**
- `accountType`: ACADEMY
- `accountApproved`: PENDING (승인 대기)
- `academyId`: 100

---

### 에러 응답 (400 Bad Request - Bean Validation)

```http
POST /api/v1/auth/signup HTTP/1.1
Content-Type: application/json

{
  "email": "invalid-email",
  "password": "123",
  "userName": "",
  "phoneNumber": ""
}
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "요청 본문에 유효하지 않은 필드가 있습니다.",
  "errors": {
    "email": "이메일 형식이 올바르지 않습니다.",
    "password": "비밀번호는 8자 이상이어야 합니다.",
    "userName": "이름은 필수입니다.",
    "phoneNumber": "전화번호는 필수입니다."
  }
}
```

---

### 에러 응답 (400 Bad Request - RFC 5322 위반)

```http
POST /api/v1/auth/signup HTTP/1.1
Content-Type: application/json

{
  "email": "user@-invalid.com",
  "password": "password123!",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "accountType": "USER"
}
```

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

### 에러 응답 (409 Conflict - 이메일 중복)

```http
POST /api/v1/auth/signup HTTP/1.1
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123!",
  "userName": "이순신",
  "phoneNumber": "010-5555-6666",
  "accountType": "USER"
}
```

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

### 에러 응답 (400 Bad Request - 전화번호 중복)

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/invalid-input",
  "title": "Invalid Input",
  "status": 400,
  "detail": "이미 사용 중인 전화번호입니다."
}
```

---

### 에러 응답 (400 Bad Request - ADMIN 차단)

```http
POST /api/v1/auth/signup HTTP/1.1
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "password123!",
  "userName": "관리자",
  "phoneNumber": "010-0000-0000",
  "accountType": "ADMIN"
}
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/invalid-input",
  "title": "Invalid Input",
  "status": 400,
  "detail": "관리자 계정은 회원가입으로 생성할 수 없습니다."
}
```

---

### 에러 응답 (400 Bad Request - ACADEMY academyId 누락)

```http
POST /api/v1/auth/signup HTTP/1.1
Content-Type: application/json

{
  "email": "teacher@example.com",
  "password": "password123!",
  "userName": "김선생",
  "phoneNumber": "010-9876-5432",
  "accountType": "ACADEMY",
  "academyId": null
}
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.softwarecampus.com/problems/invalid-input",
  "title": "Invalid Input",
  "status": 400,
  "detail": "기관 회원은 기관 ID가 필수입니다."
}
```

---

## 2. GET /api/v1/auth/check-email (이메일 중복 확인)

### 요청 (사용 가능)

```http
GET /api/v1/auth/check-email?email=newuser@example.com HTTP/1.1
Host: localhost:8080
```

**Query Parameters:**
- `email` (required): 확인할 이메일 주소

---

### 응답 (200 OK - 사용 가능)

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "사용 가능한 이메일입니다."
}
```

---

### 응답 (200 OK - 사용 불가)

```http
GET /api/v1/auth/check-email?email=user@example.com HTTP/1.1
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "이미 사용 중인 이메일입니다."
}
```

---

### 에러 응답 (400 Bad Request - 이메일 형식 오류)

```http
GET /api/v1/auth/check-email?email=invalid-email HTTP/1.1
```

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

## 📊 상태 코드 정리

| 상태 코드 | 설명 | 발생 조건 |
|----------|------|----------|
| 200 OK | 조회 성공 | GET /check-email |
| 201 Created | 생성 성공 | POST /signup 성공 |
| 400 Bad Request | 요청 오류 | Bean Validation 실패, 이메일 형식 오류, 전화번호 중복, ADMIN 차단, ACADEMY academyId 누락 |
| 409 Conflict | 리소스 충돌 | 이메일 중복 |
| 500 Internal Server Error | 서버 오류 | 예상치 못한 서버 오류 |

---

## 📝 ProblemDetail (RFC 9457)

```json
{
  "type": "https://api.softwarecampus.com/problems/{문제유형}",
  "title": "사람이 읽을 수 있는 제목",
  "status": 400,
  "detail": "구체적인 설명"
}
```

**필드 설명:**
- `type`: 문제 유형을 식별하는 URI
- `title`: 간단한 제목
- `status`: HTTP 상태 코드
- `detail`: 상세 설명
- `errors` (선택): Bean Validation 실패 시 필드별 오류

---

## 🔗 관련 문서

- [AuthController 구현](01_auth_controller.md) - Controller 코드
- [Controller 테스트](03_controller_test.md) - MockMvc 테스트
- [보안 및 RESTful 원칙](04_security_restful.md) - Postman 테스트 예시
