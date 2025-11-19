package com.softwarecampus.backend.exception;

import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 프로젝트 전역 예외 처리기
 * 
 * RFC 9457 Problem Details 형식으로 응답
 * - 프로젝트 전체 공통 예외 처리 기준
 * - 도메인별 예외는 각 담당자가 추가
 * 
 * @author 태윤 (Account 도메인)
 * 
 * * 📌 체크리스트:
     * - [ ] 예외 클래스 생성 (exception/{domain}/ 패키지)
     * - [ ] 적절한 HTTP 상태 코드 선택 (400/404/409/422/500 등)
     * - [ ] type URI 정의 (problems/{problem-type})
     * - [ ] 로깅 레벨 결정 (ERROR/WARN/DEBUG)
     * - [ ] 민감정보 포함 여부 확인
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
     * Request Parameter/Path Variable Validation 실패 처리
     * (@RequestParam, @PathVariable에 @Email, @NotNull 등 사용 시)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
        if (log.isDebugEnabled()) {
            log.debug("Constraint violation detected for request parameters");
        }
        
        // 파라미터별 오류 수집
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String parameterName = violation.getPropertyPath().toString();
            errors.put(parameterName, violation.getMessage());
        });
        
        // 단일 오류인 경우 해당 메시지를 detail에 직접 표시
        String detailMessage = errors.size() == 1 
            ? errors.values().iterator().next()
            : "요청 파라미터가 유효하지 않습니다.";
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            detailMessage
        );
        problemDetail.setType(URI.create("https://api.프로젝트주소/problems/validation-error"));
        problemDetail.setTitle("Validation Failed");
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
    // Account 도메인 예외 처리
    // ========================================
    
    /**
     * 잘못된 입력값 예외 처리
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(InvalidInputException.class)
    public ProblemDetail handleInvalidInputException(InvalidInputException ex) {
        log.warn("Invalid input detected for a request");
        if (log.isDebugEnabled()) {
            log.debug("InvalidInputException details", ex);
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()  // 이미 일반화된 메시지 사용
        );
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/invalid-input"));
        problemDetail.setTitle("Invalid Input");
        
        return problemDetail;
    }
    
    /**
     * 이메일 중복 예외 처리
     * HTTP 409 Conflict
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmailException(DuplicateEmailException ex) {
        log.warn("Email duplicate detected for a request");
        if (log.isDebugEnabled()) {
            log.debug("DuplicateEmailException details", ex);
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "이메일이 이미 등록되었습니다."
        );
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/duplicate-email"));
        problemDetail.setTitle("Duplicate Email");
        
        return problemDetail;
    }
    
    /**
     * 계정 미존재 예외 처리
     * HTTP 404 Not Found
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFoundException(AccountNotFoundException ex) {
        log.warn("Account not found for a request");
        if (log.isDebugEnabled()) {
            log.debug("AccountNotFoundException details", ex);
        }
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "요청한 계정을 찾을 수 없습니다."
        );
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/account-not-found"));
        problemDetail.setTitle("Account Not Found");
        
        return problemDetail;
    }
    
    // ========================================
    // 여기에 다른 도메인 예외 추가
    // ========================================

    /**
     *  리소스 찾기 실패 처리
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNotFoundException(NoSuchElementException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "요청한 리소스를 찾을 수 없습니다." // 구체적인 메시지는 보안상 일반화
        );
        problemDetail.setType(URI.create("https://api.프로젝트주소/problems/resource-not-found"));
        problemDetail.setTitle("Resource Not Found");

        // 디버깅을 위해 에러 메시지를 detail에 남길 수도 있지만, 여기서는 일반화합니다.
        // problemDetail.setProperty("reason", ex.getMessage());

        return problemDetail;
    }

    /**
     *  잘못된 요청/비즈니스 로직 위반 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequestException(IllegalArgumentException ex) {
        log.warn("Bad Request or Business Rule Violation: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() // 비즈니스 로직 위반 메시지를 그대로 전달
        );
        problemDetail.setType(URI.create("https://api.프로젝트주소/problems/invalid-argument"));
        problemDetail.setTitle("Invalid Request Argument");

        return problemDetail;
    }

}
