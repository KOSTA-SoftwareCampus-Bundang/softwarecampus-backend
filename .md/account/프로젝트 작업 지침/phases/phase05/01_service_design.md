# 1. Service Layer 설계 원칙

## 📂 생성/수정 파일

### 새로 생성된 파일:
```text
src/main/java/com/softwarecampus/backend/
├─ service/user/
│  ├─ signup/
│  │  ├─ SignupService.java              ✅ 회원가입 인터페이스
│  │  └─ SignupServiceImpl.java          ✅ 회원가입 구현
│  └─ profile/
│     ├─ ProfileService.java             ✅ 프로필 인터페이스
│     └─ ProfileServiceImpl.java         ✅ 프로필 구현
├─ exception/user/
│  ├─ InvalidInputException.java         ✅ 잘못된 입력 예외
│  ├─ DuplicateEmailException.java       ✅ 이메일 중복 예외
│  └─ AccountNotFoundException.java      ✅ 계정 미존재 예외
└─ util/
   └─ EmailUtils.java                    ✅ 이메일 검증/마스킹 유틸

.md/account/시나리오/
├─ README.md                             ✅ 시나리오 목록
├─ signup_scenarios.md                   ✅ 회원가입 시나리오
└─ profile_scenarios.md                  ✅ 프로필 조회 시나리오
```

### 수정된 파일:
```text
src/main/java/com/softwarecampus/backend/
├─ exception/
│  └─ GlobalExceptionHandler.java        ✅ InvalidInputException 핸들러 추가
└─ dto/user/
   └─ MessageResponse.java               ✅ Status 필드 제거 (RESTful)
```

---

## 🎯 설계 결정 사항

### 1. 기능별 독립 패키지
**결정:** signup/login/profile 별도 패키지로 분리

**이유:**
- 각 기능이 명확히 분리 (회원가입/로그인/프로필)
- Phase별 독립적 작업 가능
- 폴더 구조만 봐도 기능 파악 가능
- 테스트 파일도 같은 구조로 분리 가능

### 2. 인터페이스 + 구현체 쌍
**결정:** 각 Service는 인터페이스와 구현체로 구성

**이유:**
- 테스트 시 Mock 객체 주입 용이
- 명확한 계약(Contract) 정의
- 향후 다른 구현체로 교체 가능 (유연성)
- Spring 권장 패턴

### 3. Facade 패턴 제거
**결정:** Controller가 각 Service를 직접 주입

**이유:**
- 불필요한 중간 계층 제거 (단순화)
- 각 Service가 독립적이므로 Facade 불필요
- Controller 코드가 더 명확해짐
- 파일 개수 감소

### 4. 계정 타입 기본값
**결정:** `accountType = USER`, `accountApproved = APPROVED`

**이유:**
- 일반 사용자는 즉시 승인
- 학원 계정은 별도 API로 처리 (관리자 승인 필요)
- Phase 5에서는 일반 회원가입만 처리
- 실제 Entity 필드명 `accountApproved` 사용

### 5. DTO 변환 위치
**결정:** Service Layer에서 Entity ↔ DTO 변환

**이유:**
- Controller는 HTTP 처리에만 집중
- Repository는 Entity만 다룸
- Service가 비즈니스 로직 + 변환 담당

### 6. 트랜잭션 전략
**결정:** 클래스 레벨 `readOnly=true`, 쓰기 메서드만 `@Transactional`

**이유:**
- 읽기 작업이 대부분 → 기본값 읽기 전용
- 쓰기 작업만 명시적으로 `@Transactional` 선언
- 불필요한 트랜잭션 오버헤드 최소화

### 7. 예외 타입
**결정:** RuntimeException (Unchecked Exception)

**이유:**
- Spring은 RuntimeException만 자동 롤백
- 비즈니스 예외는 필수 처리 불필요
- GlobalExceptionHandler에서 일괄 처리

### 8. 예외 패키지 구조
**결정:** 도메인별 예외 패키지 분리 (`exception/user/`)

**이유:**
- 도메인별 예외 관리 용이
- 확장성 (course, board 등 추가 예정)
- 예외 파일이 많아져도 정리된 구조 유지

---

## 📈 Phase별 확장 계획

- **Phase 5 (현재)**: Signup + Profile (조회만)
- **Phase 16**: `login/LoginService.java` + `login/LoginServiceImpl.java` 추가
- **Phase 18**: ProfileService 확장 (수정/삭제 기능 추가)
