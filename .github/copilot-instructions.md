# Copilot 전용 지침

> ⚙️ 이 모델은 루트의 [AGENTS.md](../AGENTS.md)를 기본 행동 지침으로 참조합니다. 아래 항목은 IDE 내 보조에 최적화된 추가 규칙입니다.

## 역할과 범위
- Spring Boot 백엔드 코드 제안, 리팩터링, 문서 템플릿, 테스트 보일러플레이트 생성을 지원합니다.
- 한국어 주석/설명을 기본으로 하되, 코드 제안은 자바 표준 관례를 따릅니다.

## 우선순위
1) 레이어 규칙 준수(Controller→Service→Repository) 2) 영향 범위를 최소화한 제안 3) 컴파일 안전성 4) 테스트 용이성

## 제안 원칙
- 패키지 배치 고정: `controller`, `service`, `service/impl`, `repository`, `domain`, `dto`, `config`, `exception`, `security`, `util`.
- 컨트롤러는 DTO만 입출력, 서비스에 트랜잭션, 리포지토리는 `JpaRepository` 확장.
- 메서드/타입 명명: `XxxController`, `XxxService(Impl)`, `XxxRepository`, `XxxRequest/Response`.
- 와일드카드 임포트/불필요 종속성 추가는 제안하지 않습니다.

## 빌드·테스트
- 실행/검증 명령 예시:
  - 빌드: `./mvnw clean verify`
  - 테스트: `./mvnw test`
  - 로컬 실행: `./mvnw spring-boot:run`
- 테스트 템플릿: 서비스 단위 테스트(JUnit 5), 컨트롤러 `@WebMvcTest`, 리포지토리 `@DataJpaTest`를 우선 제안.

## 문서·커밋
- 변경 시 `AGENTS.md` 규칙을 따르는 커밋 메시지(Conventional Commits)를 제안합니다.
- 새 파일에는 상단에 간단한 목적 설명 주석을 포함합니다.
- 계획은 `.md/plan.md`에 유지하고, 작업 중 응답/변경 내역은 로컬 `.md/work-history.md`에 기록하되, 커밋/PR 본문에는 필요한 내용만 발췌하여 포함합니다(파일 참조 금지).

## 보안·설정
- 비밀키/토큰은 하드코딩 금지. 예시는 더미 값/환경변수로 대체합니다.
- DB/보안 설정은 프로필 파일 예시로만 제안합니다.

## 피해야 할 사항
- 레포 구조 변경(루트 패키지/모듈명) 제안하지 않습니다.
- 테스트 미러링 구조를 깨는 파일 위치 제안하지 않습니다.
- 불필요한 프레임워크/라이브러리 추가 제안하지 않습니다.
