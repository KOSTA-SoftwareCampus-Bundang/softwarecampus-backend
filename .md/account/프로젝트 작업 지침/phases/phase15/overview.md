# Phase 15: 마이페이지 API + 전체 E2E 통합

**목표:** 마이페이지 API 구현 및 전체 플로우 통합 테스트  
**담당자:** 태윤  
**상태:** 🚧 준비 중

---

## 📋 작업 개요

Phase 14(로그인 API)를 기반으로 마이페이지 API를 구현하고, 회원가입부터 로그인, 프로필 조회/수정까지 전체 플로우를 E2E 통합 테스트합니다.

**API 원칙:**
- **RESTful**: GET /api/mypage/profile (조회), PATCH /api/mypage/profile (수정)
- **인증 필수**: @AuthenticationPrincipal UserDetails (Spring Security)
- **보안**: JWT 토큰 검증, 본인 계정만 수정 가능
- **Bean Validation**: UpdateProfileRequest 입력값 검증

---

## 📂 상세 문서 (모듈별 분할)

1. **[DTO 설계](phase15/01_dto_layer.md)**
   - UpdateProfileRequest.java (Bean Validation)

2. **[Service Layer 확장](phase15/02_service_layer.md)**
   - ProfileService.updateProfile() 메서드 추가
   - ProfileServiceImpl 구현

3. **[Controller Layer](phase15/03_controller_layer.md)**
   - MyPageController.java 생성
   - GET /api/mypage/profile (프로필 조회)
   - PATCH /api/mypage/profile (프로필 수정)

4. **[Controller 슬라이스 테스트](phase15/04_controller_test.md)**
   - MyPageControllerTest.java (10-12개 테스트)
   - @WithMockUser 인증 모킹

5. **[전체 E2E 통합 테스트](phase15/05_full_e2e_test.md)**
   - FullE2ETest.java (15-20개 테스트)
   - 회원가입 → 로그인 → 프로필 조회/수정
   - ACADEMY 계정 승인 플로우
   - JWT 만료 처리

---

## 📂 생성 파일

```text
src/main/java/com/softwarecampus/backend/
├─ dto/user/
│  └─ UpdateProfileRequest.java           ✅ 프로필 수정 요청 DTO
├─ service/user/profile/
│  ├─ ProfileService.java                 🔧 updateProfile() 메서드 추가
│  └─ ProfileServiceImpl.java             🔧 updateProfile() 구현
└─ controller/user/
   └─ MyPageController.java               ✅ 마이페이지 API Controller

src/test/java/com/softwarecampus/backend/
├─ controller/user/
│  └─ MyPageControllerTest.java           ✅ 컨트롤러 슬라이스 테스트 (10-12개)
└─ integration/
   └─ FullE2ETest.java                    ✅ 전체 E2E 통합 테스트 (15-20개)
```

---

## 📊 의존성 관계도

```text
MyPageController
    ↓
ProfileService (인터페이스)
    ↓
ProfileServiceImpl
    ↓
    ├─ AccountRepository.findByEmail(String)
    ├─ AccountRepository.existsByPhoneNumber(String)
    └─ Account.update() (엔티티 메서드)

인증 플로우:
JwtAuthenticationFilter
    ↓
JwtTokenProvider.validateToken()
    ↓
CustomUserDetailsService.loadUserByUsername()
    ↓
SecurityContext.setAuthentication()
    ↓
MyPageController (@AuthenticationPrincipal UserDetails)
```

---

## 🎯 완료 기준

1. ✅ UpdateProfileRequest DTO 생성 (Bean Validation)
2. ✅ ProfileService.updateProfile() 메서드 추가
3. ✅ MyPageController 생성 (GET, PATCH 엔드포인트)
4. ✅ MyPageControllerTest 작성 (10-12개 테스트)
5. ✅ FullE2ETest 작성 (15-20개 테스트)
6. ✅ mvn clean compile 성공
7. ✅ 전체 테스트 통과 (기존 102-107개 + 신규 25-30개 = 127-137개)

---

## ⏱️ 예상 시간

**총 3-4시간**
- DTO + Service 확장: 30분
- Controller 구현: 30분
- Controller 슬라이스 테스트: 1시간
- E2E 통합 테스트: 1.5-2시간

---

## 📝 체크리스트

### 1. DTO Layer
- [ ] `UpdateProfileRequest.java` 생성 (@Size, @Pattern)

### 2. Service Layer 확장
- [ ] `ProfileService.java` 확장
  - [ ] `updateProfile(String email, UpdateProfileRequest)` 메서드 추가
- [ ] `ProfileServiceImpl.java` 구현
  - [ ] Account 조회 (이메일로)
  - [ ] 전화번호 중복 검증 (변경 시)
  - [ ] Account 엔티티 업데이트
  - [ ] AccountResponse 반환

### 3. Controller Layer
- [ ] `MyPageController.java` 생성
  - [ ] GET /api/mypage/profile (프로필 조회)
  - [ ] PATCH /api/mypage/profile (프로필 수정)
  - [ ] @AuthenticationPrincipal UserDetails 인증

### 4. Controller 슬라이스 테스트
- [ ] `MyPageControllerTest.java` 생성 (10-12개 테스트)
  - [ ] @WithMockUser 인증 모킹
  - [ ] 프로필 조회 성공 (200)
  - [ ] 프로필 수정 성공 (200)
  - [ ] Bean Validation 실패 (400)
  - [ ] 인증 없이 호출 (401)
  - [ ] 전화번호 중복 (409)

### 5. E2E 통합 테스트
- [ ] `FullE2ETest.java` 생성 (15-20개 테스트)
  - [ ] 시나리오 1: 회원가입 → 로그인 → 프로필 조회
  - [ ] 시나리오 2: 회원가입 → 로그인 → 프로필 수정 → 재조회
  - [ ] 시나리오 3: 전화번호 수정 (중복 검증)
  - [ ] 시나리오 4: JWT 만료 처리
  - [ ] 시나리오 5: ACADEMY 계정 전체 플로우
  - [ ] 시나리오 6: 토큰 없이 API 호출
  - [ ] 시나리오 7: Refresh Token 갱신 후 프로필 수정

### 6. 빌드 및 검증
- [ ] `mvn clean compile` 성공
- [ ] `mvn test` 전체 통과 (127-137개)

---

## 🔐 보안 고려사항

1. **인증 필수**
   - 모든 마이페이지 API는 JWT 토큰 필요
   - @AuthenticationPrincipal로 인증된 사용자 정보 추출

2. **본인 계정만 수정**
   - SecurityContext의 인증된 이메일과 수정 대상 일치 검증
   - 다른 사용자 프로필 수정 방지

3. **전화번호 중복 검증**
   - 기존 전화번호와 다른 경우에만 중복 검사
   - 본인 전화번호로 변경 시 중복 오류 방지

4. **PII 로깅**
   - 이메일, 전화번호 마스킹
   - 민감 정보 로그 노출 방지

---

## ⏭️ 다음 단계

Phase 15 완료 후:
- **Phase 11-15 전체 완료** 🎉
- **PR 생성**: account-login-mypage 브랜치 → main 병합
- **다음 작업**: 추가 기능 구현 또는 프론트엔드 연동
