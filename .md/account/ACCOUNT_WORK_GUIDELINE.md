# Account 도메인 작업 지침

> 🚨 **중요**: 이 문서는 **Account 도메인(로그인/회원가입/마이페이지/보안) 담당자 전용** 작업 지침입니다.
> 
> **핵심 원칙**: 내가 담당하는 Account 도메인의 **모든 레이어**(엔티티, 레포지토리, 서비스, 컨트롤러, DTO, Security, Test)만 작업하고, 다른 담당 영역(Academy, Course, Board 등)의 **모든 레이어는 절대 건드리지 않습니다**.

---

## ⛔ 절대 금지 사항 - 모든 레이어 공통

### 다른 도메인의 전체 레이어 작업 금지

#### Academy 도메인 (다른 담당자 영역) - 모든 레이어 금지
```
❌ domain/academy/              - Academy 엔티티 (절대 생성/수정 금지)
❌ repository/academy/          - Academy 레포지토리 (절대 생성/수정 금지)
❌ service/academy/             - Academy 서비스 (절대 생성/수정 금지)
❌ controller/academy/          - Academy 컨트롤러 (절대 생성/수정 금지)
❌ dto/academy/                 - Academy DTO (절대 생성/수정 금지)
❌ test/.../academy/            - Academy 테스트 (절대 생성/수정 금지)
```

#### Course 도메인 (다른 담당자 영역) - 모든 레이어 금지
```
❌ domain/course/               - Course 엔티티
❌ repository/course/           - Course 레포지토리
❌ service/course/              - Course 서비스
❌ controller/course/           - Course 컨트롤러
❌ dto/course/                  - Course DTO
❌ test/.../course/             - Course 테스트
```

#### Board 도메인 (다른 담당자 영역) - 모든 레이어 금지
```
❌ domain/board/                - Board 엔티티
❌ repository/board/            - Board 레포지토리
❌ service/board/               - Board 서비스
❌ controller/board/            - Board 컨트롤러
❌ dto/board/                   - Board DTO
❌ test/.../board/              - Board 테스트
```

#### 공통 파일 수정 금지
```
❌ domain/common/BaseTimeEntity.java                    - 공통 베이스 클래스
❌ domain/common/BaseSoftDeleteSupportEntity.java       - 공통 베이스 클래스
❌ domain/common/BoardCategory.java                     - 다른 도메인 Enum
❌ domain/common/CourseCategoryType.java                - 다른 도메인 Enum
```

---

## ✅ 작업 허용 범위 - Account 도메인 전체 레이어

### 담당 영역: Account 관련 모든 파일

#### 1. Domain Layer (엔티티, Enum)
```
✅ domain/common/AccountType.java          - Account 전용 Enum
✅ domain/common/ApprovalStatus.java       - Account 전용 Enum
✅ domain/user/Account.java                - Account 엔티티
```

#### 2. Repository Layer (데이터 접근)
```
✅ repository/user/AccountRepository.java  - Account 레포지토리
```

#### 3. Service Layer (비즈니스 로직)
```
✅ service/user/AccountService.java        - Account 서비스 인터페이스
✅ service/user/impl/AccountServiceImpl.java - Account 서비스 구현
```

#### 4. Controller Layer (API)
```
✅ controller/user/AuthController.java     - 인증/회원 API
✅ controller/user/MyPageController.java   - 마이페이지 API
```

#### 5. DTO Layer (데이터 전송)
```
✅ dto/user/request/SignupRequest.java
✅ dto/user/request/LoginRequest.java
✅ dto/user/request/UpdateMyInfoRequest.java
✅ dto/user/response/LoginResponse.java
✅ dto/user/response/MyPageResponse.java
```

#### 6. Security Layer (인증/인가) - Account 담당자 고유 영역
```
✅ security/SecurityConfig.java
✅ security/JwtTokenProvider.java
✅ security/JwtAuthenticationFilter.java
✅ security/CustomUserDetailsService.java
```

#### 7. Test Layer
```
✅ test/.../repository/user/AccountRepositoryTest.java
✅ test/.../service/user/AccountServiceTest.java
✅ test/.../controller/user/AuthControllerTest.java
```

---

## 🏗️ 개발 원칙

### 1. Entity-First 접근
- ✅ JPA 엔티티 코드를 먼저 작성 → 이후 DDL 자동 생성
- ❌ SQL을 기준으로 엔티티를 맞추는 것이 아님

### 2. 레이어별 독립성
각 레이어는 **Account 도메인 내에서만** 작업

### 3. 다른 도메인 의존성 처리
```java
// ❌ 이렇게 하면 안 됨
@ManyToOne
private Academy academy;  // Academy는 다른 담당자 영역!

// ✅ 이렇게 주석 처리하고 담당자에게 요청
// @ManyToOne
// @JoinColumn(name = "academy_id")
// private Academy academy;  // 추후 Academy 담당자가 구현 예정
```

---

## 📋 작업 순서

### Phase 1: Domain Layer ✅ (완료)
- [x] AccountType.java, ApprovalStatus.java
- [x] Account.java (필드 변환 완료)
- [x] AccountRepository.java (7개 쿼리 메소드)

### Phase 2: DTO Layer (진행 예정)
- [ ] Request DTO 작성 (SignupRequest, LoginRequest, UpdateMyInfoRequest)
- [ ] Response DTO 작성 (LoginResponse, AccountInfoResponse, MessageResponse)

### Phase 3: Service Layer (진행 예정)
- [ ] AccountService.java (인터페이스)
- [ ] AccountServiceImpl.java (구현)

### Phase 4: Controller Layer (진행 예정)
- [ ] AuthController.java (회원가입/로그인 API)
- [ ] MyPageController.java (마이페이지 API)

### Phase 5: Security Layer (진행 예정)
- [ ] JWT, Spring Security 설정

### Phase 6: Test Layer (진행 예정)
- [ ] Repository, Service, Controller 테스트

---

## ⚠️ 작업 전 체크리스트

- [ ] 내가 수정하는 파일이 Account 담당 영역인가?
- [ ] 다른 도메인 파일(academy/, course/, board/)을 건드리지 않았는가?
- [ ] 공통 베이스 클래스를 수정하지 않았는가?
- [ ] 다른 도메인 의존성이 필요하면 주석 처리 후 담당자에게 요청했는가?

---

## 🎯 핵심 원칙 - 5가지 철칙

1. **내 영역에만 집중** - Account 도메인의 모든 레이어만 작업
2. **경계 철저히 지키기** - 다른 도메인의 어떤 레이어도 절대 건드리지 않기
3. **의존성은 주석으로** - 필요하면 주석 처리 후 담당자 요청
4. **문서 활용하기** - 지침 문서를 적극 활용
5. **소통 우선하기** - 불확실하면 먼저 물어보기

---

**작성일**: 2025-10-29  
**담당 영역**: Account 도메인 (로그인/회원가입/마이페이지/보안) - **전체 레이어**  
**작업 상태**: Phase 1 완료, Phase 2 진행 예정
