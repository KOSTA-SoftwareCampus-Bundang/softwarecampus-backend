# 1. DTO Layer (로그인)

**목표:** 로그인 요청/응답 DTO 작성 및 Bean Validation 적용

---

## 📂 생성 파일

```
src/main/java/com/softwarecampus/backend/
└─ dto/user/
   ├─ LoginRequest.java
   └─ LoginResponse.java
```

---

## 1.1 LoginRequest.java

**경로:** `dto/user/LoginRequest.java`

**설명:** 로그인 요청 데이터를 담는 DTO

```java
package com.softwarecampus.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO
 * 
 * @param email 이메일 (필수, 이메일 형식)
 * @param password 비밀번호 (필수)
 * 
 * @author 태윤
 */
public record LoginRequest(
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    String password
) {}
```

**핵심 포인트:**
- **Java Record**: 불변 객체, 자동 생성자/getter/equals/hashCode
- **Bean Validation**: 
  - `@NotBlank`: null, 빈 문자열, 공백 문자열 방지
  - `@Email`: 이메일 형식 검증 (RFC 5322 기반)
- **보안 고려사항**:
  - 비밀번호는 패턴 검증 없음 (로그인 시점에는 이미 가입된 비밀번호를 받으므로)
  - `@Size` 제약 없음 (회원가입 시 이미 검증됨)

---

## 1.2 LoginResponse.java

**경로:** `dto/user/LoginResponse.java`

**설명:** 로그인 성공 응답 데이터 (JWT 토큰 포함)

```java
package com.softwarecampus.backend.dto.user;

/**
 * 로그인 응답 DTO
 * 
 * @param accessToken JWT Access Token (15분 유효)
 * @param refreshToken JWT Refresh Token (7일 유효)
 * @param tokenType 토큰 타입 (항상 "Bearer")
 * @param expiresIn Access Token 만료 시간 (초 단위, 900 = 15분)
 * @param account 사용자 계정 정보
 * 
 * @author 태윤
 */
public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    AccountResponse account
) {
    /**
     * 정적 팩토리 메서드: 로그인 성공 응답 생성
     * 
     * @param accessToken JWT Access Token
     * @param refreshToken JWT Refresh Token
     * @param expiresIn Access Token 만료 시간 (초)
     * @param account 사용자 계정 정보
     * @return LoginResponse
     */
    public static LoginResponse of(
        String accessToken, 
        String refreshToken, 
        Long expiresIn, 
        AccountResponse account
    ) {
        return new LoginResponse(
            accessToken, 
            refreshToken, 
            "Bearer",  // 고정값
            expiresIn, 
            account
        );
    }
}
```

**핵심 포인트:**
- **accessToken**: JWT Access Token (짧은 유효기간)
  - 모든 API 요청 시 Authorization 헤더에 포함
  - 예: `Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
  
- **refreshToken**: JWT Refresh Token (긴 유효기간)
  - Access Token 만료 시 갱신용
  - Redis에 저장 (key: `refresh:{email}`)
  
- **tokenType**: 항상 "Bearer" (OAuth 2.0 표준)

- **expiresIn**: Access Token 만료까지 남은 시간 (초)
  - 900초 = 15분 (JwtProperties.expiration 값)
  - 프론트엔드에서 자동 갱신 타이머 설정용
  
- **account**: 사용자 정보 (AccountResponse 재사용)
  - 로그인 직후 사용자 정보 표시용
  - 별도 `/api/mypage/profile` 호출 불필요

**정적 팩토리 메서드:**
- `tokenType`을 "Bearer"로 고정하여 실수 방지
- Service Layer에서 `LoginResponse.of()` 호출

---

## 📊 응답 예시

### 성공 응답 (200 OK)

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3MzI1MTUwMDAsImV4cCI6MTczMjUxNTkwMH0.abcd1234",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzMyNTE1MDAwLCJleHAiOjE3MzMxMTk4MDB9.efgh5678",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "account": {
    "id": 1,
    "email": "user@example.com",
    "userName": "홍길동",
    "phoneNumber": "010-1234-5678",
    "address": "서울시 강남구",
    "affiliation": null,
    "position": null,
    "accountType": "USER",
    "accountApproved": "APPROVED",
    "createdDate": "2024-11-23T10:30:00"
  }
}
```

---

## 🔗 다음 단계

DTO 생성 후:
1. **LoginService** 인터페이스 및 구현체 작성 ([02_service_layer.md](02_service_layer.md))
2. **AuthController** 로그인 메서드 추가 ([03_controller_layer.md](03_controller_layer.md))
