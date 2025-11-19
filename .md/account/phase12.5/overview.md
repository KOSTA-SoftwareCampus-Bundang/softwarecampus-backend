# Phase 12.5: Redis + 보안 강화

> **목적:** JWT 성능 최적화 및 보안 강화  
> **예상 시간:** 4-5시간 (점진적 구현)  
> **테스트 작성:** Phase 13에서 통합

---

## 📋 개요

Phase 12에서 구현한 기본 JWT 인증에 Redis 캐싱과 고급 보안 기능을 추가합니다.

### 문제점 (Phase 12)
1. **성능 문제**
   - 매 요청마다 DB 조회 (UserDetailsService)
   - 100 req/s → DB 100 queries/s

2. **보안 문제**
   - 로그아웃해도 JWT는 유효 (무상태의 딜레마)
   - Access Token 유효기간 30분 → 탈취 시 위험

3. **사용자 경험**
   - 30분마다 재로그인 필요

### 해결책 (Phase 12.5)
1. **Redis 캐싱**
   - UserDetails 캐싱 → DB 조회 제거
   - 100 queries/s → 5-10 queries/s (95% 감소)

2. **Refresh Token 패턴**
   - Access Token: 15분 (짧음, 보안)
   - Refresh Token: 7일 (김, 편의성)
   - Redis에 Refresh Token 저장 → 서버 제어 가능

3. **JWT Blacklist**
   - 로그아웃 시 Redis에 토큰 등록
   - 블랙리스트 체크 → 무효화된 토큰 차단

4. **Rate Limiting**
   - IP당 분당 100회 제한
   - 로그인 실패 5회 → 5분 차단
   - DDoS 공격 방어

---

## 🎯 체크리스트

### Phase 12.5-1: Redis 기본 설정 (30분)
- [ ] Docker Compose로 로컬 Redis 구성
- [ ] `pom.xml` Redis 의존성 추가
  - [ ] spring-boot-starter-data-redis
  - [ ] spring-boot-starter-cache
  - [ ] commons-pool2
- [ ] `application.properties` Redis 설정
- [ ] `config/RedisConfig.java` 생성
  - [ ] RedisTemplate 빈 설정
  - [ ] JSON 직렬화 설정
- [ ] Redis 연결 테스트

### Phase 12.5-2: UserDetails 캐싱 (1시간)
- [ ] `config/CacheConfig.java` 생성
  - [ ] @EnableCaching 활성화
  - [ ] RedisCacheManager 설정
  - [ ] TTL 설정 (10분)
- [ ] `CustomUserDetailsService` 수정
  - [ ] @Cacheable(value = "userDetails", key = "#username")
  - [ ] @CacheEvict 추가 (사용자 정보 변경 시)
- [ ] 캐시 동작 검증

### Phase 12.5-3: Refresh Token 구현 (1-1.5시간)
- [ ] `dto/auth/TokenResponse.java` 생성
  - [ ] accessToken, refreshToken, expiresIn 필드
- [ ] `service/auth/TokenService.java` 생성
  - [ ] createTokens(String email) - 토큰 쌍 생성
    - Access Token 생성 (15분 유효)
    - Refresh Token 생성 (UUID)
    - **Refresh Token을 Redis에 저장** (Key: refresh:{email}, TTL: 7일)
  - [ ] refreshAccessToken(String refreshToken, String email)
    - **Redis에서 Refresh Token 검증** (저장된 값과 비교)
    - 검증 성공 시 새 Access Token 발급
  - [ ] revokeRefreshToken(String email) - 로그아웃 시
    - **Redis에서 Refresh Token 삭제**
- [ ] `JwtTokenProvider` 수정
  - [ ] Access Token 유효기간 15분으로 변경
  - [ ] getExpiration(String token) 메서드 추가
- [ ] AuthController 수정
  - [ ] 로그인 시 TokenResponse 반환
  - [ ] POST /api/auth/refresh 엔드포인트 추가

### Phase 12.5-4: JWT Blacklist (30분)
- [ ] `service/auth/TokenBlacklistService.java` 생성
  - [ ] addToBlacklist(String token)
  - [ ] isBlacklisted(String token)
- [ ] `JwtAuthenticationFilter` 수정
  - [ ] 블랙리스트 체크 로직 추가
- [ ] AuthController 로그아웃 수정
  - [ ] 블랙리스트에 토큰 추가

### Phase 12.5-5: Rate Limiting (1시간)
- [ ] `security/RateLimitFilter.java` 생성
  - [ ] OncePerRequestFilter 상속
  - [ ] IP 기반 요청 수 카운트
  - [ ] 초과 시 429 Too Many Requests
- [ ] `security/LoginAttemptService.java` 생성
  - [ ] 로그인 실패 횟수 추적
  - [ ] 5회 실패 시 5분 차단
- [ ] SecurityConfig에 RateLimitFilter 등록
- [ ] application.properties Rate Limit 설정

---

## 📁 생성 파일 목록

```
softwarecampus-backend/
├── docker-compose.yml                   (NEW)
├── pom.xml                              (MODIFIED)
│
├── src/main/java/com/softwarecampus/backend/
│   ├── config/
│   │   ├── RedisConfig.java             (NEW)
│   │   └── CacheConfig.java             (NEW)
│   │
│   ├── dto/auth/
│   │   └── TokenResponse.java           (NEW)
│   │
│   ├── service/auth/
│   │   ├── TokenService.java            (NEW)
│   │   ├── TokenBlacklistService.java   (NEW)
│   │   └── LoginAttemptService.java     (NEW)
│   │
│   ├── security/
│   │   ├── CustomUserDetailsService.java (MODIFIED - @Cacheable)
│   │   ├── JwtAuthenticationFilter.java  (MODIFIED - Blacklist)
│   │   └── RateLimitFilter.java          (NEW)
│   │
│   ├── security/jwt/
│   │   └── JwtTokenProvider.java         (MODIFIED - getExpiration)
│   │
│   └── controller/user/
│       └── AuthController.java           (MODIFIED - Refresh, Logout)
│
└── src/main/resources/
    └── application.properties            (MODIFIED)
```

---

## 🗄️ Redis 데이터 구조

### 1. UserDetails 캐싱
```
Key: userDetails::user@example.com
Value: {
  "username": "user@example.com",
  "authorities": ["ROLE_USER"],
  "accountNonExpired": true,
  "accountNonLocked": true,
  "credentialsNonExpired": true,
  "enabled": true
}
TTL: 600000ms (10분)
저장소: Spring Cache (RedisCacheManager)
```

### 2. Refresh Token
```
Key: refresh:user@example.com
Value: "550e8400-e29b-41d4-a716-446655440000"
TTL: 604800000ms (7일)
저장소: RedisTemplate<String, String>
```

**특징:**
- 사용자당 1개만 유지 (로그인 시 덮어쓰기)
- 로그아웃 시 즉시 삭제
- UUID v4 형식 (추측 불가능)

### 3. JWT Blacklist
```
Key: blacklist:eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQG...
Value: "revoked"
TTL: 남은 토큰 유효 시간 (동적 계산)
저장소: RedisTemplate<String, String>
```

**예시:**
```java
// 로그아웃 시점: 14:30
// Access Token 만료 시간: 14:45 (15분 남음)
// → TTL: 900초 (15분)
redisTemplate.opsForValue().set(
    "blacklist:" + token,
    "revoked",
    900,
    TimeUnit.SECONDS
);
```

### 4. Rate Limiting - IP 기반
```
Key: ratelimit:192.168.1.100
Value: 15 (요청 횟수)
TTL: 60000ms (1분)
저장소: RedisTemplate<String, Long>
```

**동작:**
```java
// 요청 1
redis.increment("ratelimit:192.168.1.100") → 1
redis.expire("ratelimit:192.168.1.100", 60) → TTL 설정

// 요청 2-99
redis.increment("ratelimit:192.168.1.100") → 2, 3, ..., 99

// 요청 100
redis.increment("ratelimit:192.168.1.100") → 100 (허용)

// 요청 101
redis.get("ratelimit:192.168.1.100") → 100
→ 100 > 제한(100) → 429 Too Many Requests

// 1분 후
redis.get("ratelimit:192.168.1.100") → null (자동 삭제)
```

### 5. Rate Limiting - 로그인 실패
```
Key: loginattempt:192.168.1.100
Value: 3 (실패 횟수)
TTL: 300000ms (5분)
저장소: RedisTemplate<String, Integer>
```

**동작:**
```java
// 1-4회 실패: 카운트만 증가
redis.increment("loginattempt:192.168.1.100") → 1, 2, 3, 4

// 5회 실패: 차단
redis.increment("loginattempt:192.168.1.100") → 5
redis.expire("loginattempt:192.168.1.100", 300) → 5분 차단

// 로그인 성공: 카운트 리셋
redis.delete("loginattempt:192.168.1.100")
```

---

## 🔧 기술 스택 추가

### 의존성
```xml
<!-- Redis -->
spring-boot-starter-data-redis (3.5.6)
spring-boot-starter-cache (3.5.6)
commons-pool2 (2.12.0)
```

### 인프라
```yaml
Docker Compose:
  - Redis 7.2
  - Redis Commander (GUI, 선택)
```

### 설정
```properties
# Redis
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.lettuce.pool.max-active=10

# Cache
spring.cache.type=redis
spring.cache.redis.time-to-live=600000  # 10분

# Rate Limit
rate.limit.requests-per-minute=100
rate.limit.login-attempts=5
rate.limit.block-duration=300  # 5분
```

---

## 📊 성능 예상 개선

### Before (Phase 12)
```
요청: GET /api/courses
1. JWT 검증 (5ms)
2. DB 조회 - UserDetails (50ms)
3. 비즈니스 로직 (10ms)
---
총: 65ms

100 req/s = 100 DB queries/s
```

### After (Phase 12.5)
```
요청: GET /api/courses
1. JWT 검증 (5ms)
2. Redis 조회 - UserDetails (1ms, 캐시 히트)
3. 비즈니스 로직 (10ms)
---
총: 16ms (4배 빠름!)

100 req/s = 5-10 DB queries/s (캐시 미스만)
```

---

## 🔄 Refresh Token 흐름도

### 로그인 → 7일간 사용 → 로그아웃

```
[사용자]                [서버]                    [Redis]
   │                       │                         │
   │  POST /auth/login    │                         │
   ├──────────────────────>│                         │
   │                       │ 1. 인증 확인            │
   │                       │ 2. Access Token 생성   │
   │                       │    (15분 유효)          │
   │                       │ 3. Refresh Token 생성  │
   │                       │    (UUID, 7일)          │
   │                       ├────────────────────────>│
   │                       │ SET refresh:user@...   │
   │                       │ TTL 7일                │
   │<──────────────────────┤                         │
   │ { accessToken,        │                         │
   │   refreshToken,       │                         │
   │   expiresIn: 900 }    │                         │
   │                       │                         │
   │ ═══ 15분 사용 ═══     │                         │
   │                       │                         │
   │  GET /api/courses     │                         │
   ├──────────────────────>│ JWT 검증 (유효)         │
   │<──────────────────────┤ 200 OK                  │
   │                       │                         │
   │ ═══ 15분 경과 ═══     │                         │
   │                       │                         │
   │  GET /api/academies   │                         │
   ├──────────────────────>│ JWT 검증 (만료!)        │
   │<──────────────────────┤ 401 Unauthorized        │
   │                       │                         │
   │  POST /auth/refresh   │                         │
   │  { refreshToken }     │                         │
   ├──────────────────────>│                         │
   │                       ├────────────────────────>│
   │                       │ GET refresh:user@...   │
   │                       │<────────────────────────┤
   │                       │ "550e8400-..."          │
   │                       │ 비교 → 일치!            │
   │                       │ 새 Access Token 생성   │
   │<──────────────────────┤                         │
   │ { accessToken,        │                         │
   │   expiresIn: 900 }    │                         │
   │                       │                         │
   │ ═══ 다시 15분 사용 ═══ │                         │
   │                       │                         │
   │ ... 7일 동안 반복 ...  │                         │
   │                       │                         │
   │  POST /auth/logout    │                         │
   ├──────────────────────>│                         │
   │                       ├────────────────────────>│
   │                       │ DEL refresh:user@...   │
   │                       ├────────────────────────>│
   │                       │ SET blacklist:token... │
   │                       │ TTL 남은시간            │
   │<──────────────────────┤                         │
   │ 200 OK                │                         │
```

---

## 🔐 보안 개선

### Access Token 유효기간 단축
```
Before: 30분
After: 15분

탈취 시 피해 시간 50% 감소
```

### 로그아웃 실제 동작
```
Before: JWT 무효화 불가 (무상태)
After: Redis Blacklist로 즉시 차단
```

### DDoS 방어
```
Before: 무제한 요청 허용
After: IP당 100 req/min 제한
```

---

## ⏭️ 다음 단계

Phase 12.5 완료 후:
- **Phase 13**: JWT + Security + Redis 테스트 작성 (20-30개)
- **Phase 14**: 로그인 API 리팩토링 (TokenResponse 반환)

---

## 📝 완료 기준

1. ✅ Redis 로컬 구동 (Docker)
2. ✅ UserDetails 캐싱 동작 확인
3. ✅ Refresh Token으로 Access Token 갱신 가능
4. ✅ 로그아웃 시 JWT 무효화 확인
5. ✅ Rate Limiting 동작 확인 (429 응답)
6. ✅ 기존 테스트 90/90 유지
7. ✅ mvn clean compile 성공
