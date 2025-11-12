# Phase 4-10: 구현 상세 (링크 참조)

> 각 Phase별 상세 내용은 개별 문서를 참고하세요.

---

## Phase 4: DTO Layer (Request/Response) ✅

**목표:** 회원가입에 필요한 DTO 작성 및 Bean Validation 적용

**생성 파일:**
- `dto/user/SignupRequest.java` (accountType, academyId 필드 포함)
- `dto/user/AccountResponse.java`
- `dto/user/MessageResponse.java` (of() 메서드)
- `validation/ValidAccountType.java` (커스텀 검증)
- `validation/AccountTypeValidator.java`

**상세 내용:** [phase04_dto_layer.md](phases/phase04_dto_layer.md)

**상태:** ✅ 완료 (2025-11-05)

---

## Phase 5: Service Layer + 도메인 예외 ✅

**목표:** SignupService, ProfileService 구현 및 도메인 예외 생성

**생성 파일:**
- `service/user/signup/SignupService.java`
- `service/user/signup/SignupServiceImpl.java`
- `service/user/profile/ProfileService.java`
- `service/user/profile/ProfileServiceImpl.java`
- `util/EmailUtils.java` (RFC 5322/5321/1035 검증, PII 마스킹)
- `exception/user/DuplicateEmailException.java`
- `exception/user/AccountNotFoundException.java`
- `exception/user/InvalidInputException.java`

**상세 내용:** [phases/phase05/](phases/phase05/) (8개 문서)

**상태:** ✅ 완료 (2025-11-11)

---

## Phase 6: Service 단위 테스트 (Mockito) ✅

**목표:** Mockito로 의존성 모킹 및 정상/예외 케이스 검증

**생성 파일:**
- `test/.../service/user/signup/SignupServiceImplTest.java` (10 tests)
- `test/.../service/user/profile/ProfileServiceImplTest.java` (7 tests)
- `test/.../util/EmailUtilsTest.java` (37 tests)

**테스트 결과:** 51/51 passing

**상세 내용:** [phases/phase06/](phases/phase06/) (3개 문서)

**상태:** ✅ 완료 (2025-11-12)

---

## Phase 7: Controller Layer (회원가입 API) ✅

**목표:** 회원가입 REST API 구현 (POST /api/v1/auth/signup, GET /check-email)

**생성 파일:**
- `controller/user/AuthController.java`
  - `@Validated` 클래스 레벨 검증
  - `@Email` 파라미터 검증
  - RESTful: 201 Created + Location 헤더
  - PII 마스킹 로깅

**SignupService 확장:**
- `isEmailAvailable(String email)` 메서드 추가

**상세 내용:** [phases/phase07/](phases/phase07/) (4개 문서)

**상태:** ✅ 완료 (2025-11-12)

---

## Phase 8: Controller 슬라이스 테스트 (@WebMvcTest) ✅

**목표:** MockMvc로 HTTP 요청/응답 테스트

**생성 파일:**
- `test/.../controller/user/AuthControllerTest.java` (12 tests)
  - `@AutoConfigureMockMvc(addFilters = false)` Security 비활성화
  - MockMvc + @MockBean(SignupService)
  - Bean Validation, 중복 확인, Location 헤더 검증

**테스트 결과:** 12/12 passing

**상세 내용:** [phases/phase07/03_controller_test.md](phases/phase07/03_controller_test.md)

**상태:** ✅ 완료 (2025-11-12)

---

## Phase 9-10: 통합 테스트 (회원가입 E2E + Repository 검증) 🔜

**목표:** 전체 Spring Context로 회원가입 플로우 검증 + Repository 동작 확인

**통합 이유:** 
- Repository 단독 테스트는 Spring Data JPA 쿼리 메서드 검증에 불과
- 통합 테스트에서 Repository 실제 동작을 함께 검증하는 것이 효율적
- Phase 9(Repository 테스트) + Phase 10(통합 테스트) 병합

**생성 예정 파일:**
- `test/.../integration/SignupIntegrationTest.java`
  - @SpringBootTest + @AutoConfigureMockMvc
  - 실제 DB 사용 (H2 인메모리)
  - Controller → Service → Repository 전체 플로우
  - Repository 쿼리 메서드 동작 검증 포함

**테스트 시나리오:**
1. 회원가입 성공 (USER)
2. 회원가입 성공 (ACADEMY)
3. 이메일 중복 확인 (Repository.existsByEmail 검증)
4. DB 저장 확인 (Repository.save + findByEmail 검증)
5. 전화번호 중복 확인 (Repository.existsByPhoneNumber 검증)
6. 이메일 중복 확인 API (/check-email)

**Repository 검증 항목:**
- `existsByEmail()` 동작 확인
- `findByEmail()` 동작 확인
- `save()` + `findById()` 저장 및 조회
- UNIQUE 제약 조건 동작 (DataIntegrityViolationException)

**예상 시간:** 2-3시간

**상태:** 🔜 대기 중

---

## 📊 진행 상황 요약

| Phase | 상태 | 생성 파일 수 | 테스트 수 | 완료일 |
|-------|------|------------|-----------|--------|
| Phase 4 | ✅ | 5개 | - | 2025-11-05 |
| Phase 5 | ✅ | 8개 | - | 2025-11-11 |
| Phase 6 | ✅ | 3개 | 51 passing | 2025-11-12 |
| Phase 7 | ✅ | 2개 | - | 2025-11-12 |
| Phase 8 | ✅ | 1개 | 12 passing | 2025-11-12 |
| Phase 9-10 | 🔜 | - | - | - |

**총 테스트:** 63/63 passing (100%)
- Service Layer: 51 tests
- Controller Layer: 12 tests

**Phase 9-10 통합 이유:** Repository 단독 테스트는 Spring Data JPA 검증에 불과하므로, 통합 테스트에서 Repository 동작을 함께 검증하는 것이 효율적

---

## 📝 다음 단계

### 즉시 진행 가능:
1. **Phase 9-10: 통합 테스트** (@SpringBootTest) ← 다음 작업
   - 회원가입 E2E 플로우 검증
   - Repository 쿼리 메서드 동작 확인
   - 실제 DB 저장 및 조회 검증
   - UNIQUE 제약 조건 동작 확인

2. **Phase 11: 보안 강화**
   - Rate Limiting (/check-email 보호)
   - CORS 설정
   - TODO 주석 정리

### 보안 강화 (Phase 11):
- **Rate Limiting**: /check-email 엔드포인트 보호 (Bucket4j)
- **CORS 설정**: WebConfig.java 추가
- **문서화**: 보안 정책 및 설계 결정 기록

---

## 🔜 Phase 12-20
JWT 및 로그인/마이페이지 (Phase 11 완료 후 설계)
