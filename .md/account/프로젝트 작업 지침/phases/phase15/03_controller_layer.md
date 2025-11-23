# 3. Controller Layer (마이페이지 API)

**목표:** 마이페이지 API 엔드포인트 구현

---

## 📂 생성 파일

```
src/main/java/com/softwarecampus/backend/
└─ controller/user/
   └─ MyPageController.java
```

---

## 3.1 MyPageController.java

**경로:** `controller/user/MyPageController.java`

**설명:** 마이페이지 API 컨트롤러 (프로필 조회 및 수정)

```java
package com.softwarecampus.backend.controller.user;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.service.user.profile.ProfileService;
import com.softwarecampus.backend.util.EmailUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지 API 컨트롤러
 * 
 * 엔드포인트:
 * - GET /api/mypage/profile: 프로필 조회
 * - PATCH /api/mypage/profile: 프로필 수정
 * 
 * 보안:
 * - JWT 인증 필수 (@AuthenticationPrincipal UserDetails)
 * - 본인 계정만 접근 가능
 * 
 * @author 태윤
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {
    
    private final ProfileService profileService;
    
    /**
     * 프로필 조회 API
     * 
     * @param userDetails 인증된 사용자 정보 (Spring Security)
     * @return 200 OK + AccountResponse
     * 
     * @throws UsernameNotFoundException 401 - 사용자 없음 (JWT 토큰 유효하지만 DB에 없음)
     */
    @GetMapping("/profile")
    public ResponseEntity<AccountResponse> getProfile(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        log.info("프로필 조회 API 호출: email={}", EmailUtils.maskEmail(email));
        
        AccountResponse response = profileService.getProfile(email);
        
        log.info("프로필 조회 성공: email={}", EmailUtils.maskEmail(email));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 프로필 수정 API
     * 
     * @param userDetails 인증된 사용자 정보 (Spring Security)
     * @param request 수정 요청 (userName, phoneNumber, address, affiliation, position)
     * @return 200 OK + AccountResponse (수정된 프로필)
     * 
     * @throws InvalidInputException 400 - 빈 요청 (모든 필드 null)
     * @throws InvalidInputException 400 - Bean Validation 실패
     * @throws InvalidInputException 409 - 전화번호 중복
     */
    @PatchMapping("/profile")
    public ResponseEntity<AccountResponse> updateProfile(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        String email = userDetails.getUsername();
        log.info("프로필 수정 API 호출: email={}", EmailUtils.maskEmail(email));
        
        AccountResponse response = profileService.updateProfile(email, request);
        
        log.info("프로필 수정 성공: email={}", EmailUtils.maskEmail(email));
        
        return ResponseEntity.ok(response);
    }
}
```

**핵심 포인트:**

### 1. @AuthenticationPrincipal UserDetails
```java
public ResponseEntity<AccountResponse> getProfile(
    @AuthenticationPrincipal UserDetails userDetails
)
```
- **Spring Security**: SecurityContext에서 인증된 사용자 정보 추출
- **UserDetails.getUsername()**: 이메일 (JWT에서 추출한 subject)
- **JWT 인증 필수**: 토큰 없으면 401 Unauthorized (JwtAuthenticationFilter 차단)

### 2. HTTP 메서드: GET vs PATCH
```java
@GetMapping("/profile")   // 조회
@PatchMapping("/profile")  // 부분 수정
```
- **GET**: 프로필 조회 (읽기 전용)
- **PATCH**: 프로필 부분 수정 (일부 필드만 변경)
- **PUT vs PATCH**:
  - PUT: 전체 리소스 교체 (모든 필드 필수)
  - PATCH: 부분 수정 (null 필드는 변경 안 함) ✅

### 3. Bean Validation
```java
public ResponseEntity<AccountResponse> updateProfile(
    @AuthenticationPrincipal UserDetails userDetails,
    @Valid @RequestBody UpdateProfileRequest request
)
```
- `@Valid`: UpdateProfileRequest의 `@Size`, `@Pattern` 검증
- 검증 실패 시 자동으로 400 Bad Request

### 4. 상태 코드: 200 OK
```java
return ResponseEntity.ok(response);
```
- **200 OK**: 프로필 조회/수정 성공
- **Location 헤더 불필요**: 새로운 리소스 생성이 아니므로

---

## 📋 API 명세

### GET /api/mypage/profile

**요청 (Request)**

```http
GET /api/mypage/profile HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**성공 응답 (200 OK)**

```http
HTTP/1.1 200 OK
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
  "createdDate": "2024-11-23T10:30:00"
}
```

**실패 응답 (401 Unauthorized) - JWT 토큰 없음**

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "JWT 토큰이 필요합니다"
}
```

---

### PATCH /api/mypage/profile

**요청 (Request)**

```http
PATCH /api/mypage/profile HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "userName": "홍길동 (수정)",
  "phoneNumber": "010-9999-8888",
  "address": null,
  "affiliation": null,
  "position": null
}
```

**성공 응답 (200 OK)**

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 1,
  "email": "user@example.com",
  "userName": "홍길동 (수정)",
  "phoneNumber": "010-9999-8888",
  "address": "서울시 강남구",
  "affiliation": null,
  "position": null,
  "accountType": "USER",
  "accountApproved": "APPROVED",
  "createdDate": "2024-11-23T10:30:00"
}
```

**실패 응답 (400 Bad Request) - Bean Validation 실패**

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값 검증 실패",
  "errors": {
    "userName": "사용자명은 2~50자여야 합니다",
    "phoneNumber": "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)"
  }
}
```

**실패 응답 (400 Bad Request) - 빈 요청**

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "변경할 항목이 없습니다"
}
```

**실패 응답 (409 Conflict) - 전화번호 중복**

```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "이미 사용 중인 전화번호입니다"
}
```

---

## 🔄 인증 플로우

```text
1. 클라이언트 요청
   GET /api/mypage/profile
   Authorization: Bearer {accessToken}
   
2. JwtAuthenticationFilter
   ├─ Authorization 헤더 추출
   ├─ JwtTokenProvider.validateToken() - 토큰 유효성 검증
   └─ CustomUserDetailsService.loadUserByUsername() - 사용자 조회
   
3. SecurityContext 설정
   Authentication 객체 저장 (email, role)
   
4. MyPageController
   @AuthenticationPrincipal UserDetails
   → userDetails.getUsername() = email
   
5. ProfileService
   AccountRepository.findByEmail(email)
   
6. 응답 생성
   AccountResponse
```

---

## 🔐 보안 검증

### 1. JWT 토큰 필수
```java
@AuthenticationPrincipal UserDetails userDetails
```
- **토큰 없음**: JwtAuthenticationFilter에서 401 Unauthorized 반환
- **토큰 만료**: JwtTokenProvider.validateToken() 실패 → 401
- **잘못된 토큰**: JWT 파싱 실패 → 401

### 2. 본인 계정만 접근
```java
String email = userDetails.getUsername();  // JWT에서 추출한 이메일
profileService.getProfile(email);          // 해당 이메일의 프로필만 조회
```
- **다른 사용자 프로필 조회 불가**: email 파라미터 없음
- **URL에 userId 없음**: /api/mypage/profile (고정)
- **SecurityContext 기반**: 인증된 사용자 = 조회/수정 대상

### 3. 불변 필드 보호
```java
// UpdateProfileRequest에 포함되지 않음
- email: 계정 식별자
- accountType: USER/ACADEMY
- accountApproved: 승인 상태
```

---

## 🧪 Postman 테스트

### 1. 프로필 조회 (성공)

```
GET http://localhost:8080/api/mypage/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Expected: 200 OK + AccountResponse
```

### 2. 프로필 수정 (성공)

```
PATCH http://localhost:8080/api/mypage/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "userName": "홍길동 (수정)",
  "phoneNumber": "010-9999-8888"
}

Expected: 200 OK + AccountResponse (수정됨)
```

### 3. JWT 토큰 없이 호출 (실패)

```
GET http://localhost:8080/api/mypage/profile

Expected: 401 Unauthorized
```

### 4. Bean Validation 실패

```
PATCH http://localhost:8080/api/mypage/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "userName": "가",
  "phoneNumber": "invalid"
}

Expected: 400 Bad Request
```

---

## 🔗 다음 단계

Controller 구현 후:
1. **MyPageControllerTest** 슬라이스 테스트 작성 ([04_controller_test.md](04_controller_test.md))
2. **FullE2ETest** 통합 테스트 작성 ([05_full_e2e_test.md](05_full_e2e_test.md))
