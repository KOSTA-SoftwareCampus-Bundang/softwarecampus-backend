# Phase 12: JWT + Security 통합 구현

> **통합 범위:** 원래 Phase 12 + 14 + 16 병합  
> **예상 시간:** 4-5시간  
> **테스트 작성:** Phase 13에서 진행

---

## 📋 체크리스트

### 1. JWT 토큰 Provider 구현
- [ ] `security/jwt/JwtTokenProvider.java` 생성
  - [ ] `generateToken(String email, String role)` 메서드
  - [ ] `validateToken(String token)` 메서드
  - [ ] `getEmailFromToken(String token)` 메서드
  - [ ] `getRoleFromToken(String token)` 메서드
- [ ] `security/jwt/JwtProperties.java` 생성
  - [ ] `@ConfigurationProperties("jwt")` 설정
  - [ ] secret, expiration, issuer 필드

### 2. UserDetailsService 구현
- [ ] `security/CustomUserDetailsService.java` 생성
  - [ ] `loadUserByUsername(String email)` 메서드
  - [ ] Account → UserDetails 변환 로직
  - [ ] AccountRepository 의존성 주입

### 3. JWT 인증 Filter 구현
- [ ] `security/JwtAuthenticationFilter.java` 생성
  - [ ] `OncePerRequestFilter` 상속
  - [ ] Authorization 헤더 파싱 ("Bearer " 제거)
  - [ ] JWT 검증 및 SecurityContext 설정
  - [ ] 예외 처리 (유효하지 않은 토큰)

### 4. SecurityFilterChain 완성
- [ ] `config/SecurityConfig.java` 수정
  - [ ] JwtAuthenticationFilter 등록 (UsernamePasswordAuthenticationFilter 앞)
  - [ ] CSRF disable
  - [ ] CORS 활성화
  - [ ] permitAll: `/api/v1/auth/**`
  - [ ] authenticated: `/api/v1/mypage/**`

### 5. 환경 설정
- [ ] `pom.xml` JWT 의존성 추가 (jjwt 0.13.0)
- [ ] `application.properties` JWT 설정 추가
- [ ] `.env` 파일 JWT_SECRET 추가

### 6. 빌드 및 검증
- [ ] `mvn clean compile` 성공
- [ ] 기존 테스트 90개 유지 (`mvn test`)

---

## 🎯 완료 기준

1. ✅ 모든 Java 파일 컴파일 성공
2. ✅ 기존 테스트 90/90 통과
3. ✅ JWT 토큰 생성/검증 로직 완성
4. ✅ SecurityFilterChain에 JWT Filter 등록 완료
5. ✅ UserDetailsService 구현 완료

---

## 📁 생성 파일 목록

```
src/main/java/com/softwarecampus/backend/
├── security/
│   ├── jwt/
│   │   ├── JwtTokenProvider.java       (NEW)
│   │   └── JwtProperties.java          (NEW)
│   ├── CustomUserDetailsService.java   (NEW)
│   └── JwtAuthenticationFilter.java    (NEW)
└── config/
    └── SecurityConfig.java              (MODIFIED)

src/main/resources/
└── application.properties               (MODIFIED)

pom.xml                                  (MODIFIED)
.env                                     (MODIFIED)
```

---

## ⏭️ 다음 단계

Phase 12 완료 후:
- **Phase 13**: JWT + Security 테스트 작성 (15-20개 테스트)
- **Phase 14**: 로그인 API 구현 및 테스트
