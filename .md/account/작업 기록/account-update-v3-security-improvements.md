# account-update-v3 브랜치 보안 개선 작업 내역

> **브랜치**: account-update-v3  
> **작업일**: 2025-12-01  
> **목적**: 보안 강화, Soft Delete 정책 일관성 유지, API 계약 복원  
> **문서 업데이트**: 2025-12-01 - 모든 관련 .md 파일 최신화 완료

---

## 📋 작업 요약

이 브랜치에서는 인증/계정 관련 보안 취약점을 수정하고, Soft Delete 정책을 일관되게 적용하며, API 계약을 원래 설계대로 복원하는 작업을 진행했습니다.

**총 커밋 수**: 13개  
**변경된 파일**: 15개 (코드) + 10개 (문서)  
**주요 변경**: Rate Limiting, Soft Delete 정책 일관성, 정보 누출 방지, API 계약 복원

---

## 🔒 보안 개선 사항

### 1. Rate Limiting 구현 (Redis 기반)
**커밋**: `075ce17`, `e3370a9`

#### 문제
- 비밀번호 검증 및 로그인 API에 브루트포스 공격 취약점 존재
- DDoS 공격에 대한 방어 메커니즘 부재

#### 해결
- **Redis + Lua Script** 기반 Rate Limiting 구현
- 다층 방어 전략:
  - 전역: 100 req/min per IP (DDoS 방어)
  - 로그인: 5 req/min per IP
  - 비밀번호 검증: 10 req/min per (IP + username)
- 원자적 연산 (INCR + EXPIRE) 보장
- 분산 환경 지원

#### 변경 파일
- `RateLimitFilter.java` - 엔드포인트별 세밀한 제한 추가
- `SecurityConfig.java` - 전역 인증 정책 확인
- `application.yml` - Rate limit 설정 추가

#### 기술적 이점
- Redis Lua Script로 원자적 연산
- 분산 서버 환경에서도 정확한 제한
- Defense in Depth 전략

---

### 2. 정보 누출 방지

#### 2.1 비밀번호 검증 응답 통일
**커밋**: `3e4bd30`

**변경 전**:
```java
// 인증 정보 없음 → 401 Unauthorized
// 비밀번호 불일치 → 400 Bad Request
```

**변경 후**:
```java
// 모든 실패 케이스 → 200 OK + failure()
```

**효과**:
- HTTP 상태 코드로 계정 존재 여부 추론 불가능
- 타이밍 공격 완화
- 일관된 에러 처리

#### 2.2 승인 상태 정보 숨김
**커밋**: `9b2a4bf`

**변경 전**:
```java
switch (account.getAccountApproved()) {
    case PENDING -> "승인 대기 중인 계정입니다";
    case REJECTED -> "승인이 거부된 계정입니다";
    default -> "승인되지 않은 계정입니다";
}
```

**변경 후**:
```java
// 모든 미승인 상태 → "계정 승인이 필요합니다"
```

**효과**:
- 계정 승인 상태 추론 불가능
- 로그에는 상세 정보 유지 (관리자용)

**⚠️ 주의**: UX 저하 가능성 있음 (비즈니스 요구사항에 따라 조정 가능)

---

### 3. 코드 품질 개선

#### 3.1 Enum 직접 비교
**커밋**: `36ac731`

**변경 전**:
```java
account.getAccountType().name().equals("ACADEMY")
!account.getAccountApproved().name().equals("APPROVED")
```

**변경 후**:
```java
account.getAccountType() == AccountType.ACADEMY
account.getAccountApproved() != ApprovalStatus.APPROVED
```

**이점**:
- 타입 안정성 향상 (컴파일 타임 체크)
- 성능 개선 (문자열 변환 오버헤드 제거)
- 코드 가독성 향상

---

## 📊 Soft Delete 정책 일관성 유지

### 1. 이메일/전화번호/사용자명 재사용 허용
**커밋**: `e284efe`, `303fb42`

#### 문제
- `existsByEmail()`, `existsByPhoneNumber()` - 삭제된 계정도 포함
- 탈퇴 후 동일 정보로 재가입 불가능

#### 해결
```java
// Before
boolean existsByEmail(String email);
boolean existsByPhoneNumber(String phoneNumber);

// After
boolean existsByEmailAndIsDeletedFalse(String email);
boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);
```

#### 변경 파일
- `AccountRepository.java` - 메서드 시그니처 변경
- `SignupServiceImpl.java` - 이메일 중복 검증 로직
- `ProfileServiceImpl.java` - 전화번호 중복 검증 로직
- `AdminAccountInitializer.java` - 관리자 계정 생성
- `SignupIntegrationTest.java` - 테스트 케이스

#### 효과
- 탈퇴 후 재가입 시 동일 정보 사용 가능
- Soft Delete 정책 완전 일관성 유지
- 사용자 편의성 향상

---

### 2. 테스트 Mock 수정
**커밋**: `49d982e`

#### 문제
```java
// 모순: 삭제된 계정을 반환
inactiveAccount.markDeleted();
when(accountRepository.findByEmailAndIsDeletedFalse(...))
    .thenReturn(Optional.of(inactiveAccount)); // ❌
```

#### 해결
```java
// 삭제된 계정은 조회되지 않음
when(accountRepository.findByEmailAndIsDeletedFalse(...))
    .thenReturn(Optional.empty()); // ✅
```

#### 테스트 시나리오
1. Repository가 삭제된 계정을 찾지 못함
2. Service는 "이메일 또는 비밀번호가 올바르지 않습니다" 예외
3. 정보 누출 방지 (계정 삭제 여부 알 수 없음)

---

## 🔧 API 계약 복원

### 1. 중복 엔드포인트 제거
**커밋**: `d8dd552`

#### 문제
- `PUT /password`와 `PATCH /password` 중복 존재
- PATCH는 `ChangePasswordRequest`를 받지만 서비스 메서드 미구현
- 보안 어노테이션 불일치

#### 해결
- PATCH 엔드포인트 완전 제거
- PUT만 유지 (이중 인증 방식)

#### 최종 API
```
PUT /api/mypage/password
Authorization: Bearer {JWT_TOKEN}
Body: {
  "verificationCode": "123456",
  "newPassword": "NewPass123!"
}
```

---

### 2. changePassword DTO 복원
**커밋**: `5e5c9ef`

#### 문제
- `changePassword`가 `ResetPasswordRequest`를 사용하도록 잘못 변경
- `resetPassword`와 의미적 차이 소실

#### 해결

| 메서드 | 사용 DTO | 인증 타입 | 시나리오 |
|--------|---------|----------|----------|
| `resetPassword` | `ResetPasswordRequest` | PASSWORD_RESET | 비로그인 상태 (비밀번호 찾기) |
| `changePassword` | `ChangePasswordRequest` | PASSWORD_CHANGE | 로그인 상태 (비밀번호 변경) |

#### DTO 필드 차이
```java
// ChangePasswordRequest (로그인 상태)
{
  "verificationCode": "123456",
  "newPassword": "NewPass123!"
}

// ResetPasswordRequest (비로그인 상태)
{
  "code": "123456",
  "newPassword": "NewPass123!"
}
```

#### 변경 파일
- `ProfileService.java` - 인터페이스 시그니처
- `ProfileServiceImpl.java` - `request.getVerificationCode()` 사용
- `MyPageController.java` - `ChangePasswordRequest` 사용

---

## 📈 Repository 계약 정리

### Soft Delete 필터링 메서드 정리

#### 조회 메서드
```java
// 활성 계정만 조회
Optional<Account> findByEmailAndIsDeletedFalse(String email);
Optional<Account> findByUserNameAndIsDeletedFalse(String userName);
List<Account> findByAccountTypeAndIsDeletedFalse(AccountType type);
Page<Account> findByIsDeletedFalse(Pageable pageable);
```

#### 중복 체크 메서드
```java
// 활성 계정만 중복 체크
boolean existsByEmailAndIsDeletedFalse(String email);
boolean existsByUserNameAndIsDeletedFalse(String userName);
boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);
```

#### 검색 메서드
```java
// JPQL로 복잡한 조건 처리
@Query("SELECT a FROM Account a WHERE a.isDeleted = false AND ...")
Page<Account> searchActiveAccounts(@Param("keyword") String keyword, Pageable pageable);
```

---

## 🧪 테스트 개선 사항

### 1. Soft Delete 테스트
- Repository 계약 준수 검증
- Mock 설정의 일관성 확보
- 삭제된 계정 처리 시나리오 명확화

### 2. 보안 테스트
- Rate Limiting 동작 검증 필요
- 정보 누출 방지 검증
- 인증/인가 테스트 강화 필요

---

## 📝 주요 커밋 이력

```
49d982e - fix: Correct soft-deleted account test mock
5e5c9ef - fix: Restore changePassword to use ChangePasswordRequest
d8dd552 - fix: Remove duplicate PATCH /password endpoint
36ac731 - refactor: Use direct enum comparison instead of string comparison
9b2a4bf - fix: Prevent approval status disclosure in login
303fb42 - fix: Allow email reuse after soft delete
e284efe - fix: Allow phone number reuse after soft delete
3e4bd30 - fix: Prevent information disclosure in password verification
075ce17 - refactor: Extend Redis-based RateLimitFilter instead of Bucket4j
```

---

## ⚠️ 주의사항

### 1. UX vs 보안 트레이드오프
- 승인 상태 정보 숨김으로 UX 저하 가능성
- 비즈니스 요구사항에 따라 추후 조정 가능
- 대안: 이메일 알림, 마이페이지 상태 표시

### 2. Rate Limiting 설정
- 현재 설정은 기본값
- 프로덕션 환경에서는 트래픽 패턴에 맞게 조정 필요
- Redis 가용성 확인 필요

### 3. Soft Delete 정책
- 모든 개인정보(이메일, 전화번호, 사용자명) 재사용 가능
- GDPR 등 개인정보 보호 규정 준수
- 필요시 물리적 삭제 정책 추가 고려

---

## 🎯 다음 작업 제안

### 1. 보안 강화
- [ ] Rate Limiting 통합 테스트 작성
- [ ] 비밀번호 정책 강화 (히스토리, 만료 등)
- [ ] 계정 잠금 정책 (연속 로그인 실패)
- [ ] 2FA (Two-Factor Authentication) 도입 검토

### 2. 모니터링
- [ ] Rate Limiting 이벤트 로깅
- [ ] 보안 이벤트 알림 설정
- [ ] Redis 가용성 모니터링

### 3. 문서화
- [x] Soft Delete 전략 문서 전면 개편 (`soft_delete_username_strategy.md`)
- [x] Repository 설계 문서 업데이트 (`entity_repository_design.md`)
- [x] Phase별 구현 가이드 업데이트 (phase07, phase09_10, phase15 등)
- [x] Rate Limiting 문서 최신화 (`phase12.5/05_rate_limiting.md`)
- [x] 통합 테스트 문서 업데이트 (existsByEmail → existsByEmailAndIsDeletedFalse)
- [ ] API 문서 업데이트 (Swagger/OpenAPI)
- [ ] 보안 정책 문서화
- [ ] 운영 가이드 작성

---

## 📝 업데이트된 문서 목록

### 코드 작업 지침
1. ✅ `soft_delete_username_strategy.md` - 전면 개편 (모든 개인정보 재사용 가능)
2. ✅ `entity_repository_design.md` (끝난거) - 변경사항 명시 및 최신 문서 참조

### 프로젝트 작업 지침
3. ✅ `phases/phase01_domain_repository.md` - Repository 메서드 목록 업데이트
4. ✅ `phases/phase07_controller_layer.md` - existsByEmailAndIsDeletedFalse 사용
5. ✅ `phases/phase07/01_auth_controller.md` - 이메일 중복 체크 로직 업데이트
6. ✅ `phases/phase09_10_integration_test.md` - 모든 테스트 케이스 업데이트
7. ✅ `phases/phase15/overview.md` - Repository 메서드명 수정
8. ✅ `phases/phase15.1/02_service.md` - existsByPhoneNumberAndIsDeletedFalse 사용
9. ✅ `phases/phase12.5/05_rate_limiting.md` - 다층 Rate Limiting 정책 추가
10. ✅ `작업 기록/account-update-v3-security-improvements.md` - 이 문서 작성 및 업데이트

---

## 📚 참고 문서

- `soft_delete_username_strategy.md` - **최신 Soft Delete 전략** (2025-12-01 업데이트)
- `entity_repository_design.md` (끝난거) - 초기 설계 참고용
- `phase12.5/05_rate_limiting.md` - Rate Limiting 설계 및 구현
- `SecurityConfig.java` - Spring Security 설정
- `RateLimitFilter.java` - Rate Limiting 구현체

---

**작성자**: GitHub Copilot (AI Assistant)  
**최종 업데이트**: 2025-12-01
