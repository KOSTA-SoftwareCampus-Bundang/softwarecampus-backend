# 파일 업로드/삭제 모듈 아키텍처 및 보안 설계

## 1. 개요
본 문서는 `FileController`를 중심으로 한 파일 업로드 및 삭제 기능의 최종 구현 아키텍처와 보안 설계를 기술합니다.
Spring Security 6.x 기반의 보안 설정과 `GlobalExceptionHandler`를 통한 예외 처리 전략이 적용되었습니다.

## 2. 아키텍처 개요

### 2.1 구성 요소
- **Controller**: `FileController` - 파일 업로드/삭제 요청 처리 및 입력값 검증
- **Service**: `S3Service` - AWS S3와의 통신 담당
- **Security**: 
    - `SecurityConfig` - URL 기반 접근 제어 및 필터 체인 설정
    - `JwtAuthenticationFilter` - JWT 토큰 검증 및 인증 객체 생성
- **Exception Handling**: `GlobalExceptionHandler` - 전역 예외 처리 및 표준화된 에러 응답(`ProblemDetail`) 생성

### 2.2 데이터 흐름
1. **요청 진입**: 클라이언트가 `/api/files/upload` (POST) 또는 `/api/admin/files/delete` (DELETE) 요청
2. **보안 필터링**: `SecurityConfig`의 `filterChain`에서 인증 여부 및 권한(Role) 확인
    - `/api/files/**`: 인증된 사용자(`authenticated`) 접근 가능
    - `/api/admin/**`: 관리자(`ADMIN`) 권한 필요
3. **컨트롤러 진입**: `FileController` 메서드 실행
4. **유효성 검증**: 파일명, 폴더명, 파일 크기 등에 대한 1차 검증 수행
5. **서비스 호출**: `S3Service`를 통해 S3 작업 수행
6. **응답 반환**: 성공 시 JSON 응답, 실패 시 예외 발생 -> `GlobalExceptionHandler` 처리

## 3. 주요 보안 설계

### 3.1 인증 및 인가 (Authentication & Authorization)
- **전략**: "Secure by Default" 원칙 적용
- **구현**:
    - `SecurityConfig`에서 공개 리소스(GET 요청 일부)를 제외한 **모든 요청에 대해 인증(`authenticated`)을 요구**하도록 설정했습니다.
    - 관리자 전용 기능은 `/api/admin/**` 경로 패턴으로 분리하고 `hasRole('ADMIN')`으로 접근을 제한했습니다.
    - `JwtAuthenticationEntryPoint`를 통해 인증되지 않은 접근 시 `401 Unauthorized`를 명확히 반환합니다.

### 3.2 입력값 검증 (Input Validation)
- **Path Traversal 방지**:
    - **폴더명**: 상위 디렉토리 이동 패턴(`..`) 포함 시 즉시 차단 (`IllegalArgumentException`)
    - **파일명**: `..` 패턴은 허용하되(일부 파일명에 사용됨), 경로 제어 문자(`/`, `\`) 및 위험 문자(`<`, `>`, `:`, `"`, `|`, `?`, `*`) 포함 시 차단
- **파일 검증**:
    - 빈 파일(`isEmpty()`) 업로드 차단
    - 파일명 `null` 체크

### 3.3 예외 처리 (Exception Handling)
- **표준화된 응답**: Spring 6의 `ProblemDetail` 객체(RFC 7807)를 사용하여 에러 응답을 표준화했습니다.
- **보안 예외 처리**:
    - `AccessDeniedException`: 인가 실패 시 `403 Forbidden` 반환 (기존 500 에러 문제 해결)
    - `AuthenticationException`: 인증 실패 시 `401 Unauthorized` 반환
- **비즈니스 예외 처리**:
    - `IllegalArgumentException`: 잘못된 입력값에 대해 `400 Bad Request` 및 구체적인 사유 반환
    - `S3UploadException`: S3 연동 실패 시 `500 Internal Server Error` 반환

## 4. 테스트 전략 (`FileControllerTest`)

### 4.1 테스트 환경
- **@WebMvcTest**: 컨트롤러 레이어만 격리하여 테스트 수행 (가볍고 빠른 실행)
- **Mocking**:
    - `S3Service`: 실제 S3 연동 없이 성공/실패 케이스 시뮬레이션
    - `JwtTokenProvider`, `UserDetailsService`: 보안 필터 동작을 위한 Mock Bean 주입
- **Security Integration**:
    - `@Import(SecurityConfig.class)` 대신 필요한 필터와 핸들러만 로드하거나, 테스트용 설정이 실제 보안 로직을 방해하지 않도록 구성
    - `JwtAuthenticationFilter`를 실제 객체로 로드하여 필터 체인 동작 검증

### 4.2 주요 테스트 케이스
- **권한 제어**: 관리자/일반 사용자/비로그인 사용자의 접근 권한 테스트 (200 vs 403 vs 401)
- **입력값 검증**: Path Traversal 시도, 빈 파일, 위험 문자 포함 시 400 에러 반환 검증
- **예외 매핑**: 서비스 계층에서 발생한 예외가 `GlobalExceptionHandler`를 통해 올바른 JSON 포맷으로 변환되는지 검증

## 5. 변경 전후 비교 (Comparison)

기존 구현과 개선된 구현의 주요 차이점은 **보안의 명확성**과 **테스트 신뢰성**에 있습니다.

| 구분 | 변경 전 (Before) | 변경 후 (After) | 개선 효과 |
|---|---|---|---|
| **SecurityConfig** | - `RegexRequestMatcher` 등 복잡한 매처 혼용<br>- 일부 경로에 대한 모호한 `permitAll` 설정 | - `requestMatchers`로 통일 및 단순화<br>- **Secure by Default**: 명시적 허용 외 모든 요청 인증 요구 | - 보안 설정의 가독성 향상<br>- 의도치 않은 보안 구멍 제거 |
| **FileController** | - `@PreAuthorize`에 의존했으나 테스트 환경과 불일치<br>- Path Traversal 검증 로직 미흡 (정상 파일명도 차단 가능성) | - URL 기반 보안(`SecurityConfig`)과 `@PreAuthorize`의 조화<br>- **정교한 검증 로직**: 폴더(`..` 차단)와 파일명(위험 문자 차단) 분리 검증 | - 계층별 보안 강화 (Filter + Controller)<br>- 정상적인 파일 업로드 허용 및 공격 방어 |
| **GlobalExceptionHandler** | - `AccessDeniedException` 미처리 (500 에러 발생)<br>- 예외 메시지가 클라이언트에 불친절 | - **AccessDeniedException 핸들러 추가** (403 Forbidden 반환)<br>- `ProblemDetail`을 통한 표준화된 에러 응답 | - 클라이언트가 권한 문제와 서버 오류를 명확히 구분 가능<br>- API 사용성 향상 |
| **FileControllerTest** | - Security Context 설정 미흡으로 403 상황에서 200 OK 반환 (False Negative)<br>- Mocking 범위가 불명확 | - **실제 Security Filter Chain 적용** (`@Import`)<br>- `WithMockUser`와 실제 필터의 조화로 정확한 권한 테스트 수행 | - 테스트 결과의 신뢰성 확보<br>- 배포 전 보안 결함 조기 발견 |

### 5.1 주요 개선 사례 상세

#### A. 권한 없는 삭제 요청 처리
- **Before**: 일반 사용자가 `/api/admin/files/delete` 요청 시, 컨트롤러 진입 전 필터에서 막히더라도 적절한 응답 핸들러가 없어 `500 Internal Server Error`가 발생하거나, 테스트에서는 필터가 동작하지 않아 `200 OK`가 반환됨.
- **After**: `SecurityConfig`에서 `hasRole('ADMIN')`으로 차단하고, `GlobalExceptionHandler`가 `AccessDeniedException`을 잡아 `403 Forbidden`과 함께 "접근 권한이 없습니다." 메시지 반환. 테스트에서도 이를 정확히 검증.

#### B. Path Traversal 방어
- **Before**: `originalFilename.contains("..")` 로직이 `file..name.jpg`와 같은 정상적인 파일명까지 차단할 위험이 있었음.
- **After**: 폴더명은 `..`을 엄격히 차단하되, 파일명은 경로 제어 문자(`/`, `\`)와 위험 특수문자만 선별적으로 차단하여 보안과 사용성의 균형을 맞춤.

## 6. 결론
현재 구현은 **보안성**, **안정성**, **유지보수성**을 고려하여 설계되었습니다. 특히 보안 설정의 모호함을 제거하고, 예외 상황에 대한 명확한 피드백을 제공함으로써 클라이언트와의 연동 효율성을 높였습니다.
