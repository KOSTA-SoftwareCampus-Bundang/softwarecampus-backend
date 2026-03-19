---
title: Backend Skill & Tool Usage Guide
version: 1.0.0
last_updated: 2026-03-20 (KST)
applies_to: Claude / Codex / Gemini
---

# 백엔드 스킬 및 도구 활용 가이드

> 에이전트별 공통 스킬(Claude 스킬 호출, Codex/Gemini 가이드)은 `../SKILL.md`를 참조합니다 (경로가 존재할 경우).

---

## 빌드·테스트·실행

```bash
# 로컬 실행
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 전체 빌드 + 검증
./mvnw clean verify

# 테스트만 실행
./mvnw test

# 특정 테스트 클래스 실행
./mvnw test -Dtest=UserServiceTests

# JAR 패키징
./mvnw clean package -DskipTests
```

---

## API 문서

- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (로컬 실행 시)
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- API 설계 원칙: `.md/API_GUIDELINES.md`

---

## 코드 품질

- Java 코드 스타일: IntelliJ 기본 포맷터 사용
- 레이어 규칙 위반 검토 후 수정 제안
- 빌드 후 반드시 `./mvnw test` 통과 확인

---

## 보안 체크리스트

작업 완료 전 반드시 확인:

- [ ] 비밀정보(패스워드, 토큰, 키)가 코드에 하드코딩되지 않았는가?
- [ ] 모든 변경 API에 `@PreAuthorize` 적용되었는가?
- [ ] Soft Delete 조건(`deletedAt IS NULL`)이 모든 조회에 포함되었는가?
- [ ] 연관 엔티티 접근에서 N+1 문제가 발생하지 않는가?
- [ ] 환경변수·비밀정보가 `application-local.properties` 또는 `.env`에만 존재하는가?

---

## 테스트 가이드

| 상황 | 권장 테스트 |
|------|------------|
| 새 Service 로직 추가 | `@ExtendWith(MockitoExtension.class)` 단위 테스트 |
| 새 API 엔드포인트 추가 | `@WebMvcTest` 슬라이스 테스트 |
| 새 Repository 쿼리 추가 | `@DataJpaTest` 슬라이스 테스트 |
| 전체 흐름 검증 | `@SpringBootTest` 통합 테스트 |

**테스트 우선순위**: 보안·인증 로직 → 핵심 비즈니스 로직 → API 엔드포인트

---

## 작업 흐름 패턴

### 새 기능 추가 시
1. `.md/plan.md`에 계획 기록
2. 새 브랜치 생성 (`feat/feature-name`)
3. 구현 → `./mvnw test` → 빌드 통과 확인
4. 커밋 (Conventional Commits)
5. fetch → main merge → push

### 버그 수정 시
1. 재현 조건 파악 후 원인 분석
2. 새 브랜치 생성 (`fix/bug-description`)
3. 최소 변경으로 수정 → 회귀 테스트 확인

**문서 끝**
