# Phase 7: Controller Layer (회원가입 API)

**목표:** 회원가입 API 엔드포인트 구현 (RESTful)  
**담당자:** 태윤  
**상태:** 🚧 준비 중

---

## 📋 작업 개요

Phase 5(Service Layer)와 Phase 6(단위 테스트)를 기반으로 회원가입 API 엔드포인트를 구현합니다. RESTful API 원칙을 준수하며, Bean Validation, RFC 9457 ProblemDetail 표준을 적용합니다.

**API 원칙:**
- **RESTful**: HTTP 메서드(POST), 상태 코드(201, 400, 409), Location 헤더
- **Bean Validation**: `@Valid` + `@NotBlank`, `@Email`, `@Size`
- **ProblemDetail**: RFC 9457 표준 오류 응답
- **보안**: PII 로깅 제거, 비밀번호 평문 노출 방지
- **문서화**: OpenAPI (Swagger) 자동 생성

---

## 📂 상세 문서 (최적화)

이 문서는 토큰 효율성을 위해 주제별로 분할되었습니다:

1. **[AuthController 구현](phase07/01_auth_controller.md)**
   - AuthController.java 전체 코드
   - RESTful API 설계 원칙
   - Bean Validation 적용
   - Location 헤더 생성

2. **[API 명세서](phase07/02_api_specification.md)**
   - POST /api/v1/auth/signup
   - GET /api/v1/auth/check-email
   - 요청/응답 예시
   - 에러 응답 (RFC 9457)

3. **[Controller 테스트](phase07/03_controller_test.md)**
   - AuthControllerTest.java 전체 코드
   - @WebMvcTest + MockMvc
   - Service Layer 모킹
   - 12개 테스트 시나리오

4. **[보안 및 RESTful 원칙](phase07/04_security_restful.md)**
   - PII 로깅 제거 전략
   - HTTP 상태 코드 가이드
   - CORS 설정
   - Postman 테스트 예시

---

## 📂 생성 파일

```text
src/main/java/com/softwarecampus/backend/
└─ controller/user/
   └─ AuthController.java                 ✅ 회원가입 API Controller
```

---

---

## 📊 의존성 관계도

```text
AuthController
    ↓
SignupService (인터페이스)
    ↓
SignupServiceImpl (구현체)
    ↓
    ├─ AccountRepository.existsByEmail(String)
    ├─ AccountRepository.save(Account)
    └─ PasswordEncoder.encode(String)

예외 처리 플로우:
Controller (Bean Validation 실패)
    ↓
MethodArgumentNotValidException
    ↓
GlobalExceptionHandler
    ↓
RFC 9457 ProblemDetail (400 Bad Request)

Controller → Service (예외 발생)
    ↓
InvalidInputException / DuplicateEmailException
    ↓
GlobalExceptionHandler
    ↓
RFC 9457 ProblemDetail (400 / 409)
```

---

## 🔗 SignupService 인터페이스 확장

**기존 파일 수정:** `service/user/signup/SignupService.java`

isEmailAvailable() 메서드 추가가 필요합니다. 자세한 내용은 [AuthController 구현](phase07/01_auth_controller.md#service-확장) 참고.

---

## 📝 Phase 완료 기준

- [ ] **Controller 파일 생성**
  - [ ] `AuthController.java` 생성
  - [ ] `@RestController`, `@RequestMapping` 적용
  - [ ] `@RequiredArgsConstructor` (DI)

- [ ] **API 엔드포인트 구현**
  - [ ] POST /api/v1/auth/signup (회원가입)
  - [ ] GET /api/v1/auth/check-email (이메일 중복 확인)
  - [ ] `@Valid` Bean Validation 적용
  - [ ] Location 헤더 생성 (RESTful)

- [ ] **Service 인터페이스 확장**
  - [ ] `isEmailAvailable(String)` 메서드 추가
  - [ ] SignupServiceImpl 구현

- [ ] **로깅 및 보안**
  - [ ] PII 로깅 제거 (이메일 원본 노출 방지)
  - [ ] INFO 레벨: accountId만 로깅
  - [ ] DEBUG 레벨: 일반화된 정보만
  - [ ] 비밀번호 평문 로깅 금지

- [ ] **Controller 통합 테스트 (Phase 7)**
  - [ ] `AuthControllerTest.java` 작성 (12개 테스트)
  - [ ] `@WebMvcTest` + MockMvc 사용
  - [ ] Service Layer 모킹 (`@MockBean`)
  - [ ] HTTP 요청/응답 검증
  - [ ] Location 헤더 검증
  - [ ] ProblemDetail 검증

- [ ] **Postman 테스트**
  - [ ] 회원가입 성공 (일반/학원)
  - [ ] 이메일 형식 오류
  - [ ] 이메일 중복
  - [ ] 전화번호 중복
  - [ ] 이메일 중복 확인

---

## 🔜 다음 단계

**Phase 8: 프로필 조회 API**
- `AccountController.java` 작성
- GET /api/v1/accounts/{accountId} (ID로 조회)
- GET /api/v1/accounts/email/{email} (이메일로 조회)
- HTTP 200 OK / 404 Not Found
- ProfileService 활용

**Phase 9: 통합 테스트 및 E2E 테스트**
- Spring Boot Test (`@SpringBootTest`)
- 실제 DB 연동 (TestContainers 또는 H2)
- 전체 플로우 검증
- Postman Collection 작성

**Phase 10: OpenAPI (Swagger) 문서 자동 생성**
- Springdoc OpenAPI 의존성 추가
- `@Operation`, `@ApiResponse` 애노테이션
- Swagger UI 활성화
- API 문서 자동 생성

---

## 📚 참고 자료

### Spring MVC 문서
- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [@RestController](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/RestController.html)
- [Bean Validation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)

### RESTful API 표준
- [RFC 7231 - HTTP/1.1 Semantics](https://www.rfc-editor.org/rfc/rfc7231)
- [RFC 9457 - Problem Details](https://www.rfc-editor.org/rfc/rfc9457.html)
- [REST API 디자인 가이드](https://restfulapi.net/)

### 테스트 문서
- [@WebMvcTest](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/autoconfigure/web/servlet/WebMvcTest.html)
- [MockMvc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/MockMvc.html)
- [Testing Spring Boot Applications](https://spring.io/guides/gs/testing-web)

---

## 📊 테스트 통계

**총 테스트 개수:** 39개
- Phase 6 단위 테스트: 27개
  - SignupServiceImplTest: 10개
  - ProfileServiceImplTest: 6개
  - EmailUtilsTest: 12개
- Phase 7 Controller 통합 테스트: 12개
  - AuthControllerTest: 12개

**커버리지 목표:**
- Line Coverage: 85% 이상
- Branch Coverage: 75% 이상
- Method Coverage: 90% 이상

**예상 소요 시간:** 2-3시간
- Controller 구현: 1시간
- Controller 테스트: 1시간
- Postman 테스트: 30분

