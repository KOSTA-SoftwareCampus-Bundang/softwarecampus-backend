# Phase 11-15: 보안/인증 시스템 (통합 버전)

> **병합 전략:** 관련 Phase 통합으로 빠른 진행 (10개 → 5개 Phase, 50% 시간 단축)

---

## 📌 통합 개요

| Phase | 통합 내용 | 주요 작업 | 시간 | 테스트 |
|-------|----------|----------|------|--------|
| **11** | 원래 11 | CORS + TODO 정리 | 1-2h | - |
| **12** | 12+14+16 | JWT + UserDetailsService + Filter | 4-5h | - |
| **13** | 13+15 | JWT + Security 테스트 | 2-3h | 15-20개 |
| **14** | 17+18 | 로그인 API + 테스트 | 3-4h | 20-25개 |
| **15** | 19+20 | 마이페이지 + E2E | 3-4h | 25-30개 |

**총계:** 13-18시간 / 60-75개 테스트 추가 → **최종 140-160개 테스트**

---

## ✅ 완료 현황 (Phase 1-10)

- Phase 1-3: Domain, Exception, Security
- Phase 4: DTO Layer (5개 파일)
- Phase 5: Service Layer (8개 파일) 
- Phase 6: Service 테스트 (51 tests)
- Phase 7: Controller Layer (2개 파일)
- Phase 8: Controller 테스트 (12 tests)
- Phase 9-10: 통합 테스트 (10 tests) - **2025-11-18 완료**

**현재 테스트:** 82/82 passing (100%)

#### Phase 11: 보안 기본 설정 ⏱️ 1-2시간
**목표:** CORS 설정 및 기본 보안 정책 수립

**작업 내용:**
- ✅ CORS 설정 (`config/WebConfig.java`)
  - 프론트엔드 Origin 허용 (`http://localhost:3000`)
  - allowedMethods: GET, POST, PUT, DELETE, PATCH
  - allowedHeaders: Authorization, Content-Type
  - allowCredentials: true
  
- ✅ TODO 주석 정리
  - `AuthController.java`의 "TODO Phase 8" → 실제 작업 내용으로 교체
  - 관련 문서 업데이트

- ⏭️ Rate Limiting은 **선택사항**으로 미루기
  - 이유: 개발 환경에서 불필요, 운영 시 추가

**생성 파일:**
- `config/WebConfig.java` (1개)

**예상 시간:** 1-2시간

---

#### Phase 12: JWT + Security 통합 구현 ⏱️ 4-5시간
**통합 내용:** 원래 Phase 12 + 14 + 16 병합
**목표:** JWT 인증 시스템 전체 구현 (토큰 발급부터 Filter까지)

**작업 내용:**
1. **JWT 토큰 Provider 구현**
   - `security/jwt/JwtTokenProvider.java`
     - 토큰 생성 (`generateToken()`)
     - 토큰 검증 (`validateToken()`)
     - Claims 추출 (`getEmailFromToken()`, `getRoleFromToken()`)
   - `security/jwt/JwtProperties.java`
     - secret, expiration, issuer 설정
   - application.properties JWT 설정
     ```properties
     jwt.secret=your-secret-key-here
     jwt.expiration=1800000  # 30분
     jwt.issuer=softwarecampus
     ```

2. **UserDetailsService 구현**
   - `security/CustomUserDetailsService.java`
     - Account → UserDetails 변환
     - loadUserByUsername(email) 구현
     - AccountRepository 의존성 주입

3. **JWT 인증 Filter 구현**
   - `security/JwtAuthenticationFilter.java`
     - Authorization 헤더 파싱 ("Bearer " 제거)
     - JWT 검증 및 SecurityContext 설정
     - OncePerRequestFilter 상속

4. **SecurityFilterChain 완성**
   - `config/SecurityConfig.java` 수정
     - JwtAuthenticationFilter 추가
     - permitAll: `/api/v1/auth/signup`, `/api/v1/auth/login`, `/api/v1/auth/check-email`
     - authenticated: `/api/v1/mypage/**`
     - CSRF disable (JWT 사용)
     - CORS 활성화

**생성 파일:**
- `security/jwt/JwtTokenProvider.java`
- `security/jwt/JwtProperties.java`
- `security/CustomUserDetailsService.java`
- `security/JwtAuthenticationFilter.java`
- `config/SecurityConfig.java` (수정)

**pom.xml 의존성 추가:**
```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
```

**예상 시간:** 4-5시간

---

#### Phase 13: JWT + Security 테스트 통합 ⏱️ 2-3시간
**통합 내용:** 원래 Phase 13 + 15 병합
**목표:** JWT 및 UserDetailsService 단위 테스트

**작업 내용:**
1. **JwtTokenProvider 단위 테스트**
   - `test/.../security/jwt/JwtTokenProviderTest.java`
     - 토큰 생성 검증
     - 토큰 파싱 검증 (email, role 추출)
     - 토큰 만료 검증
     - 유효하지 않은 토큰 검증
     - Secret 변경 시 검증 실패

2. **CustomUserDetailsService 단위 테스트**
   - `test/.../security/CustomUserDetailsServiceTest.java`
     - loadUserByUsername 성공
     - 존재하지 않는 사용자 → UsernameNotFoundException
     - Account → UserDetails 변환 검증
     - 권한(authorities) 매핑 검증

**생성 파일:**
- `test/.../security/jwt/JwtTokenProviderTest.java` (8-10 tests)
- `test/.../security/CustomUserDetailsServiceTest.java` (5-7 tests)

**예상 테스트 수:** 15-20개

**예상 시간:** 2-3시간

---

#### Phase 14: 로그인 API + 테스트 통합 ⏱️ 3-4시간
**통합 내용:** 원래 Phase 17 + 18 병합
**목표:** 로그인 엔드포인트 구현 및 전체 테스트

**작업 내용:**
1. **DTO 생성**
   - `dto/user/LoginRequest.java`
     ```java
     public record LoginRequest(
         @NotBlank @Email String email,
         @NotBlank String password
     ) {}
     ```
   - `dto/user/LoginResponse.java`
     ```java
     public record LoginResponse(
         String accessToken,
         String tokenType,  // "Bearer"
         Long expiresIn,    // 1800 (초)
         AccountResponse account
     ) {}
     ```

2. **Service Layer 확장**
   - `service/user/login/LoginService.java` (인터페이스)
   - `service/user/login/LoginServiceImpl.java` (구현)
     - `login(LoginRequest)` 메서드
     - 이메일 검증 → Account 조회
     - 비밀번호 검증 (PasswordEncoder.matches)
     - JWT 토큰 생성 (JwtTokenProvider)
     - LoginResponse 반환
     - 예외: `InvalidCredentialsException` (이메일/비밀번호 오류)

3. **Controller Layer 확장**
   - `controller/user/AuthController.java` 로그인 메서드 추가
     ```java
     @PostMapping("/login")
     public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request)
     ```
     - 200 OK + LoginResponse

4. **예외 처리**
   - `exception/user/InvalidCredentialsException.java` 생성
   - GlobalExceptionHandler에 401 Unauthorized 매핑

5. **Service 단위 테스트**
   - `test/.../service/user/login/LoginServiceImplTest.java`
     - 로그인 성공
     - 이메일 없음 → InvalidCredentialsException
     - 비밀번호 틀림 → InvalidCredentialsException
     - JWT 토큰 발급 검증

6. **Controller 슬라이스 테스트**
   - `test/.../controller/user/AuthControllerTest.java` 확장
     - 로그인 성공 (200 + accessToken)
     - Bean Validation 실패 (400)
     - 잘못된 자격증명 (401)

7. **통합 테스트**
   - `test/.../integration/LoginIntegrationTest.java`
     - 회원가입 → 로그인 성공
     - 로그인 후 JWT 토큰으로 인증된 API 호출

**생성 파일:**
- `dto/user/LoginRequest.java`
- `dto/user/LoginResponse.java`
- `service/user/login/LoginService.java`
- `service/user/login/LoginServiceImpl.java`
- `exception/user/InvalidCredentialsException.java`
- `controller/user/AuthController.java` (로그인 메서드 추가)
- `test/.../service/user/login/LoginServiceImplTest.java` (8-10 tests)
- `test/.../controller/user/AuthControllerTest.java` (로그인 테스트 추가, 5-7 tests)
- `test/.../integration/LoginIntegrationTest.java` (5-8 tests)

**예상 테스트 수:** 20-25개

**예상 시간:** 3-4시간

---

#### Phase 15: 마이페이지 + 전체 E2E 통합 ⏱️ 3-4시간
**통합 내용:** 원래 Phase 19 + 20 병합
**목표:** 마이페이지 API 구현 및 전체 플로우 E2E 테스트

**작업 내용:**
1. **DTO 생성**
   - `dto/user/UpdateProfileRequest.java`
     ```java
     public record UpdateProfileRequest(
         @Size(min = 2, max = 50) String userName,
         @Pattern(regexp = "^01[0-9]-[0-9]{3,4}-[0-9]{4}$|^01[0-9][0-9]{7,8}$")
         String phoneNumber,
         String address,
         String affiliation,
         String position
     ) {}
     ```

2. **Service Layer 확장**
   - `service/user/profile/ProfileService.java` 확장
     - `updateProfile(String email, UpdateProfileRequest)` 메서드 추가
   - `service/user/profile/ProfileServiceImpl.java` 구현
     - Account 조회 → 필드 업데이트 → 저장
     - 전화번호 중복 검증 (기존과 다른 번호인 경우)

3. **Controller Layer**
   - `controller/user/MyPageController.java` 생성
     ```java
     @RestController
     @RequestMapping("/api/v1/mypage")
     @RequiredArgsConstructor
     public class MyPageController {
         
         @GetMapping("/profile")
         public ResponseEntity<AccountResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails)
         
         @PatchMapping("/profile")
         public ResponseEntity<AccountResponse> updateProfile(
             @AuthenticationPrincipal UserDetails userDetails,
             @Valid @RequestBody UpdateProfileRequest request)
     }
     ```

4. **Controller 테스트 (@WithMockUser)**
   - `test/.../controller/user/MyPageControllerTest.java`
     - @WithMockUser(username = "user@test.com", roles = "USER")
     - 프로필 조회 성공 (200)
     - 프로필 수정 성공 (200)
     - Bean Validation 실패 (400)
     - 인증 없이 호출 → 401

5. **전체 E2E 통합 테스트**
   - `test/.../integration/FullE2ETest.java`
     - **시나리오 1: 회원가입 → 로그인 → 프로필 조회**
       1. POST /signup (USER)
       2. POST /login → accessToken 획득
       3. GET /mypage/profile (Authorization: Bearer {token})
       
     - **시나리오 2: 회원가입 → 로그인 → 프로필 수정 → 재조회**
       1. POST /signup
       2. POST /login
       3. PATCH /mypage/profile (userName 변경)
       4. GET /mypage/profile → 변경 확인
       
     - **시나리오 3: JWT 만료 처리**
       1. 만료된 토큰으로 API 호출 → 401
       
     - **시나리오 4: ACADEMY 계정 전체 플로우**
       1. POST /signup (ACADEMY, academyId 포함)
       2. POST /login → PENDING 상태 확인
       3. 관리자가 승인 (수동 DB 업데이트)
       4. 재로그인 → APPROVED 상태 확인

**생성 파일:**
- `dto/user/UpdateProfileRequest.java`
- `service/user/profile/ProfileService.java` (메서드 추가)
- `service/user/profile/ProfileServiceImpl.java` (메서드 구현)
- `controller/user/MyPageController.java`
- `test/.../controller/user/MyPageControllerTest.java` (10-12 tests)
- `test/.../integration/FullE2ETest.java` (15-20 tests)

**예상 테스트 수:** 25-30개

**예상 시간:** 3-4시간

---

## 📊 통합 Phase 요약

| Phase | 통합 내용 | 주요 작업 | 예상 시간 | 예상 테스트 수 |
|-------|----------|----------|----------|--------------|
| **Phase 11** | 원래 Phase 11 | CORS + TODO 정리 | 1-2시간 | - |
| **Phase 12** | 12+14+16 통합 | JWT + UserDetailsService + Filter | 4-5시간 | - |
| **Phase 13** | 13+15 통합 | JWT + Security 테스트 | 2-3시간 | 15-20개 |
| **Phase 14** | 17+18 통합 | 로그인 API + 테스트 | 3-4시간 | 20-25개 |
| **Phase 15** | 19+20 통합 | 마이페이지 + 전체 E2E | 3-4시간 | 25-30개 |

**총 예상:**
- 시간: 13-18시간 (원래 30시간+ → **약 50% 단축**)
- 테스트: 60-75개 추가 (기존 82개 + 신규 = **140-160개**)

---

## 🎯 통합 전략 및 장점

### 병합 원칙
1. **구현 + 테스트 분리 유지**
   - 구현 Phase와 테스트 Phase는 별도로 진행
   - 이유: 한 번에 너무 많으면 디버깅 어려움

2. **관련 기능 묶기**
   - JWT 관련 (Provider + UserDetails + Filter) → Phase 12
   - 로그인 관련 (API + 테스트) → Phase 14
   - 마이페이지 관련 (API + E2E) → Phase 15

3. **선택사항 제외**
   - Rate Limiting: 개발 단계에서 불필요
   - 운영 환경에서 필요시 추가 구현

### 통합의 장점
✅ **시간 절약**: 50% 단축 (30시간 → 15시간)
✅ **맥락 유지**: 관련 작업을 연속으로 진행하여 컨텍스트 스위칭 최소화
✅ **중복 제거**: 반복적인 설정/초기화 작업 한 번만 수행
✅ **전체 흐름 파악**: 큰 단위로 작업하여 시스템 이해도 향상

### 품질 유지
✅ **테스트 커버리지**: 모든 기능에 대한 단위/통합 테스트 포함
✅ **문서화**: 각 Phase별 상세 설계 문서 작성
✅ **코드 리뷰**: Phase 완료 시점마다 검증
✅ **점진적 구현**: Phase 12 → 13 → 14 순차 진행으로 안정성 확보

---

## 🎯 작업 진행 체크리스트

### ✅ 완료 (Phase 1-10)
- [x] Phase 1: Domain & Repository
- [x] Phase 2: GlobalExceptionHandler 기본 틀
- [x] Phase 3: 기본 보안 설정 (PasswordEncoder)
- [x] Phase 4: DTO Layer (Bean Validation)
- [x] Phase 5: Service Layer + 도메인 예외
- [x] Phase 6: Service 단위 테스트 (51 tests)
- [x] Phase 7: Controller Layer (회원가입 API)
- [x] Phase 8: Controller 슬라이스 테스트 (12 tests)
- [x] Phase 9-10: 통합 테스트 (10 tests) - **2025-11-18 완료**

**현재 테스트:** 82/82 passing (100%)

---

### 🔜 진행 예정 (Phase 11-15) - 통합 버전

#### Phase 11: 보안 기본 설정 (1-2시간)
- [ ] WebConfig.java 생성 (CORS 설정)
  - [ ] localhost:3000 허용
  - [ ] allowedMethods 설정
  - [ ] allowCredentials: true
- [ ] TODO 주석 정리
  - [ ] AuthController.java "TODO Phase 8" 제거
  - [ ] 관련 문서 업데이트
- [ ] ⏭️ Rate Limiting 제외 (선택사항)

#### Phase 12: JWT + Security 통합 (4-5시간)
- [ ] **JWT Provider 구현**
  - [ ] JwtTokenProvider.java (generateToken, validateToken, getClaims)
  - [ ] JwtProperties.java (@ConfigurationProperties)
  - [ ] application.properties JWT 설정
- [ ] **UserDetailsService 구현**
  - [ ] CustomUserDetailsService.java
  - [ ] Account → UserDetails 변환
- [ ] **JWT Filter 구현**
  - [ ] JwtAuthenticationFilter.java (OncePerRequestFilter)
  - [ ] Authorization 헤더 파싱
  - [ ] SecurityContext 설정
- [ ] **SecurityConfig 완성**
  - [ ] permitAll: /signup, /login, /check-email
  - [ ] authenticated: /mypage/**
  - [ ] CSRF disable
  - [ ] CORS 활성화
- [ ] **pom.xml 의존성**
  - [ ] jjwt-api, jjwt-impl, jjwt-jackson

#### Phase 13: JWT + Security 테스트 (2-3시간)
- [ ] **JwtTokenProvider 테스트**
  - [ ] 토큰 생성 검증
  - [ ] 토큰 파싱 검증
  - [ ] 토큰 만료 검증
  - [ ] 유효하지 않은 토큰 검증
- [ ] **CustomUserDetailsService 테스트**
  - [ ] loadUserByUsername 성공
  - [ ] 사용자 없음 → UsernameNotFoundException
  - [ ] Account → UserDetails 변환
  - [ ] 권한 매핑 검증

**예상 테스트:** 15-20개

#### Phase 14: 로그인 API + 테스트 (3-4시간)
- [ ] **DTO 생성**
  - [ ] LoginRequest.java
  - [ ] LoginResponse.java (accessToken, tokenType, expiresIn, account)
- [ ] **Service Layer**
  - [ ] LoginService.java (인터페이스)
  - [ ] LoginServiceImpl.java (login 메서드)
  - [ ] InvalidCredentialsException.java
- [ ] **Controller Layer**
  - [ ] AuthController.java 로그인 메서드 추가
- [ ] **GlobalExceptionHandler**
  - [ ] InvalidCredentialsException → 401 매핑
- [ ] **Service 테스트**
  - [ ] 로그인 성공
  - [ ] 이메일 없음
  - [ ] 비밀번호 틀림
  - [ ] JWT 토큰 발급 검증
- [ ] **Controller 테스트**
  - [ ] 로그인 성공 (200)
  - [ ] Bean Validation 실패 (400)
  - [ ] 잘못된 자격증명 (401)
- [ ] **통합 테스트**
  - [ ] 회원가입 → 로그인
  - [ ] JWT로 인증 API 호출

**예상 테스트:** 20-25개

#### Phase 15: 마이페이지 + 전체 E2E (3-4시간)
- [ ] **DTO 생성**
  - [ ] UpdateProfileRequest.java
- [ ] **Service Layer 확장**
  - [ ] ProfileService.updateProfile 메서드
  - [ ] ProfileServiceImpl 구현
- [ ] **Controller Layer**
  - [ ] MyPageController.java 생성
  - [ ] GET /mypage/profile
  - [ ] PATCH /mypage/profile
- [ ] **Controller 테스트**
  - [ ] @WithMockUser 사용
  - [ ] 프로필 조회 성공
  - [ ] 프로필 수정 성공
  - [ ] 인증 없이 호출 → 401
- [ ] **전체 E2E 테스트**
  - [ ] 회원가입 → 로그인 → 프로필 조회
  - [ ] 회원가입 → 로그인 → 프로필 수정 → 재조회
  - [ ] JWT 만료 처리
  - [ ] ACADEMY 계정 전체 플로우

**예상 테스트:** 25-30개

---

## 📊 최종 목표

**완료 시 달성:**
- ✅ 총 Phase: 15개 (1-15)
- ✅ 총 테스트: 140-160개
- ✅ 기능 완성도: 회원가입 + 로그인 + 마이페이지 (100%)
- ✅ 보안: JWT 인증, CORS 설정
- ✅ 테스트 커버리지: 단위/통합/E2E 전체

**시간 절약:**
- 원래 계획: 30시간+
- 통합 버전: 13-18시간
- **절감률: 약 50%**
