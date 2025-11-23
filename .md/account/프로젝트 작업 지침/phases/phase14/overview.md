# Phase 14: 로그인 API + 테스트

**목표:** 로그인 엔드포인트 구현 및 전체 테스트  
**담당자:** 태윤  
**상태:** 🚧 준비 중

---

## 📋 작업 개요

Phase 12(JWT + Security)를 기반으로 로그인 API를 구현합니다. 이메일/비밀번호 검증 후 JWT Access Token과 Refresh Token을 발급하며, 전체 인증 플로우를 통합 테스트합니다.

**API 원칙:**
- **RESTful**: POST /api/auth/login (200 OK)
- **JWT**: Access Token(15분) + Refresh Token(7일) 발급
- **보안**: 비밀번호 검증, 계정 상태 확인, PII 로깅 방지
- **예외 처리**: InvalidCredentialsException (401 Unauthorized)

---

## 📂 상세 문서 (모듈별 분할)

1. **[DTO 설계](phase14/01_dto_layer.md)**
   - LoginRequest.java (Bean Validation)
   - LoginResponse.java (JWT 토큰 응답)

2. **[Service Layer](phase14/02_service_layer.md)**
   - LoginService 인터페이스
   - LoginServiceImpl 구현
   - 인증 로직 (이메일 검증, 비밀번호 검증, JWT 발급)

3. **[Controller Layer](phase14/03_controller_layer.md)**
   - AuthController.login() 메서드 추가
   - API 명세 (POST /api/auth/login)

4. **[Exception Handling](phase14/04_exception_handling.md)**
   - InvalidCredentialsException 생성
   - GlobalExceptionHandler 확장 (401 매핑)

5. **[Service 단위 테스트](phase14/05_service_test.md)**
   - LoginServiceImplTest.java (8-10개 테스트)
   - Mock: AccountRepository, PasswordEncoder, JwtTokenProvider, TokenService

6. **[Controller 슬라이스 테스트](phase14/06_controller_test.md)**
   - AuthControllerTest.login() 테스트 추가 (5-7개 테스트)
   - @WebMvcTest + MockMvc

7. **[통합 테스트](phase14/07_integration_test.md)**
   - LoginIntegrationTest.java (5-8개 테스트)
   - 회원가입 → 로그인 → JWT 인증 API 호출 E2E

---

## 📂 생성 파일

```text
src/main/java/com/softwarecampus/backend/
├─ dto/user/
│  ├─ LoginRequest.java                   ✅ 로그인 요청 DTO
│  └─ LoginResponse.java                  ✅ 로그인 응답 DTO
├─ service/user/login/
│  ├─ LoginService.java                   ✅ 로그인 서비스 인터페이스
│  └─ LoginServiceImpl.java               ✅ 로그인 서비스 구현
├─ exception/user/
│  └─ InvalidCredentialsException.java    ✅ 인증 실패 예외
└─ controller/user/
   └─ AuthController.java                 🔧 login() 메서드 추가

src/test/java/com/softwarecampus/backend/
├─ service/user/login/
│  └─ LoginServiceImplTest.java           ✅ 서비스 단위 테스트 (8-10개)
├─ controller/user/
│  └─ AuthControllerTest.java             🔧 login() 테스트 추가 (5-7개)
└─ integration/
   └─ LoginIntegrationTest.java           ✅ 통합 테스트 (5-8개)
```

---

## 📊 의존성 관계도

```text
AuthController
    ↓
LoginService (인터페이스)
    ↓
LoginServiceImpl
    ↓
    ├─ AccountRepository.findByEmail(String)
    ├─ PasswordEncoder.matches(rawPassword, encodedPassword)
    ├─ JwtTokenProvider.generateToken(email, role)
    └─ TokenService.saveRefreshToken(email, refreshToken)

예외 처리 플로우:
LoginServiceImpl (이메일 없음 or 비밀번호 틀림)
    ↓
InvalidCredentialsException
    ↓
GlobalExceptionHandler
    ↓
RFC 9457 ProblemDetail (401 Unauthorized)
```

---

## 🎯 완료 기준

1. ✅ DTO 2개 생성 (LoginRequest, LoginResponse)
2. ✅ LoginService + LoginServiceImpl 구현
3. ✅ AuthController.login() 메서드 추가
4. ✅ InvalidCredentialsException + GlobalExceptionHandler 확장
5. ✅ Service 단위 테스트 8-10개 작성 및 통과
6. ✅ Controller 슬라이스 테스트 5-7개 작성 및 통과
7. ✅ 통합 테스트 5-8개 작성 및 통과
8. ✅ mvn clean compile 성공
9. ✅ 전체 테스트 통과 (기존 82개 + 신규 20-25개 = 102-107개)

---

## ⏱️ 예상 시간

**총 3-4시간**
- DTO + Service 구현: 1시간
- Controller + Exception: 30분
- Service 단위 테스트: 1시간
- Controller 슬라이스 테스트: 30분
- 통합 테스트: 1시간

---

## 📝 체크리스트

### 1. DTO Layer
- [ ] `LoginRequest.java` 생성 (@NotBlank, @Email)
- [ ] `LoginResponse.java` 생성 (accessToken, refreshToken, expiresIn, account)

### 2. Service Layer
- [ ] `LoginService.java` 인터페이스 생성
- [ ] `LoginServiceImpl.java` 구현
  - [ ] `login(LoginRequest)` 메서드
  - [ ] 이메일로 Account 조회
  - [ ] 비밀번호 검증 (PasswordEncoder.matches)
  - [ ] JWT Access Token 생성
  - [ ] Refresh Token 생성 및 저장
  - [ ] LoginResponse 반환

### 3. Controller Layer
- [ ] `AuthController.java` 확장
  - [ ] `login()` 메서드 추가 (POST /api/auth/login)
  - [ ] LoginService 의존성 주입
  - [ ] 200 OK + LoginResponse 반환

### 4. Exception Handling
- [ ] `InvalidCredentialsException.java` 생성
- [ ] `GlobalExceptionHandler` 확장 (401 Unauthorized 매핑)

### 5. Service 단위 테스트
- [ ] `LoginServiceImplTest.java` 생성 (8-10개 테스트)
  - [ ] 로그인 성공
  - [ ] 이메일 없음 → InvalidCredentialsException
  - [ ] 비밀번호 틀림 → InvalidCredentialsException
  - [ ] JWT 토큰 발급 검증
  - [ ] Refresh Token 저장 검증
  - [ ] 계정 상태 검증 (INACTIVE, UNAPPROVED)

### 6. Controller 슬라이스 테스트
- [ ] `AuthControllerTest.java` 확장 (5-7개 테스트)
  - [ ] 로그인 성공 (200 + accessToken)
  - [ ] Bean Validation 실패 (400)
  - [ ] 잘못된 자격증명 (401)
  - [ ] 이메일 형식 오류 (400)

### 7. 통합 테스트
- [ ] `LoginIntegrationTest.java` 생성 (5-8개 테스트)
  - [ ] 회원가입 → 로그인 성공
  - [ ] 로그인 후 JWT 토큰으로 인증된 API 호출
  - [ ] 잘못된 비밀번호로 로그인 실패
  - [ ] 존재하지 않는 이메일로 로그인 실패
  - [ ] Access Token으로 보호된 엔드포인트 접근

### 8. 빌드 및 검증
- [ ] `mvn clean compile` 성공
- [ ] `mvn test` 전체 통과 (102-107개)

---

## ⏭️ 다음 단계

Phase 14 완료 후:
- **Phase 15**: 마이페이지 API + 전체 E2E 통합 테스트
