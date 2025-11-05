# Phase 4-10: 구현 상세 (링크 참조)

> 각 Phase별 상세 내용은 원본 [implementation_plan.md](../implementation_plan.md)를 참고하세요.  
> 이 파일들은 Phase 10 완료 후 상세 계획이 확정되면 분리할 예정입니다.

---

## Phase 4: DTO Layer (Request/Response)

**목표:** 회원가입에 필요한 DTO 작성 및 Bean Validation 적용

**생성 파일:**
- `dto/user/request/SignupRequest.java`
- `dto/user/response/AccountResponse.java`  
- `dto/user/response/MessageResponse.java`

**상세 내용:** [implementation_plan.md](../implementation_plan.md) 참조

---

## Phase 5: Service Layer + 도메인 예외 (동시 작성)

**목표:** AccountService 구현 및 도메인 예외 즉시 생성

**생성 파일:**
- `service/user/AccountService.java`
- `service/user/impl/AccountServiceImpl.java`
- `exception/DuplicateEmailException.java`
- `exception/AccountNotFoundException.java`

**상세 내용:** [implementation_plan.md](../implementation_plan.md) 참조

---

## Phase 6: Service 단위 테스트 (Mockito)

**목표:** Mockito로 의존성 모킹 및 정상/예외 케이스 검증

**생성 파일:**
- `test/java/com/softwarecampus/backend/service/user/AccountServiceImplTest.java`

**상세 내용:** [implementation_plan.md](../implementation_plan.md) 참조

---

## Phase 7: Controller Layer (회원가입 API)

**목표:** 회원가입 REST API 구현 (POST /api/auth/signup)

**생성 파일:**
- `controller/user/AuthController.java`

**상세 내용:** [implementation_plan.md](../implementation_plan.md) 참조

---

## Phase 8: Controller 슬라이스 테스트 (@WebMvcTest)

**목표:** MockMvc로 HTTP 요청/응답 테스트

**생성 파일:**
- `test/java/com/softwarecampus/backend/controller/user/AuthControllerTest.java`

**상세 내용:** [implementation_plan.md](../implementation_plan.md) 참조

---

## Phase 9: Repository 테스트 (@DataJpaTest)

**목표:** Repository 쿼리 메서드 동작 검증 (실제 DB 사용)

**생성 파일:**
- `test/java/com/softwarecampus/backend/repository/user/AccountRepositoryTest.java`

**상세 내용:** [implementation_plan.md](../implementation_plan.md) 참조

---

## Phase 10: 통합 테스트 (회원가입 E2E)

**목표:** 실제 Spring Context로 전체 플로우 검증

**생성 파일:**
- `test/java/com/softwarecampus/backend/integration/SignupIntegrationTest.java`

**상세 내용:** [implementation_plan.md](../implementation_plan.md) 참조

---

## 📝 참고

**Phase 4-10 상세 계획 분리 예정:**
- Phase 10 완료 후, 실제 작업 경험을 바탕으로 각 Phase별 상세 가이드 문서 작성
- 현재는 `implementation_plan.md`에 모든 구현 코드와 설명이 포함되어 있음

---

## 🔜 다음 단계
Phase 11-20: JWT 및 로그인/마이페이지 (Phase 10 완료 후 설계)
