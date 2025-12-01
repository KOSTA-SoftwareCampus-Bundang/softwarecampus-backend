# 토큰 갱신 API 수정 요청 (긴급)

> **📅 작성일:** 2025-12-02
> **🎯 목적:** `/api/auth/refresh` API의 인증 로직 수정 요청
> **⚠️ 우선순위:** 🔴 긴급 (현재 토큰 갱신 불가)

---

## 1. 현재 문제 상황

### 증상
- 액세스 토큰 만료 후 리프레시 토큰으로 갱신 시도 시 **401 Unauthorized** 발생
- 사용자가 로그인 후 일정 시간(토큰 만료 시간) 경과 후 **자동 로그아웃** 됨
- 게시글 작성/수정 등 인증 필요 API 호출 시 토큰 갱신 실패

### 에러 로그 (프론트엔드)
```
POST http://localhost:5173/api/auth/refresh 401 (Unauthorized)
```

---

## 2. 원인 분석

### 2.1 현재 백엔드 코드 (`AuthController.java` 234행~)

```java
@PostMapping("/refresh")
public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    try {
        // ⚠️ 문제: 여기서 SecurityContext 인증 체크
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 인증되지 않은 경우 또는 익명 사용자인 경우
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            log.warn("Unauthenticated refresh attempt for email: {}", EmailUtils.maskEmail(request.email()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();  // ❌ 여기서 401 반환
        }
        // ...
    }
}
```

### 2.2 문제점

| 항목 | 현재 동작 | 올바른 동작 |
|------|----------|------------|
| 액세스 토큰 만료 | SecurityContext에 인증 정보 없음 | - |
| `/api/auth/refresh` 호출 | `auth == null` → **401 반환** | 리프레시 토큰 검증 후 새 액세스 토큰 발급 |
| 결과 | 토큰 갱신 불가 → 로그아웃 | 정상 갱신 |

### 2.3 SecurityConfig 설정 (문제 없음)

```java
// SecurityConfig.java - /api/auth/** 는 permitAll로 설정되어 있음
.requestMatchers(
    "/api/auth/**",  // ✅ 인증 불필요로 설정됨
    // ...
).permitAll()
```

`SecurityConfig`에서는 `/api/auth/**`를 `permitAll()`로 설정했으나,
**컨트롤러에서 수동으로 인증 체크**를 하고 있어서 문제 발생.

---

## 3. 수정 요청

### 3.1 수정 방안 A (권장)

**인증 체크 제거 + 리프레시 토큰만으로 검증**

```java
@PostMapping("/refresh")
public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    try {
        // ❌ 제거: SecurityContext 인증 체크
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        // }

        // ✅ 리프레시 토큰 자체의 유효성만 검증
        // tokenService.refreshAccessToken() 내부에서 Redis의 리프레시 토큰 검증
        String newAccessToken = tokenService.refreshAccessToken(
                request.email(),
                request.refreshToken());

        log.info("Access Token refreshed for user: {}", EmailUtils.maskEmail(request.email()));

        long expiresInSeconds = jwtTokenProvider.getExpiration() / 1000;

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "expiresIn", expiresInSeconds,
                "tokenType", "Bearer"));

    } catch (IllegalArgumentException e) {
        // 리프레시 토큰 검증 실패 (Redis에 없거나 만료됨)
        log.warn("Invalid refresh token for user: {}", EmailUtils.maskEmail(request.email()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
```

### 3.2 수정 방안 B (보안 강화)

**리프레시 토큰에서 사용자 검증**

```java
@PostMapping("/refresh")
public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    try {
        // 리프레시 토큰에서 이메일 추출하여 요청 이메일과 비교
        String emailFromToken = jwtTokenProvider.getEmailFromRefreshToken(request.refreshToken());
        
        if (!request.email().equals(emailFromToken)) {
            log.warn("Email mismatch in refresh token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String newAccessToken = tokenService.refreshAccessToken(
                request.email(),
                request.refreshToken());

        // ... 나머지 동일
    }
}
```

---

## 4. 토큰 갱신 플로우 (정상 동작)

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   프론트엔드     │     │     백엔드       │     │     Redis       │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │  POST /api/auth/refresh                       │
         │  { refreshToken, email }                      │
         │──────────────────────>│                       │
         │                       │                       │
         │                       │  리프레시 토큰 조회    │
         │                       │──────────────────────>│
         │                       │                       │
         │                       │<──────────────────────│
         │                       │  토큰 존재 + 유효     │
         │                       │                       │
         │  200 OK               │                       │
         │  { accessToken,       │                       │
         │    expiresIn }        │                       │
         │<──────────────────────│                       │
         │                       │                       │
```

---

## 5. 테스트 케이스

수정 후 다음 시나리오 테스트 필요:

| # | 시나리오 | 예상 결과 |
|---|---------|----------|
| 1 | 유효한 리프레시 토큰으로 갱신 | 200 OK + 새 액세스 토큰 |
| 2 | 만료된 리프레시 토큰으로 갱신 | 401 Unauthorized |
| 3 | 잘못된 리프레시 토큰으로 갱신 | 401 Unauthorized |
| 4 | 이메일 불일치 (토큰 탈취 시도) | 401 또는 403 |
| 5 | 액세스 토큰 만료 후 갱신 | 200 OK (핵심!) |

---

## 6. 임시 대응 (프론트엔드)

백엔드 수정 전까지 프론트엔드에서 임시 대응:

```typescript
// client.ts - 만료 2분 전에 미리 갱신 (기존 1분 → 2분)
const isExpiringSoon = expiresAt && (expiresAt - now < 120 * 1000);
```

⚠️ **한계:** 토큰 만료 시간(3분)보다 작업 시간이 길면 여전히 문제 발생

---

## 7. 참고: OAuth 2.0 표준

RFC 6749에 따르면 토큰 갱신 요청은 **리프레시 토큰만으로 인증**해야 합니다:

> The authorization server MUST validate the refresh token.
> (인가 서버는 리프레시 토큰을 검증해야 한다)

현재 구현은 **액세스 토큰 + 리프레시 토큰** 둘 다 요구하고 있어서 표준에 맞지 않습니다.

---

**문의:** 프론트엔드 팀  
**긴급도:** 🔴 높음 (사용자 경험에 직접 영향)
