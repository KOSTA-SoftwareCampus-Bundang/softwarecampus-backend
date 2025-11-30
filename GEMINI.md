# Gemini 전용 지침

> ⚙️ 이 모델은 `AGENTS.md`를 기본 행동 지침으로 참조합니다. 아래 항목은 Gemini 사용 시 추가로 적용되는 운영 지침입니다.

## 역할과 범위
- 백엔드(Spring Boot) 기여를 우선 지원하며, 문서/리뷰/리팩터링을 포함합니다.
- 답변은 항상 한국어로 간결하고 실행 가능하게 작성합니다.

## 우선순위
1) 정확성 2) 레이어 규칙 준수(Controller→Service→Repository) 3) 최소 변경 4) 테스트 동반

## 필수 검증 사항 (Critical Checklist)

### 1. Soft Delete 준수
- **조회**: 모든 조회 로직(Repository, Service)에서 `deleted_at IS NULL` 또는 `isDeleted = false` 조건을 **반드시** 포함해야 합니다.
  - 예: `findById` 대신 `findByIdAndDeletedAtIsNull` 사용 권장
  - JPQL 작성 시 `WHERE` 절에 `isDeleted = false` 추가 필수
- **삭제**: 물리 삭제(`delete()`)를 금지하고, 논리 삭제(`markDeleted()`)를 사용합니다.

### 2. 보안 및 권한 검증
- **Controller**: 모든 변경(POST, PUT, DELETE) 요청 및 민감한 조회 요청에 대해 `@PreAuthorize`를 사용하여 역할(Role) 검증을 명시합니다.
  - 예: `@PreAuthorize("hasRole('ADMIN')")`
- **Service**: 데이터 소유권 확인이 필요한 경우(예: 본인 글 수정/삭제), Service 계층에서 현재 사용자와 리소스 소유자를 비교하는 로직을 반드시 포함합니다.
