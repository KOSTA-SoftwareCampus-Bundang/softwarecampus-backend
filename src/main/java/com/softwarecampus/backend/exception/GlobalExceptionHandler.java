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
 * 전역 예외 처리기 (임시 구현)
 * 
 * ⚠️ 주의: 이 클래스는 Account 도메인 개발을 위한 임시 구현입니다.
 * - 프로젝트 전체 예외 처리 기준이 아님
 * - 다른 도메인 담당자와 협의하여 통합 예외 처리 구조로 개선 필요
 * - RFC 9457 Problem Details 형식 채택 (추후 팀 논의 필요)
 * 
 * RFC 9457 Problem Details 형식으로 응답
 * Phase 2: 기본 틀만 구현 (Bean Validation, fallback)
 * Phase 5: 도메인 예외 추가 예정
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 처리 (@Valid)
     * 
     * TODO: 전체 프로젝트 표준 정해지면 수정 필요
     * - 에러 메시지 형식
     * - 응답 구조
     * - HTTP 상태 코드 정책
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "요청 본문에 유효하지 않은 필드가 있습니다."
        );
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/validation-error")); //나중에 실제 도메인으로 교체할 예정
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
     * 
     * TODO: 프로덕션 환경에서는 상세 에러 정보 노출 금지
     * - 로깅 전략 수립 필요
     * - 에러 추적 시스템 연동 검토
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
