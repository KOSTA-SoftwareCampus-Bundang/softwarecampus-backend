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
