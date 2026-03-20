# softwarecampus-backend ✨

> ⚠️ **포트폴리오 고지**: 사이트 개발 중 채택되지 않은 B안으로, 포트폴리오 용도로 사용을 허가받았습니다.

## 프로젝트 소개

**소프트웨어캠퍼스**는 부트캠프 정보를 한눈에 보고, 실제 수료생의 인증 리뷰까지 확인할 수 있는 웹서비스입니다.

- 진행 중/개설 예정인 부트캠프 과정 정보 제공 (취업예정자·재직자 과정 포함)
- 수료증 인증 기반의 실제 수료생 후기 공유
- 개발자 커뮤니티 기능 (정보 공유 / 토론)

---

## 기술 스택 🔧

| 구분 | 사용 기술 |
| --- | --- |
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.5.6 |
| 빌드 도구 | Maven (Wrapper 포함) |
| ORM | Spring Data JPA (Hibernate) |
| 데이터베이스 | MySQL 8.4.5 LTS |
| 캐시 / 세션 | Redis + Spring Session JDBC |
| 보안 | Spring Security + JWT (jjwt 0.13.0) |
| 파일 스토리지 | AWS SDK v2 S3 (2.38.7) |
| 이메일 | Spring Boot Starter Mail |
| API 문서 | SpringDoc OpenAPI 3 (2.7.0) |
| 배포 | Docker Compose + GitHub Actions |

---

## 주요 구현 포인트 ✅

- **JWT 인증/인가**: Access Token + Spring Security 필터 체인으로 stateless 인증 구현
- **Soft Delete**: 물리 삭제 없이 `deletedAt` 필드로 논리 삭제, 전 계층에서 `deletedAt IS NULL` 조건 일관 적용
- **수료증 인증**: S3 이미지 업로드 후 관리자 승인 플로우로 리뷰 신뢰성 확보
- **Redis 캐시**: 조회 빈도 높은 과정 목록 등 캐싱 적용
- **N+1 방지**: `@EntityGraph` / JPQL `JOIN FETCH`로 연관 엔티티 즉시 로딩 최적화
- **RFC 9457 오류 응답**: 전역 `@ControllerAdvice`에서 Problem Details(`application/problem+json`) 포맷 통일

---

## 패키지 구조 🗂️

```
src/
├── main/
│   ├── java/com/softwarecampus/backend/
│   │   ├── controller/      # REST 컨트롤러
│   │   ├── service/         # 서비스 계층 (기능별 패키지 분리)
│   │   ├── repository/      # JPA 리포지토리
│   │   ├── domain/          # 엔티티, 값객체, enum
│   │   ├── dto/             # 요청/응답 DTO
│   │   ├── config/          # 전역 설정
│   │   ├── exception/       # 예외 클래스 + @ControllerAdvice
│   │   ├── security/        # JWT 필터 / Security 설정
│   │   └── util/            # 공용 유틸리티
│   └── resources/
│       ├── application.properties
│       ├── application-local.properties   # 로컬 환경 (gitignore)
│       └── application-prod.properties
└── test/
    └── java/com/softwarecampus/backend/   # 메인 구조 미러링
```

의존 방향: `controller → service → repository` (역참조 금지)

---

## 로컬 실행 ▶️

**요구사항**: JDK 17, MySQL, Redis

```bash
# 로컬 프로필로 실행
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 전체 빌드 + 검증
./mvnw clean verify

# 테스트만 실행
./mvnw test
```

환경변수는 `.env.example`을 복사해 `.env`로 생성 후 설정합니다.

---

## 환경 설정 🔐

| 환경변수 | 설명 |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL 접속 정보 |
| `JWT_SECRET` | JWT 서명 키 |
| `REDIS_PASSWORD` | Redis 인증 (운영 환경 필수) |
| `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `S3_BUCKET` | S3 파일 업로드 |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | 이메일 발송 |

로컬 개발은 `application-local.properties` 또는 `.env` 파일로 관리합니다.

---

## API 문서 📄

로컬 실행 후 아래 주소에서 확인할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 세부 문서 📚

| 문서 | 경로 |
| --- | --- |
| API 가이드라인 | `.md/API_GUIDELINES.md` |
| JPA 네이밍 규칙 | `.md/JPA_GUIDELINE.md` |
| 파일 업로드 설계 | `.md/FILE_UPLOAD_DESIGN.md` |
| AI 에이전트 지침 | `AGENTS.md` |
