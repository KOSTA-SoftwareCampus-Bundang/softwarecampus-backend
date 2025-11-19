package com.softwarecampus.backend.exception;

import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${problem.base-uri}")
    private String problemBaseUri;

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
        problemDetail.setType(URI.create(problemBaseUri + "/validation-error"));
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
        
        // 첫 번째 위반 메시지 추출
        String detail = ex.getConstraintViolations().stream()
            .findFirst()
            .map(violation -> violation.getMessage())
            .orElse("요청 파라미터가 유효하지 않습니다.");
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            detail
        );
        problemDetail.setType(URI.create(problemBaseUri + "/validation-error"));
        problemDetail.setTitle("Validation Failed");
        
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
        problemDetail.setType(URI.create(problemBaseUri + "/invalid-input"));
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
        problemDetail.setType(URI.create(problemBaseUri + "/duplicate-email"));
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
        problemDetail.setType(URI.create(problemBaseUri + "/account-not-found"));
        problemDetail.setTitle("Account Not Found");
        
        return problemDetail;
    }
    
    // ========================================
    // 여기에 다른 도메인 예외 추가
    // ========================================

    /**
     * S3 파일 업로드 실패 예외 처리
     * FailureReason에 따라 적절한 HTTP 상태 코드 및 로깅 레벨 매핑
     */
    @ExceptionHandler(S3UploadException.class)
    public ProblemDetail handleS3UploadException(S3UploadException ex) {
        S3UploadException.FailureReason reason = ex.getReason();
        HttpStatus status;
        String message;
        String type;
        String title;

        // FailureReason에 따른 HTTP 상태 코드 및 메시지 매핑
        switch (reason) {
            case FILE_TOO_LARGE:
                status = HttpStatus.PAYLOAD_TOO_LARGE; // 413
                message = "파일 크기가 너무 큽니다. 최대 허용 크기를 초과했습니다.";
                type = problemBaseUri + "/file-too-large";
                title = "File Too Large";
                log.warn("S3 upload failed - File too large: {}", ex.getMessage());
                break;

            case INVALID_FILE_TYPE:
                status = HttpStatus.UNSUPPORTED_MEDIA_TYPE; // 415
                message = "지원하지 않는 파일 형식입니다.";
                type = problemBaseUri + "/invalid-file-type";
                title = "Invalid File Type";
                log.warn("S3 upload failed - Invalid file type: {}", ex.getMessage());
                break;

            case VALIDATION_ERROR:
                status = HttpStatus.BAD_REQUEST; // 400
                message = "파일 검증에 실패했습니다. 파일이 비어있거나 유효하지 않습니다.";
                type = problemBaseUri + "/file-validation-error";
                title = "File Validation Error";
                log.warn("S3 upload failed - Validation error: {}", ex.getMessage());
                break;

            case AUTHENTICATION_ERROR:
                status = HttpStatus.FORBIDDEN; // 403
                message = "파일 저장소 접근 권한이 없습니다.";
                type = problemBaseUri + "/s3-access-denied";
                title = "Access Denied";
                log.error("S3 upload failed - Authentication/Permission error: {}", ex.getMessage(), ex);
                break;

            case RESOURCE_NOT_FOUND:
                status = HttpStatus.NOT_FOUND; // 404
                message = "파일 저장소를 찾을 수 없습니다.";
                type = problemBaseUri + "/s3-resource-not-found";
                title = "Resource Not Found";
                log.error("S3 upload failed - Resource not found: {}", ex.getMessage(), ex);
                break;

            case NETWORK_ERROR:
                status = HttpStatus.BAD_GATEWAY; // 502
                message = "파일 저장소와의 연결에 실패했습니다. 잠시 후 다시 시도해주세요.";
                type = problemBaseUri + "/s3-network-error";
                title = "Network Error";
                log.error("S3 upload failed - Network error: {}", ex.getMessage(), ex);
                break;

            case AWS_SDK_ERROR:
                status = HttpStatus.SERVICE_UNAVAILABLE; // 503
                message = "파일 저장 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.";
                type = problemBaseUri + "/s3-service-unavailable";
                title = "Service Unavailable";
                log.error("S3 upload failed - AWS SDK error: {}", ex.getMessage(), ex);
                break;

            case INTERNAL_ERROR:
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR; // 500
                message = "파일 업로드 중 서버 오류가 발생했습니다.";
                type = problemBaseUri + "/s3-internal-error";
                title = "Internal Server Error";
                log.error("S3 upload failed - Internal error: {}", ex.getMessage(), ex);
                break;
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setType(URI.create(type));
        problemDetail.setTitle(title);
        problemDetail.setProperty("reason", reason.name());

        return problemDetail;
    }

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
        problemDetail.setType(URI.create(problemBaseUri + "/resource-not-found"));
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
        problemDetail.setType(URI.create(problemBaseUri + "/invalid-argument"));
        problemDetail.setTitle("Invalid Request Argument");

        return problemDetail;
    }

}
