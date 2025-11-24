# Phase 15-1: Controller Layer

**파일:** `MyPageController.java`  
**목적:** 마이페이지 REST API 엔드포인트

---

## 📋 엔드포인트

1. `GET /api/mypage/profile` - 프로필 조회
2. `PATCH /api/mypage/profile` - 프로필 수정
3. `DELETE /api/mypage/account` - 계정 삭제

**공통:**
- 모든 엔드포인트 JWT 인증 필수
- `@AuthenticationPrincipal`로 인증 정보 추출

---

## 📄 MyPageController 구현

```java
package com.softwarecampus.backend.controller.user;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.service.user.profile.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지 API Controller
 * 
 * 엔드포인트:
 * - GET /api/mypage/profile: 프로필 조회
 * - PATCH /api/mypage/profile: 프로필 수정
 * - DELETE /api/mypage/account: 계정 삭제
 * 
 * 인증: 모든 엔드포인트 JWT 토큰 필수
 */
@Slf4j
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final ProfileService profileService;

    /**
     * 프로필 조회
     * 
     * @param userDetails Spring Security 인증 정보
     * @return 200 OK + AccountResponse
     */
    @GetMapping("/profile")
    public ResponseEntity<AccountResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String email = userDetails.getUsername();
        log.info("프로필 조회 요청 - email: {}", email);
        
        AccountResponse response = profileService.getProfile(email);
        return ResponseEntity.ok(response);
    }

    /**
     * 프로필 수정
     * 
     * @param userDetails Spring Security 인증 정보
     * @param request 수정할 프로필 정보
     * @return 200 OK + AccountResponse
     */
    @PatchMapping("/profile")
    public ResponseEntity<AccountResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        String email = userDetails.getUsername();
        log.info("프로필 수정 요청 - email: {}", email);
        
        AccountResponse response = profileService.updateProfile(email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 계정 삭제 (소프트 삭제)
     * 
     * @param userDetails Spring Security 인증 정보
     * @return 204 No Content
     */
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String email = userDetails.getUsername();
        log.info("계정 삭제 요청 - email: {}", email);
        
        profileService.deleteAccount(email);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 📊 API 명세

### 1. GET /api/mypage/profile

**Request:**
```http
GET /api/mypage/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 200 OK:**
```json
{
  "email": "user@example.com",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구",
  "affiliation": "소프트캠퍼스",
  "position": "개발자",
  "accountType": "USER",
  "approvalStatus": "APPROVED"
}
```

**Response 401 Unauthorized:**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "인증이 필요합니다"
}
```

---

### 2. PATCH /api/mypage/profile

**Request:**
```http
PATCH /api/mypage/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "userName": "김철수",
  "phoneNumber": "010-9999-8888",
  "address": "부산시 해운대구"
}
```

**Response 200 OK:**
```json
{
  "email": "user@example.com",
  "userName": "김철수",
  "phoneNumber": "010-9999-8888",
  "address": "부산시 해운대구",
  "affiliation": "소프트캠퍼스",
  "position": "개발자",
  "accountType": "USER",
  "approvalStatus": "APPROVED"
}
```

**Response 400 Bad Request (Validation 실패):**
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "전화번호 형식이 올바르지 않습니다"
}
```

**Response 409 Conflict (전화번호 중복):**
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "이미 사용 중인 전화번호입니다"
}
```

---

### 3. DELETE /api/mypage/account

**Request:**
```http
DELETE /api/mypage/account
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response 204 No Content:**
```
(응답 바디 없음)
```

**Response 401 Unauthorized:**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "인증이 필요합니다"
}
```

---

## 🔐 보안 설정

### SecurityConfig 수정 필요
```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/api/mypage/**").authenticated()  // ✅ 추가
        .anyRequest().authenticated()
    );
```

---

## 📌 체크리스트

- [ ] `controller/user/MyPageController.java` 생성
- [ ] GET /api/mypage/profile 구현
- [ ] PATCH /api/mypage/profile 구현
- [ ] DELETE /api/mypage/account 구현
- [ ] @Valid 검증 적용
- [ ] SecurityConfig 설정 확인
