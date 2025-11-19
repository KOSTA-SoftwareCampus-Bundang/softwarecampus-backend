# Phase 12.5-3: Refresh Token 구현

> **소요 시간:** 1-1.5시간  
> **목표:** Access Token + Refresh Token 2개 토큰 패턴 구현 및 Redis 저장

---

## 개요

### 현재 문제점 (Phase 12)

```java
// 로그인 시
String token = jwtTokenProvider.generateToken(email, role);
return ResponseEntity.ok(token);

// 30분 후 → 토큰 만료 → 재로그인 필요 😰
```

**문제:**
- 30분마다 재로그인 불편
- 긴 유효기간 설정 시 보안 위험 (탈취 시 오래 사용 가능)

### 해결책: Refresh Token 패턴

```java
// 로그인 시
TokenResponse response = TokenResponse.builder()
    .accessToken("eyJhbGc...")      // 15분 유효 (짧음 → 보안)
    .refreshToken("550e8400...")     // 7일 유효 (김 → 편의)
    .expiresIn(900000)               // 15분
    .build();

// 15분 후 Access Token 만료
// → POST /api/auth/refresh로 갱신 (재로그인 없이!)
// → 7일간 로그인 유지
```

---

## 1. TokenResponse DTO 생성

**패키지:** `com.softwarecampus.backend.dto.auth`

```java
package com.softwarecampus.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 * Access Token + Refresh Token 쌍 반환
 * 
 * @since 2025-11-19 (Phase 12.5)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    
    /**
     * Access Token (짧은 수명)
     * - 유효기간: 15분
     * - 용도: API 요청 인증
     * - 위치: HTTP Header (Authorization: Bearer {token})
     */
    private String accessToken;
    
    /**
     * Refresh Token (긴 수명)
     * - 유효기간: 7일
     * - 용도: Access Token 갱신
     * - 저장: Redis (서버 제어 가능)
     */
    private String refreshToken;
    
    /**
     * Access Token 만료 시간 (밀리초)
     * 
     * 예: 900000 = 15분
     * 클라이언트는 이 값으로 갱신 타이밍 결정
     */
    private Long expiresIn;
    
    /**
     * 토큰 타입
     * 
     * 항상 "Bearer" 반환
     * HTTP Authorization 헤더 형식: "Bearer {accessToken}"
     */
    @Builder.Default
    private String tokenType = "Bearer";
}
```

---

## 2. JwtTokenProvider 메서드 추가 (선택사항)

**현재 상태:** `getExpiration()` 메서드는 이미 구현되어 있음 (설정값 반환)

**추가 메서드 (선택사항):**
- 실제 토큰에서 만료 시간 추출하는 메서드
- **권장: 추가하지 않음** (현재 구현으로 충분)
- 이유: `TokenResponse.expiresIn`으로 클라이언트가 계산 가능

```java
// 선택사항: 정교한 만료 시간 계산이 필요한 경우에만 추가
public Date getExpirationDate(String token) {
    Claims claims = parseToken(token);
    return claims.getExpiration();
}

public long getRemainingTime(String token) {
    Date expiration = getExpirationDate(token);
    return expiration.getTime() - System.currentTimeMillis();
}
```

### application.properties 확인

```properties
# JWT 설정 - 현재 설정 확인
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:180000}  # 기본값: 3분
jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION:604800000}  # 7일

# 권장: .env 파일에서 설정
# JWT_EXPIRATION=900000  # 15분
```

---

## 3. TokenService 생성

**패키지:** `com.softwarecampus.backend.service.auth`

```java
package com.softwarecampus.backend.service.auth;

import com.softwarecampus.backend.dto.auth.TokenResponse;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 토큰 관리 서비스
 * - Refresh Token 발급/검증/갱신
 * - Redis에 Refresh Token 저장
 * 
 * @since 2025-11-19 (Phase 12.5)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final long REFRESH_TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L; // 7일
    
    /**
     * Access Token + Refresh Token 생성
     * 
     * 동작:
     * 1. Access Token 생성 (JWT, 15분)
     * 2. Refresh Token 생성 (UUID, 7일)
     * 3. Refresh Token을 Redis에 저장
     * 
     * @param email 사용자 이메일
     * @return TokenResponse (accessToken, refreshToken, expiresIn)
     */
    public TokenResponse createTokens(String email) {
        // 1. Access Token 생성 (기존 JWT)
        String accessToken = jwtTokenProvider.generateToken(
            email,
            jwtTokenProvider.getRoleFromToken(
                jwtTokenProvider.generateToken(email, "USER")
            )
        );
        
        // 2. Refresh Token 생성 (UUID - 추측 불가능)
        String refreshToken = UUID.randomUUID().toString();
        
        // 3. Redis에 Refresh Token 저장
        String key = REFRESH_TOKEN_PREFIX + email;
        redisTemplate.opsForValue().set(
            key,
            refreshToken,
            REFRESH_TOKEN_VALIDITY_MS,
            TimeUnit.MILLISECONDS
        );
        
        log.debug("Refresh Token created for user: {}", email);
        
        // 4. TokenResponse 반환
        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(900000L)  // 15분 (밀리초)
            .build();
    }
    
    /**
     * Refresh Token으로 Access Token 갱신
     * 
     * 동작:
     * 1. Redis에서 저장된 Refresh Token 조회
     * 2. 요청된 Refresh Token과 비교
     * 3. 일치하면 새 Access Token 발급
     * 
     * @param refreshToken 클라이언트가 보낸 Refresh Token
     * @param email 사용자 이메일
     * @return 새로운 Access Token
     * @throws IllegalArgumentException Refresh Token이 유효하지 않은 경우
     */
    public String refreshAccessToken(String refreshToken, String email) {
        // 1. Redis에서 저장된 Refresh Token 조회
        String key = REFRESH_TOKEN_PREFIX + email;
        String storedToken = redisTemplate.opsForValue().get(key);
        
        // 2. Refresh Token 검증
        if (storedToken == null) {
            log.warn("Refresh Token not found for user: {}", email);
            throw new IllegalArgumentException("Refresh Token expired or not found");
        }
        
        if (!storedToken.equals(refreshToken)) {
            log.warn("Invalid Refresh Token for user: {}", email);
            throw new IllegalArgumentException("Invalid Refresh Token");
        }
        
        // 3. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.generateToken(
            email,
            jwtTokenProvider.getRoleFromToken(
                jwtTokenProvider.generateToken(email, "USER")
            )
        );
        
        log.debug("Access Token refreshed for user: {}", email);
        
        return newAccessToken;
    }
    
    /**
     * Refresh Token 무효화 (로그아웃)
     * 
     * 동작:
     * - Redis에서 Refresh Token 삭제
     * - 이후 갱신 불가능
     * 
     * @param email 사용자 이메일
     */
    public void revokeRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Refresh Token revoked for user: {}", email);
        } else {
            log.warn("Refresh Token not found for user: {}", email);
        }
    }
    
    /**
     * Refresh Token 존재 여부 확인
     * 
     * @param email 사용자 이메일
     * @return 존재 여부
     */
    public boolean hasRefreshToken(String email) {
        String key = REFRESH_TOKEN_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

---

## 4. AuthController 수정

**파일:** `src/main/java/com/softwarecampus/backend/controller/user/AuthController.java`

### 로그인 엔드포인트 수정

```java
/**
 * 로그인 (Phase 12.5 - Refresh Token 추가)
 * 
 * Before: String token 반환
 * After: TokenResponse 반환 (accessToken + refreshToken)
 */
@PostMapping("/login")
public ResponseEntity<TokenResponse> login(
        @Valid @RequestBody LoginRequest request) {
    
    // 1. 인증 확인
    authService.authenticate(request.getEmail(), request.getPassword());
    
    // 2. Access Token + Refresh Token 생성
    TokenResponse tokenResponse = tokenService.createTokens(request.getEmail());
    
    return ResponseEntity.ok(tokenResponse);
}
```

### Refresh 엔드포인트 추가 (NEW)

```java
/**
 * Access Token 갱신
 * 
 * Request Body:
 * {
 *   "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
 *   "email": "user@example.com"
 * }
 * 
 * Response:
 * {
 *   "accessToken": "eyJhbGc...",
 *   "expiresIn": 900000
 * }
 */
@PostMapping("/refresh")
public ResponseEntity<Map<String, Object>> refresh(
        @RequestBody Map<String, String> request) {
    
    String refreshToken = request.get("refreshToken");
    String email = request.get("email");
    
    // Refresh Token 검증 및 새 Access Token 발급
    String newAccessToken = tokenService.refreshAccessToken(refreshToken, email);
    
    Map<String, Object> response = new HashMap<>();
    response.put("accessToken", newAccessToken);
    response.put("expiresIn", 900000L);
    
    return ResponseEntity.ok(response);
}
```

### 로그아웃 엔드포인트 수정

```java
/**
 * 로그아웃 (Phase 12.5 - Refresh Token 무효화 추가)
 */
@PostMapping("/logout")
public ResponseEntity<Void> logout(
        @RequestHeader("Authorization") String authorization,
        @RequestParam String email) {
    
    // 1. Access Token 추출
    String token = authorization.substring(7); // "Bearer " 제거
    
    // 2. JWT Blacklist에 추가 (Phase 12.5-4)
    tokenBlacklistService.addToBlacklist(token);
    
    // 3. Refresh Token 무효화
    tokenService.revokeRefreshToken(email);
    
    return ResponseEntity.ok().build();
}
```

---

## 5. Redis 데이터 구조

### 저장 형식

```
Key: refresh:user@example.com
Value: "550e8400-e29b-41d4-a716-446655440000"
TTL: 604800000ms (7일)
```

### Redis CLI 확인

```bash
# Redis 접속
docker exec -it softwarecampus-redis redis-cli

# Refresh Token 확인
127.0.0.1:6379> GET "refresh:user@example.com"
"550e8400-e29b-41d4-a716-446655440000"

# TTL 확인 (초)
127.0.0.1:6379> TTL "refresh:user@example.com"
(integer) 604800  # 7일 = 604800초

# 삭제 (로그아웃 시)
127.0.0.1:6379> DEL "refresh:user@example.com"
(integer) 1
```

---

## 6. API 사용 예시

### 6.1 로그인

**Request:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "expiresIn": 900000,
  "tokenType": "Bearer"
}
```

### 6.2 API 요청 (Access Token 사용)

**Request:**
```http
GET /api/courses
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1...
```

**Response:**
```json
{
  "courses": [...]
}
```

### 6.3 Access Token 갱신 (15분 후)

**Request:**
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1...",
  "expiresIn": 900000
}
```

### 6.4 로그아웃 (Phase 14에서 구현 예정)

**Request:**
```http
POST /api/auth/logout?email=user@example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1...
```

**Response:**
```
200 OK
```

**결과:**
- Refresh Token → Redis에서 삭제 (갱신 불가)

**참고:** JWT Blacklist는 구현하지 않음 (Access Token 15분 짧은 수명으로 충분)

---

## 7. 클라이언트 구현 가이드

### JavaScript 예시

```javascript
// 로그인
async function login(email, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  
  const data = await response.json();
  
  // 토큰 저장
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  localStorage.setItem('email', email);
  
  // 자동 갱신 타이머 설정 (14분 후)
  setTimeout(refreshToken, 14 * 60 * 1000);
}

// Access Token 갱신
async function refreshToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  const email = localStorage.getItem('email');
  
  const response = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken, email })
  });
  
  if (response.ok) {
    const data = await response.json();
    localStorage.setItem('accessToken', data.accessToken);
    
    // 다시 14분 후 갱신
    setTimeout(refreshToken, 14 * 60 * 1000);
  } else {
    // Refresh Token 만료 → 재로그인 필요
    logout();
    window.location.href = '/login';
  }
}

// API 요청
async function apiRequest(url) {
  const accessToken = localStorage.getItem('accessToken');
  
  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });
  
  if (response.status === 401) {
    // Access Token 만료 → 갱신 시도
    await refreshToken();
    return apiRequest(url);  // 재시도
  }
  
  return response.json();
}

// 로그아웃
async function logout() {
  const accessToken = localStorage.getItem('accessToken');
  const email = localStorage.getItem('email');
  
  await fetch(`/api/auth/logout?email=${email}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });
  
  localStorage.clear();
}
```

---

## ✅ 완료 체크리스트

- [ ] TokenResponse.java 생성 확인 (이미 완료)
- [ ] JwtTokenProvider 메서드는 현재대로 유지 (선택사항)
- [ ] application.properties JWT 유효기간 확인
- [ ] TokenService.java 확인 (이미 완료)
- [ ] AuthController 로그인/갱신 API 구현 (Phase 14)
- [ ] mvn clean compile 성공
- [ ] Redis에 Refresh Token 저장 확인
- [ ] API 테스트 (로그인 → 갱신)

---

## 📝 다음 단계

✅ Refresh Token 구현 완료!

**Phase 12.5-4 (JWT Blacklist)는 삭제됨** - 불필요 (Access Token 짧은 수명)

다음: **Phase 12.5-5 - Rate Limiting**
- RateLimitFilter 생성 (IP 기반)
- LoginAttemptService 생성 (로그인 실패)
- DDoS 공격 방어
