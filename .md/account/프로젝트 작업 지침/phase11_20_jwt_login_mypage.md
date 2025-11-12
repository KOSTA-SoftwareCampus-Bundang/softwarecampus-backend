# Phase 11-20: JWT 및 로그인/마이페이지

> Phase 10 완료 후 상세 계획 작성 예정

---

## 📋 Phase 개요

### Phase 11: JWT 구현
- JwtTokenProvider.java
- JwtProperties.java
- application.properties 설정

### Phase 12: JWT 단위 테스트
- JwtTokenProviderTest.java

### Phase 13: UserDetailsService 구현
- CustomUserDetailsService.java

### Phase 14: UserDetailsService 테스트
- CustomUserDetailsServiceTest.java

### Phase 15: Security 고도화
- JwtAuthenticationFilter.java
- SecurityFilterChain 완성

### Phase 16: 로그인 API
- LoginRequest/Response
- AuthService 로그인 메서드
- AuthController 로그인 엔드포인트

### Phase 17: 로그인 테스트
- Service + Controller 테스트

### Phase 18: 마이페이지 API
- MyPageController.java

### Phase 19: 마이페이지 테스트
- @WithMockUser 사용

### Phase 20: 통합 테스트 (전체 플로우)
- 회원가입 → 로그인 → JWT → 마이페이지

---

## ⚠️ 미정 사항

Phase 10 완료 후 다음을 결정해야 합니다:

### JWT 전략
- 라이브러리 선택 (jjwt vs java-jwt vs nimbus-jose-jwt)
- Access Token + Refresh Token 여부
- 토큰 만료시간 (Access: 30분? Refresh: 7일?)
- 저장 위치 (Cookie vs LocalStorage vs httpOnly)

### SecurityFilterChain 정책
- `/api/auth/**` → permitAll (회원가입, 로그인)
- `/api/**` → authenticated (나머지)
- CSRF 설정 (JWT 사용 시 disable)
- CORS 허용 오리진

### 인증 예외 응답
- 401 Unauthorized 포맷 (RFC 9457)
- 403 Forbidden 포맷

---

## 🔜 다음 작업

**Phase 10 완료 후:**
1. 보안 설계 문서 작성 (`security_design.md`)
2. Phase 11-20 상세 계획 수립
3. 순차적 구현 및 테스트

---

**상세 내용:** Phase 10 완료 후 `implementation_plan.md` 또는 별도 문서에 작성 예정
