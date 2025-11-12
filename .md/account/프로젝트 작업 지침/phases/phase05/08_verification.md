# 8. 검증 및 완료 기준

## ✅ 검증 방법

### 1. 컴파일 확인
```bash
mvn clean compile
```

### 2. Service 메서드 시그니처 확인
- `SignupService` 인터페이스와 `SignupServiceImpl` 메서드 일치 확인
- `ProfileService` 인터페이스와 `ProfileServiceImpl` 메서드 일치 확인
- IDE에서 구현 여부 검증

### 3. 예외 처리 확인
- `GlobalExceptionHandler`에 핸들러 추가 확인
- HTTP 상태 코드 매핑 적절성 확인

### 4. 단위 테스트 (Phase 6에서 작성)
- `SignupServiceImplTest` - 회원가입 정상/예외 케이스
- `ProfileServiceImplTest` - 조회 정상/예외 케이스

---

## 📝 Phase 완료 기준

### 파일 생성 완료
- [x] `SignupService.java` 인터페이스 생성
- [x] `SignupServiceImpl.java` 구현체 생성
- [x] `ProfileService.java` 인터페이스 생성
- [x] `ProfileServiceImpl.java` 구현체 생성
- [x] `InvalidInputException.java` 예외 생성
- [x] `DuplicateEmailException.java` 예외 생성
- [x] `AccountNotFoundException.java` 예외 생성
- [x] `EmailUtils.java` 유틸리티 생성

### GlobalExceptionHandler 수정
- [x] `InvalidInputException` 핸들러 추가 (400)
- [x] `DuplicateEmailException` 핸들러 추가 (409)
- [x] `AccountNotFoundException` 핸들러 추가 (404)

### MessageResponse 수정
- [x] `Status` 필드 제거
- [x] `of(String)` 팩토리 메서드로 단순화

### 보안 강화
- [x] PII 로깅 제거 (이메일 마스킹)
- [x] 동시성 안전 처리 (DB UNIQUE 제약)
- [x] RFC 표준 준수 (RFC 5322, RFC 1035)

### CodeRabbit PR 리뷰 반영
- [x] Javadoc FQCN 수정
- [x] DataIntegrityViolationException 처리 개선
- [x] 이메일 정규식 강화 (RFC 1035 하이픈 규칙)
- [x] MessageResponse Status 필드 제거
- [x] Markdown 코드 블록 언어 지정

### 코드 검증
- [x] 컴파일 성공 (`mvn clean compile`)
- [x] 인터페이스-구현체 메서드 일치
- [x] 로깅 적절히 배치 (PII 마스킹)
- [x] `@Transactional` 올바르게 적용
- [x] 도메인별 예외 패키지 분리

### 문서화
- [x] Phase 5 설계 문서 최신화
- [x] 실제 구현 내용 반영
- [x] CodeRabbit 리뷰 반영 내역 문서화
- [x] 설계 결정 사항 추가
- [x] 테스트 시나리오 문서 링크 추가

### 테스트 시나리오 작성
- [x] `signup_scenarios.md` (20개 시나리오)
- [x] `profile_scenarios.md` (8개 시나리오)
- [x] Race Condition 시나리오 포함
- [x] RFC 표준 위반 케이스 포함

---

## 🔜 다음 단계: Phase 6

### Phase 6: Service 단위 테스트 (Mockito)
- SignupServiceImplTest 작성
- ProfileServiceImplTest 작성
- Mockito로 Repository, PasswordEncoder 모킹
- 정상 케이스: 회원가입 성공, 조회 성공
- 예외 케이스: 이메일 중복, 계정 미존재, 이메일 형식 오류
- @ExtendWith(MockitoExtension.class) 사용
- EmailUtils 유틸리티 테스트 (RFC 표준 검증)

---

## 📊 테스트 시나리오 참조

- [회원가입 시나리오](../../시나리오/signup_scenarios.md) (20개)
- [프로필 조회 시나리오](../../시나리오/profile_scenarios.md) (8개)
