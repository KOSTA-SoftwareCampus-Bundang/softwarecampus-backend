# Phase 2: GlobalExceptionHandler 기본 틀 ✅ (완료)

**작업 기간:** 2025-10-29  
**상태:** ✅ 완료

---

## 📌 작업 목표
- RFC 9457 Problem Details 구조 확립
- Spring 기본 예외 처리 (Validation)
- 도메인 예외는 나중에 추가 (주석으로 표시)

---

## 📂 생성된 파일

```
exception/
  ├─ GlobalExceptionHandler.java    # @RestControllerAdvice
  └─ (도메인 예외는 Phase 5에서 추가)
```

---

## 🔨 구현 내용

### `GlobalExceptionHandler.java`

```java
package com.softwarecampus.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 프로젝트 전역 예외 처리기
 * 
 * RFC 9457 Problem Details 형식으로 응답
 * - 프로젝트 전체 공통 예외 처리 기준
 * - 도메인별 예외는 각 담당자가 추가
 * 
 * @author 태윤 (Account 도메인)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 처리 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        if (log.isDebugEnabled()) {
            var failedFields = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField())
                .toList();
            log.debug("Validation failed. Fields: {}", failedFields);
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "요청 본문에 유효하지 않은 필드가 있습니다."
        );
        problemDetail.setType(URI.create("https://api.프로젝트주소/problems/validation-error"));
        problemDetail.setTitle("Validation Failed");
        
        // 필드별 오류 수집
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        problemDetail.setProperty("errors", errors);
        
        return problemDetail;
    }

    /**
     * 일반 예외 처리 (fallback)
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다."
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Internal Server Error");
        
        return problemDetail;
    }

    // ========================================
    // Account 도메인 예외 처리 (Phase 5에서 추가 예정)
    // ========================================
    
    // ========================================
    // 여기에 다른 도메인 예외 추가
    // ========================================
}
```

---

## ✅ 검증 및 비고

### 구현 완료 사항
- ✅ `GlobalExceptionHandler.java` 적용됨
- ✅ **로깅**: Lombok `@Slf4j`로 예외 로그 출력(운영 모니터링용으로 error 레벨 기록)
- ✅ **Validation 핸들러**: 필드명만 로깅하며(개인정보 비노출), ProblemDetail의 `errors` 프로퍼티로 상세 필드 에러 제공
- ✅ **템플릿 간소화**: 도메인별 예외는 각 도메인 담당자가 `exception/{domain}`에 추가하도록 안내
- ✅ **type URI**: `https://api.프로젝트주소/problems/{problem-type}` 형식 사용

### 검증 방법
- 임시 Controller 생성하여 Validation 오류 발생 테스트
- RFC 9457 형식으로 응답 확인
- `mvn -q clean compile` 및 관련 단위/통합 테스트로 동작 검증

---

## 📝 Phase 5에서 추가할 도메인 예외 (주석 템플릿)

```java
// /**
//  * 이메일 중복 예외 처리
//  */
// @ExceptionHandler(DuplicateEmailException.class)
// public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
//     ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
//         HttpStatus.CONFLICT,
//         ex.getMessage()
//     );
//     problemDetail.setType(URI.create("https://api.프로젝트주소/problems/duplicate-email"));
//     problemDetail.setTitle("Duplicate Email");
//     return problemDetail;
// }

// /**
//  * 계정 없음 예외 처리
//  */
// @ExceptionHandler(AccountNotFoundException.class)
// public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
//     ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
//         HttpStatus.NOT_FOUND,
//         ex.getMessage()
//     );
//     problemDetail.setType(URI.create("https://api.프로젝트주소/problems/account-not-found"));
//     problemDetail.setTitle("Account Not Found");
//     return problemDetail;
// }
```

---

## 🔜 다음 단계
Phase 3: 기본 보안 설정 (PasswordEncoder)
