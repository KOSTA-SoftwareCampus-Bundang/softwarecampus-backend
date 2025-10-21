# API 가이드라인

본 문서는 Swagger/OpenAPI로 기술된 엔드포인트 계약을 보완하는 정책 문서입니다. 엔드포인트·스키마는 Swagger를 단일 진실 공급원으로 유지하고, 본 지침은 일관된 API 경험을 위한 공통 규칙을 정의합니다.

## 버전 관리
- URI 버전 권장: `/api/v1` (주요 변경 시 `v2` 추가). 헤더 버전은 보조 수단으로만 사용.
- 폐기 정책: `Deprecation`, `Sunset` 헤더 제공, 변경 로그에 명시.

## 리소스·경로·메서드
- 리소스는 복수형, 하이픈 금지: `/users`, `/courses/{id}`.
- HTTP 메서드: GET 조회, POST 생성, PUT 전체 수정, PATCH 부분 수정, DELETE 삭제.
- 관계: `/courses/{id}/lessons` 등 중첩 허용(2단계 이내 권장).

## 요청/응답 규칙
- Content-Type: `application/json; charset=utf-8`.
- JSON 키: lowerCamelCase. 날짜/시간: ISO‑8601 UTC(`2025-01-01T00:00:00Z`).
- 201 생성 시 `Location` 헤더에 새 리소스 URL 포함.
- null 남발 지양, 불필요 필드 미포함(스키마에 정의된 것만).

## 오류 표준 (RFC 9457 Problem Details)
- 모든 4xx/5xx 응답은 `Content-Type: application/problem+json`을 사용합니다.
- 표준 필드: `type`(문제 유형 URI 또는 `about:blank`), `title`(간단 설명), `status`(HTTP 코드), `detail`(구체 사유), `instance`(발생 항목 URI/식별자).
- 확장 필드 허용: `traceId`(또는 `requestId`), `errorCode`, `errors`(필드별 유효성 오류 배열) 등.
- 상태 코드 권장 매핑: 400 유효성, 401 인증, 403 인가, 404 없음, 409 충돌, 422 처리 불가, 429 한도, 500 서버.
- 예시:
```
{
  "type": "https://api.softwarecampus.com/problems/user-not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "ID '123'에 해당하는 사용자를 찾을 수 없습니다.",
  "instance": "/users/123",
  "traceId": "${X-Request-Id}",
  "errorCode": "USER_NOT_FOUND",
  "errors": []
}
```
- 유효성 오류 예시(`errors` 확장):
```
{
  "type": "https://api.softwarecampus.com/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "요청 본문에 유효하지 않은 필드가 있습니다.",
  "instance": "/users",
  "errors": [
    {"name": "email", "reason": "invalid format"},
    {"name": "age", "reason": "must be >= 18"}
  ]
}
```
- 구현 팁: Spring Boot 3/Spring 6의 `ProblemDetail`을 사용하고, 전역 예외 처리기(`@ControllerAdvice`)에서 일관 포맷으로 변환합니다. `X-Request-Id`를 로깅/응답 모두에 포함하세요.

## 페이징·정렬·필터링
- 페이징: `page`(0기반), `size`(기본 20, 최대 100).
- 정렬: `sort=field,asc` (복수 허용). 필터는 쿼리 파라미터 사용(예: `status=ACTIVE`).
- 응답 예시:
```
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 125,
  "totalPages": 7
}
```

## 인증·보안
- 인증: `Authorization: Bearer <JWT>` 권장. 세션 기반 시 CSRF 고려.
- 권한 부족 시 403, 인증 누락/만료 시 401.
- 민감정보는 응답에 포함 금지. 입력 검증과 레이트 리밋 헤더(`X-RateLimit-*`) 고려.

## 캐싱·멱등성
- 캐싱: 읽기 응답에 `ETag`/`Last-Modified` 제공, 조건부 요청 지원.
- 멱등성: PUT/DELETE는 멱등. 중요한 POST 작업은 `Idempotency-Key`(요청 헤더) 지원 고려.

## 문서화
- Swagger/OpenAPI는 최신 상태로 유지하고 예시·제약(최솟값/최댓값/패턴)을 포함합니다.
- 각 `type` 문제 URI는 가능하면 사람/머신이 읽을 수 있는 설명 페이지로 해석 가능하도록 제공합니다.
- API 변경 시 이 문서와 변경 로그를 함께 갱신합니다.

