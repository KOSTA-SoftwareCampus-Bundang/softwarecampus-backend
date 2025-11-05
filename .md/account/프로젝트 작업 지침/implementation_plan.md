# Account 도메인 구현 계획 (최종 설계안)

**담당자:** 태윤  
**목표:** Spring Boot + Spring Security를 사용한 인증/인가 시스템 구축  
**작업 기간:** 2025-10-29 ~ (예상)

---

## 📋 전체 작업 Phase 개요

```
Phase 1: Domain & Repository ✅ (완료)
Phase 2: GlobalExceptionHandler 기본 틀 ✅ (완료)
Phase 3: 기본 보안 설정 (PasswordEncoder) ✅ (완료)
Phase 4: DTO Layer (Request/Response) ✅ (완료)
Phase 5: Service Layer + 도메인 예외 (동시 작성)
Phase 6: Service 단위 테스트 (Mockito)
Phase 7: Controller Layer (회원가입 API)
Phase 8: Controller 슬라이스 테스트 (@WebMvcTest)
Phase 9: Repository 테스트 (@DataJpaTest)
Phase 10: 통합 테스트 (회원가입 E2E)
Phase 11: JWT 구현 (JwtTokenProvider)
Phase 12: JWT 단위 테스트
Phase 13: UserDetailsService 구현
Phase 14: UserDetailsService 테스트
Phase 15: Security 고도화 (필터, 권한)
Phase 16: 로그인 API
Phase 17: 로그인 테스트 (Service + Controller)
Phase 18: 마이페이지 API
Phase 19: 마이페이지 테스트 (인증 포함)
Phase 20: 통합 테스트 (전체 플로우)
```

---

## 📂 Phase별 상세 문서

각 Phase의 상세 구현 계획은 아래 파일을 참고하세요:

- **[Phase 1: Domain & Repository](./phases/phase01_domain_repository.md)** ✅
- **[Phase 2: GlobalExceptionHandler 기본 틀](./phases/phase02_exception_handler.md)** ✅
- **[Phase 3: 기본 보안 설정](./phases/phase03_security_config.md)** ✅
- **[Phase 4: DTO Layer](./phases/phase04_dto_layer.md)** ✅
- **[Phase 5: Service Layer](./phases/phase05_service_layer.md)** ✅
- **[Phase 6: Service 단위 테스트 (Mockito)](./phases/phase06_service_unit_test.md)** 🚧
- **[Phase 7-10: Controller~통합테스트](./phases/phase04_10_implementation.md)** (Phase 6 완료 후 분리 예정)
- **[Phase 11-20: JWT 및 로그인/마이페이지](./phases/phase11_20_jwt_login_mypage.md)** (Phase 10 완료 후 설계)

> 💡 **참고**: Phase 4-10의 상세 구현 코드는 `phase04_10_implementation.md` 파일에 포함되어 있습니다.  
> Phase 10 완료 후 실제 작업 경험을 바탕으로 각 Phase를 개별 파일로 분리할 예정입니다.

---

## 📝 작업 진행 체크리스트

- [x] Phase 1: Domain & Repository
- [x] Phase 2: GlobalExceptionHandler 기본 틀
- [x] Phase 3: 기본 보안 설정
- [x] Phase 4: DTO Layer
- [x] Phase 5: Service Layer + 도메인 예외
- [ ] Phase 6: Service 단위 테스트 (Mockito)
- [ ] Phase 7: Controller Layer
- [ ] Phase 8: Controller 슬라이스 테스트
- [ ] Phase 9: Repository 테스트
- [ ] Phase 10: 통합 테스트 (회원가입)
- [ ] Phase 11-20: JWT 및 로그인/마이페이지

---

## 🎯 각 Phase별 검증 기준

### Phase 완료 기준
1. **코드 작성 완료** - 해당 Phase의 모든 파일 생성
2. **테스트 통과** - 관련 테스트가 모두 green
3. **빌드 성공** - `./mvnw clean verify` 통과
4. **문서 갱신** - 작업 기록에 내역 기록

### 다음 Phase로 넘어가는 조건
- 현재 Phase의 모든 검증 기준 충족
- 코드 리뷰 완료 (필요 시)
- Git 커밋 완료

---

**작성일:** 2025-10-29  
**최종 수정:** 2025-11-05  
**상태:** Phase 1-5 완료, Phase 6 진행 예정