# softwarecampus-backend ✨


## 무슨 프로젝트인가요?

소프트캠퍼스는 부트캠프 정보를 한눈에 보고, 실제 수료생의 인증 리뷰까지 확인할 수 있는 웹서비스예요.

핵심 기능은 다음과 같습니다:

- 진행 중/개설 예정인 부트캠프 과정 정보 제공
- 취업예정자 과정뿐 아니라 재직자 과정 정보도 제공
- 개발 관련 커뮤니티 기능 제공(정보 공유/토론)
- 수료증 인증을 기반으로 한 실제 수료생 후기 공유

---

## 개발 환경 한눈에 보기 🔧

| 구분 | 사용 기술 |
| --- | --- |
| 데이터베이스 | MySQL 8.4.5 LTS |
| 메인 개발 언어 | Java + Typescript(TSX) |
| 웹 서버 | Nginx |
| 백엔드 프레임워크 | Spring Boot 3.5.6 |
| JDK Version | Zulu 21 (17버전 사용) |
| 백엔드 빌드 도구 | Maven (Wrapper 포함) |
| 프론트엔드 프레임워크 | React 18 + Vite |
| CSS 프레임워크 | Tailwind CSS |
| 테스트 솔루션 | JUnit5 + Mockito |
| 배포 방식 | Docker Compose + GitHub Actions (자동화) |
| 브랜치 전략 | GitHub Flow 스타일 (Trunk-Based) |


---

## 패키지 구조 🗂️

- 애플리케이션 루트: `src/main/java/com/softwarecampus/backend`
- 레이어 규칙: `controller → service → repository` (컨트롤러는 DTO만 입출력)
- 주요 패키지
  - `controller`: REST 컨트롤러(예: `HomeController`, `AdminController`)
  - `service`, `service/impl`: 트랜잭션 경계 및 구현체
  - `repository`: Spring Data JPA 리포지토리
  - `domain`: JPA 엔티티(예: `domain/user/Account`)와 공통 베이스
  - `dto`: 요청/응답 DTO, 매퍼
  - `security`: 보안 설정(예: `SecurityConfig`)
  - `config`, `exception`, `util`: 전역 구성/예외/유틸
- 리소스: `src/main/resources` (`application.properties`, 프로필/정적 리소스)
- 테스트: `src/test/java/com/softwarecampus/backend` (메인 구조 미러링)

- 트랜잭션은 서비스 계층에서 관리합니다(`@Transactional`).
- 컨트롤러는 엔티티를 직접 노출하지 않고 DTO만 사용합니다.
- 의존 방향은 한 방향으로: `controller → service → repository`.
- 예외는 전역 처리기에서 일관되게 처리합니다(`@ControllerAdvice`).

예시 구조

```
src/
  main/
    java/com/softwarecampus/backend/...   # controller, service, domain
    resources/                            # application-*.properties, static, templates
  test/
    java/com/softwarecampus/backend/...   # unit/integration tests
```

---

## 로컬에서 바로 돌려보기 ▶️

- 요구사항: `JDK 17`, `MySQL` 접근 가능, 환경변수/프로필 설정
- 실행: `./mvnw spring-boot:run`
- 프로필: `-Dspring-boot.run.profiles=local` 또는 `--spring.profiles.active=local`

### 빌드/테스트 빠른 레퍼런스

- 전체 빌드/검증: `./mvnw clean verify`
- 테스트만: `./mvnw test`
- 패키징(JAR): `./mvnw clean package`

> 팁: Maven Wrapper를 기본으로 사용합니다. 로컬 Maven 설치가 없어도 OK. 👍

---

## 코딩 스타일과 네이밍 ✍️

- Java 17, 4칸 들여쓰기, UTF-8
- 패키지: 소문자 dot, 클래스: PascalCase, 메서드/필드: camelCase
- 와일드카드 임포트 지양, 작은 단위로 응집도 높게
- Lombok은 합리적으로, 가독성을 해치지 않는 선에서 사용
- JPA 네이밍은 `.md/JPA_GUIDELINE.md` 참고
- API 규칙은 `.md/API_GUIDELINES.md`를 따릅니다. 오류 응답은 RFC 9457 Problem Details(`application/problem+json`) 권장

---

## 테스트 가이드 ✅

- 프레임워크: JUnit 5 + Spring Boot Test
- 파일명: `*Tests.java` (예: `UserServiceTests.java`)
- 단위 테스트 우선, 필요 시 통합 테스트(`@SpringBootTest`)
- 슬라이스 테스트 권장: 컨트롤러 `@WebMvcTest`, 리포지토리 `@DataJpaTest`
- 실행: `./mvnw test`

---

## 보안과 설정 🔐

- 비밀정보는 커밋 금지. 환경변수나 프로필(`application-local.properties`)로 관리
- DB 설정은 `SPRING_DATASOURCE_*` 또는 프로필 파일 사용
- 외부 입력은 검증/정규화, PII는 로깅 금지

### 운영 환경 필수 설정 (2025-12-01 업데이트)

**Redis 보안 설정:**
- 운영 환경에서는 반드시 `REDIS_PASSWORD` 환경변수를 설정하세요
- 예시: `REDIS_PASSWORD=your-strong-redis-password`
- 로컬 개발: 비밀번호 없이 실행 가능 (기본값: 빈 문자열)

**데이터베이스 SSL/TLS:**
- 운영 환경: `DB_USE_SSL=true` (기본값, TLS 암호화 활성화)
- 로컬 개발: `DB_USE_SSL=false` (자체 서명 인증서 문제 회피)

**환경변수 템플릿:**
- `.env.example` 파일을 복사하여 `.env` 생성
- 모든 환경변수 설명 및 예시 포함

---

## 문서 모음 📚

- AI 통합 지침: `AGENTS.md`
- API 가이드라인: `.md/API_GUIDELINES.md`
- JPA 네이밍: `.md/JPA_GUIDELINE.md`
- 도움말/참고: `.md/HELP.md`
- 계획/히스토리: `.md/plan.md`, `.md/work-history.md`(로컬 메모)

---

## 커밋/PR 가이드 📨

- Conventional Commits: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `build:`, `chore:`
- 작은 단위로 자주 커밋, 비자명 변경은 의도/영향을 본문에 간단히 설명
- PR에는 요약, 주요 변경, 테스트 방법(명령/엔드포인트), 관련 이슈를 포함

---

## 팀이 일하는 방식(AGENTS 요약) 🤝

- 역할/태도: 신뢰성 있게, 명확하고 간결하게. 항상 “계획 → 최소 변경 → 검증”.
- 우선순위: 보안/안전 > 정확성 > 유지보수성 > 성능.
- 외부 라이브러리 추가는 근거와 대안을 함께.
- Git 흐름: 기능 브랜치에서 작업, main 직푸시 지양.

---

## 참고: 레이어링 네이밍 규칙 🧭

- 명명 규칙: `XxxController`, `XxxService`, `XxxServiceImpl`, `XxxRepository`, `XxxRequest/Response`
- DTO만 컨트롤러에 노출, 엔티티 직렬화 금지
- 트랜잭션은 서비스 계층이 책임

---

