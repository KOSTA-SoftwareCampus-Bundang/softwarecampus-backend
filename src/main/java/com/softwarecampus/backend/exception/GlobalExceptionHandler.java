package com.softwarecampus.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기
 * RFC 9457 Problem Details 형식으로 응답
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 처리 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "요청 본문에 유효하지 않은 필드가 있습니다."
        );
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/validation-error"));
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
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다."
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Internal Server Error");
        
        return problemDetail;
    }

    // Phase 5에서 추가할 도메인 예외들 (주석으로 표시)
    
    // /**
    //  * 이메일 중복 예외 처리
    //  */
    // @ExceptionHandler(DuplicateEmailException.class)
    // public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
    //     ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
    //         HttpStatus.CONFLICT,
    //         ex.getMessage()
    //     );
    //     problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/duplicate-email"));
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
    //     problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/account-not-found"));
    //     problemDetail.setTitle("Account Not Found");
    //     return problemDetail;
    // }
}
