# Phase 9-20: 테스트 완성 및 보안/인증 시스템

> Phase 8(Controller 테스트) 완료 후 순차 진행

---

## � 주요 변경 사항

**Phase 번호 재조정:**
- 원래 계획: Phase 8 = Controller 테스트 (✅ 완료)
- **Phase 9-10**: 나머지 테스트 완성 (Repository, 통합)
- **Phase 11**: 보안 강화 (Rate Limiting + CORS)
- **Phase 12-20**: JWT 및 인증 시스템

**코드 내 TODO 주석:**
- `AuthController.java` 및 문서의 "TODO Phase 8" 주석은 **Phase 11 보안 작업 시 수정 예정**
- 현재는 원래 계획(Phase 8 = Controller 테스트) 유지

---

## �📋 Phase 개요 (최종 확정안)

### ✅ 완료된 Phase (1-8)
- Phase 1-3: Domain, Exception, Security
- Phase 4: DTO Layer
- Phase 5: Service Layer
- Phase 6: Service 테스트 (51 tests)
- Phase 7: Controller Layer
- Phase 8: Controller 테스트 (12 tests)

### 🔜 진행 예정 Phase (9-20)

#### Phase 9-10: 통합 테스트 (회원가입 E2E + Repository 검증)
**목표:** 전체 Spring Context로 회원가입 플로우 검증 + Repository 동작 확인

**Phase 통합 이유:**
- Repository 단독 테스트는 Spring Data JPA 쿼리 메서드 검증에 불과
- 통합 테스트에서 Repository 실제 동작을 함께 검증하는 것이 효율적
- 중복 작업 제거 및 시간 절약

**생성 파일:**
- SignupIntegrationTest.java
  - @SpringBootTest + @AutoConfigureMockMvc
  - 실제 H2 DB 사용
  - Controller → Service → Repository 전체 플로우

**테스트 시나리오:**
- 회원가입 성공 (USER/ACADEMY)
- 이메일 중복 확인 (existsByEmail 검증)
- DB 저장 및 조회 (save + findByEmail 검증)
- UNIQUE 제약 조건 동작 확인
- /check-email API 동작 확인

**예상 시간:** 2-3시간

#### Phase 11: 보안 강화 ⭐
**목표:** Rate Limiting 및 CORS 설정 추가

**Rate Limiting 구현:**
- `/check-email` 엔드포인트 보호
- IP 기반 제한: 60 req/min
- 429 Too Many Requests 응답
- **구현 방식 선택:**
  1. Bucket4j + Redis (추천)
  2. Spring Cloud Gateway
  3. Custom Interceptor

**CORS 설정:**
- WebConfig.java 생성
- 프론트엔드 Origin 허용 (`http://localhost:3000`)
- allowedMethods, allowedHeaders, allowCredentials 설정

**문서화:**
- 보안 정책 문서
- Rate Limiting 설계 결정 기록
- TODO 주석 정리 (Phase 8 → Phase 11)

**예상 시간:** 3-4시간

#### Phase 12: JWT 구현
- JwtTokenProvider.java
- JwtProperties.java
- application.properties 설정
- 라이브러리 선택 (jjwt 추천)

#### Phase 13: JWT 단위 테스트
- JwtTokenProviderTest.java
- 토큰 생성/검증 테스트

#### Phase 14: UserDetailsService 구현
- CustomUserDetailsService.java
- Account → UserDetails 변환

#### Phase 15: UserDetailsService 테스트
- CustomUserDetailsServiceTest.java

#### Phase 16: Security 고도화
- JwtAuthenticationFilter.java
- SecurityFilterChain 완성
- permitAll vs authenticated 설정

#### Phase 17: 로그인 API
- LoginRequest/Response DTO
- AuthService 로그인 메서드
- AuthController 로그인 엔드포인트

#### Phase 18: 로그인 테스트
- Service + Controller 테스트
- JWT 발급 검증

#### Phase 19: 마이페이지 API
- MyPageController.java
- 프로필 조회/수정 기능

#### Phase 20: 마이페이지 테스트 + 전체 E2E
- @WithMockUser 사용
- 회원가입 → 로그인 → JWT → 마이페이지 전체 플로우

---

## ⚠️ 설계 결정 필요 사항

### Phase 11: 보안 강화 (즉시 결정 필요)
**Rate Limiting 구현 방식 선택:**
1. **Bucket4j + Redis** (추천)
   - 장점: 분산 환경 지원, 성능 우수
   - 단점: Redis 의존성 추가
2. **Spring Cloud Gateway**
   - 장점: 선언적 설정, 게이트웨이 레벨 제어
   - 단점: 별도 Gateway 서버 필요
3. **Custom Interceptor**
   - 장점: 의존성 없음, 단순
   - 단점: 인메모리 제한, 분산 환경 불가

**CORS 정책:**
- 프론트엔드 도메인: `http://localhost:3000` (개발), `https://prod-domain.com` (운영)
- allowedMethods: GET, POST, PUT, DELETE, PATCH
- allowCredentials: true

---

### Phase 12-20: JWT 및 인증 (Phase 11 완료 후 결정)

**JWT 전략:**
- 라이브러리 선택 (jjwt vs java-jwt vs nimbus-jose-jwt)
- Access Token + Refresh Token 여부
- 토큰 만료시간 (Access: 30분? Refresh: 7일?)
- 저장 위치 (Cookie vs LocalStorage vs httpOnly)

**SecurityFilterChain 정책:**
- `/api/auth/**` → permitAll (회원가입, 로그인)
- `/api/**` → authenticated (나머지)
- CSRF 설정 (JWT 사용 시 disable)
- CORS 허용 오리진

**인증 예외 응답:**
- 401 Unauthorized 포맷 (RFC 9457)
- 403 Forbidden 포맷

---

## 🎯 우선순위 및 작업 전략

### 즉시 진행 (Phase 9-11)
**목표:** 회원가입 기능 완전히 마무리 + 보안 강화

1. **Phase 9** (Repository 테스트) - 1-2시간
   - 데이터 계층 검증 완료
   
2. **Phase 10** (통합 테스트) - 1-2시간
   - 회원가입 E2E 검증 완료
   
3. **Phase 11** (보안 강화) - 3-4시간
   - Rate Limiting 구현 방식 결정
   - CORS 설정
   - 코드 내 TODO 주석 정리

### 중기 진행 (Phase 12-16)
**목표:** JWT 인증 시스템 구축

- JWT 라이브러리 선택 및 구현
- Security Filter 체계 완성
- 단위 테스트 작성

### 장기 진행 (Phase 17-20)
**목표:** 로그인 및 마이페이지 기능 완성

- 로그인 API 구현
- 인증 필요 엔드포인트 추가
- 전체 플로우 통합 테스트

---

## 📋 작업 진행 체크리스트

### 완료 (Phase 1-8)
- [x] Phase 1: Domain & Repository
- [x] Phase 2: GlobalExceptionHandler 기본 틀
- [x] Phase 3: 기본 보안 설정 (PasswordEncoder)
- [x] Phase 4: DTO Layer (Bean Validation)
- [x] Phase 5: Service Layer + 도메인 예외
- [x] Phase 6: Service 단위 테스트 (51 tests)
- [x] Phase 7: Controller Layer (회원가입 API)
- [x] Phase 8: Controller 슬라이스 테스트 (12 tests)

### 진행 예정 (Phase 9-11)
- [ ] **Phase 9-10: 통합 테스트 (통합)** ← 다음 작업
  - [ ] SignupIntegrationTest.java 작성
  - [ ] 회원가입 E2E 검증
  - [ ] Repository 동작 검증 포함
- [ ] Phase 11: 보안 강화 (Rate Limiting + CORS)
  - [ ] Rate Limiting 구현 방식 결정
  - [ ] 구현 및 테스트
  - [ ] CORS 설정
  - [ ] TODO 주석 정리 (AuthController.java 등)
  - [ ] 보안 정책 문서 작성

### 대기 중 (Phase 12-20)
- [ ] Phase 12-20: JWT 및 로그인/마이페이지
  - Phase 11 완료 후 상세 계획 수립
