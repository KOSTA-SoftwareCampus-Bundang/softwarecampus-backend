# Phase 5: Service Layer + 도메인 예외

**목표:** 회원가입 비즈니스 로직을 처리하는 Service Layer 구현  
**담당자:** 태윤  
**상태:** ✅ 완료 (2025-11-05)

---

## 📋 작업 개요

회원가입의 핵심 비즈니스 로직을 처리하는 Service Layer를 구현합니다. 이메일 형식 검증, 비밀번호 암호화, 엔티티 저장 등의 작업을 수행하며, 발생 가능한 도메인 예외를 함께 정의합니다.

**설계 원칙:**
- 기능별 독립 패키지 (signup/login/profile)
- 각 기능은 인터페이스 + 구현체 쌍으로 구성
- DTO ↔ Entity 변환은 Service Layer에서 처리
- 비즈니스 예외는 도메인 예외로 명확히 표현
- `@Transactional` 적용으로 트랜잭션 보장
- **보안**: PII(개인정보) 로깅 제거, 동시성 안전 처리

---

## 📂 문서 구조

Phase 5는 주제별로 분리된 문서로 구성됩니다:

1. **[설계 원칙 및 구조](./01_service_design.md)**
   - 패키지 구조 및 설계 원칙
   - Phase별 확장 계획
   - 생성/수정 파일 목록

2. **[SignupService 구현](./02_signup_service.md)**
   - SignupService 인터페이스
   - SignupServiceImpl 구현체
   - 설계 포인트

3. **[ProfileService 구현](./03_profile_service.md)**
   - ProfileService 인터페이스
   - ProfileServiceImpl 구현체
   - Phase 18 확장 계획

4. **[도메인 예외](./04_exceptions.md)**
   - InvalidInputException
   - DuplicateEmailException
   - AccountNotFoundException

5. **[EmailUtils 유틸리티](./05_email_utils.md)**
   - RFC 표준 준수 이메일 검증
   - PII 보호를 위한 마스킹

6. **[GlobalExceptionHandler 수정](./06_exception_handler.md)**
   - 도메인 예외 핸들러 추가
   - RFC 9457 ProblemDetail 적용

7. **[MessageResponse 수정](./07_dto_changes.md)**
   - RESTful 표준 준수
   - Status 필드 제거

8. **[검증 및 완료 기준](./08_verification.md)**
   - 컴파일 확인
   - Phase 완료 체크리스트
   - 다음 단계

---

## 🎯 빠른 참조

### 의존성 관계도
```text
Controller (Phase 7)
    ↓
SignupService (인터페이스)
    ↓
SignupServiceImpl (구현체)
    ↓
    ├─ AccountRepository
    ├─ PasswordEncoder
    └─ throw DuplicateEmailException

Controller (Phase 7)
    ↓
ProfileService (인터페이스)
    ↓
ProfileServiceImpl (구현체)
    ↓
    ├─ AccountRepository
    └─ throw AccountNotFoundException
```

### 테스트 시나리오
- [회원가입 시나리오](../../시나리오/signup_scenarios.md) (20개)
- [프로필 조회 시나리오](../../시나리오/profile_scenarios.md) (8개)

---

**작성일:** 2025-11-05  
**최종 수정:** 2025-11-12  
**상태:** ✅ 구현 완료
