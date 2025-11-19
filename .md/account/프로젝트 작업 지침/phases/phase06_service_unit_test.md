# Phase 6: Service 단위 테스트 (Mockito) - 인덱스

**목표:** Mockito를 사용한 Service Layer 단위 테스트 작성  
**담당자:** 태윤  
**상태:** ✅ 완료 (51/51 테스트 통과)

---

## 📋 작업 개요

Phase 5에서 구현한 Service Layer의 비즈니스 로직을 검증하는 단위 테스트를 작성합니다. Mockito를 사용하여 의존성(Repository, PasswordEncoder 등)을 모킹하고, 정상 케이스와 예외 케이스를 모두 테스트합니다.

> ⚠️ **이 파일은 인덱스 파일입니다.**  
> 상세 내용은 `phase06/` 디렉토리의 개별 문서를 참조하세요.

---

## 📂 문서 구조

1. **[SignupService 단위 테스트](./phase06/01_signup_service_test.md)** - 회원가입 Service 테스트 (10개)
2. **[ProfileService 단위 테스트](./phase06/02_profile_service_test.md)** - 프로필 조회 Service 테스트 (7개)
3. **[EmailUtils 단위 테스트](./phase06/03_email_utils_test.md)** - 이메일 유틸리티 테스트 (37개)
4. **[Mockito 패턴 및 검증](./phase06/04_mockito_patterns.md)** - Mock 설정, 행위 검증, 테스트 실행

---

## 🎯 테스트 원칙

- **단위 테스트**: Service Layer만 격리하여 테스트
- **Mockito 모킹**: 외부 의존성(Repository, PasswordEncoder) 모킹
- **Given-When-Then**: 테스트 구조 명확화
- **예외 케이스**: 정상 케이스뿐만 아니라 예외 상황도 철저히 검증
- **행위 검증**: `verify()`로 메서드 호출 여부 확인

---

## 📊 테스트 통계

**총 테스트 개수:** 54개
- SignupServiceImplTest: 10개
- ProfileServiceImplTest: 7개
- EmailUtilsTest: 37개

**커버리지 달성:**
- Line Coverage: 85% 이상
- Branch Coverage: 75% 이상
- Method Coverage: 90% 이상

**실제 소요 시간:** 3시간

---

## 🎯 빠른 참조

### 생성된 테스트 파일

```text
src/test/java/com/softwarecampus/backend/
├─ service/user/
│  ├─ signup/
│  │  └─ SignupServiceImplTest.java       ✅ 10 tests
│  └─ profile/
│     └─ ProfileServiceImplTest.java      ✅ 7 tests
└─ util/
   └─ EmailUtilsTest.java                 ✅ 37 tests
```

### 테스트 실행

```bash
# 전체 테스트
mvn test

# Service 테스트만
mvn test -Dtest=*ServiceImplTest

# 커버리지 리포트
mvn test jacoco:report
```

### Mockito 핵심 패턴

```java
// Given: Mock 설정
when(repository.findById(1L)).thenReturn(Optional.of(entity));

// When: 실행
Result result = service.doSomething(1L);

// Then: 검증
assertThat(result).isNotNull();
verify(repository).findById(1L);
```

---

## ✅ 완료 기준

- [x] 테스트 파일 생성 (3개)
- [x] 정상 케이스 테스트 (회원가입, 조회, 검증, 마스킹)
- [x] 예외 케이스 테스트 (형식 오류, 중복, 미존재)
- [x] Mockito 패턴 적용 (`@Mock`, `@InjectMocks`, `when()`, `verify()`)
- [x] 51/51 테스트 PASS
- [x] 빌드 성공
- [x] Given-When-Then 구조 준수

---

## 🔜 다음 단계

**Phase 7: Controller Layer (회원가입 API)**
- `AuthController.java` 작성
- POST /api/v1/auth/signup 엔드포인트
- GET /api/v1/auth/check-email 엔드포인트
- Bean Validation 적용
- HTTP 201 Created + Location 헤더

---

**작성일:** 2025-11-12  
**최종 수정:** 2025-11-12 (문서 구조 최적화)  
**상태:** ✅ 완료 (51/51 테스트 통과)
