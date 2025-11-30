package com.softwarecampus.backend.exception;

import com.softwarecampus.backend.exception.course.BadRequestException;
import com.softwarecampus.backend.exception.course.NotFoundException;
import com.softwarecampus.backend.exception.email.EmailSendException;
import com.softwarecampus.backend.exception.email.EmailVerificationException;
import com.softwarecampus.backend.exception.email.EmailNotVerifiedException;
import com.softwarecampus.backend.exception.email.VerificationCodeExpiredException;
import com.softwarecampus.backend.exception.email.TooManyAttemptsException;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.exception.user.InvalidCredentialsException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.exception.user.InvalidPasswordException;
import com.softwarecampus.backend.exception.user.PhoneNumberAlreadyExistsException;
import com.softwarecampus.backend.exception.user.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 프로젝트 전역 예외 처리기
 * <p>
 * RFC 9457 Problem Details 형식으로 응답
 * - 프로젝트 전체 공통 예외 처리 기준
 * - 도메인별 예외는 각 담당자가 추가
 *
 * @author 태윤 (Account 도메인)
 *         <p>
 *         * 📌 체크리스트:
 *         - [ ] 예외 클래스 생성 (exception/{domain}/ 패키지)
 *         - [ ] 적절한 HTTP 상태 코드 선택 (400/404/409/422/500 등)
 *         - [ ] type URI 정의 (problems/{problem-type})
 *         - [ ] 로깅 레벨 결정 (ERROR/WARN/DEBUG)
 *         - [ ] 민감정보 포함 여부 확인
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
                "요청 본문에 유효하지 않은 필드가 있습니다.");
        problemDetail.setType(URI.create(problemBaseUri + "/validation-error"));
        problemDetail.setTitle("Validation Failed");

        // 필드별 오류 수집
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    /**
     * 필수 요청 파라미터 누락 처리 (@RequestParam required=true)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing required request parameter: {}", ex.getParameterName());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                String.format("필수 파라미터 '%s'가 누락되었습니다.", ex.getParameterName()));
        problemDetail.setType(URI.create(problemBaseUri + "/missing-parameter"));
        problemDetail.setTitle("Missing Required Parameter");

        return problemDetail;
    }

    /**
     * 필수 멀티파트 파일 누락 처리 (multipart/form-data의 file part)
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingFilePart(MissingServletRequestPartException ex) {
        log.warn("Missing required file part: {}", ex.getRequestPartName());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                String.format("필수 파일 '%s'가 누락되었습니다.", ex.getRequestPartName()));
        problemDetail.setType(URI.create(problemBaseUri + "/missing-file"));
        problemDetail.setTitle("Missing Required File");

        return problemDetail;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "API 엔드포인트 경로가 잘못되었습니다");

        problemDetail.setType(URI.create(problemBaseUri + "/not-found"));
        problemDetail.setTitle("Api EndPoint Not Found");

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
                detailMessage);
        problemDetail.setType(URI.create(problemBaseUri + "/validation-error"));
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
                "서버 내부 오류가 발생했습니다.");
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
                ex.getMessage() // 이미 일반화된 메시지 사용
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
                "이메일이 이미 등록되었습니다.");
        problemDetail.setType(URI.create(problemBaseUri + "/duplicate-email"));
        problemDetail.setTitle("Duplicate Email");

        return problemDetail;
    }

    /**
     * 전화번호 중복 예외 처리
     * HTTP 409 Conflict
     */
    @ExceptionHandler(PhoneNumberAlreadyExistsException.class)
    public ProblemDetail handlePhoneNumberAlreadyExistsException(PhoneNumberAlreadyExistsException ex) {
        log.warn("Phone number duplicate detected for a request");
        if (log.isDebugEnabled()) {
            log.debug("PhoneNumberAlreadyExistsException details", ex);
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "이미 사용 중인 전화번호입니다.");
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/duplicate-phone-number"));
        problemDetail.setTitle("Duplicate Phone Number");

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
                "요청한 계정을 찾을 수 없습니다.");
        problemDetail.setType(URI.create(problemBaseUri + "/account-not-found"));
        problemDetail.setTitle("Account Not Found");

        return problemDetail;
    }

    /**
     * 로그인 인증 실패 예외 처리
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("인증 실패: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage());

        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/invalid-credentials"));
        problemDetail.setTitle("Unauthorized");

        return problemDetail;
    }

    /**
     * 현재 비밀번호 불일치 예외 처리
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPasswordException(InvalidPasswordException ex) {
        log.warn("비밀번호 검증 실패: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());

        problemDetail.setType(URI.create(problemBaseUri + "/invalid-password"));
        problemDetail.setTitle("Invalid Password");
     * 인증되지 않은 사용자 접근 예외 처리
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("인증되지 않은 사용자 접근: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage());

        problemDetail.setType(URI.create(problemBaseUri + "/unauthorized"));
        problemDetail.setTitle("Unauthorized");

        return problemDetail;
    }

    /**
     * 권한 부족 예외 처리
     * HTTP 403 Forbidden
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("접근 권한 부족: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "접근 권한이 없습니다.");
        problemDetail.setType(URI.create(problemBaseUri + "/access-denied"));
        problemDetail.setTitle("Access Denied");

        return problemDetail;
    }

    // ========================================
    // 여기에 다른 도메인 예외 추가
    // ========================================

    // ========================================
    // Email 도메인 예외 처리
    // ========================================

    /**
     * 이메일 발송 실패 예외
     * HTTP 500 Internal Server Error
     */
    @ExceptionHandler(EmailSendException.class)
    public ProblemDetail handleEmailSendException(EmailSendException ex) {
        log.error("이메일 발송 실패", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "이메일 발송에 실패했습니다.");
        problemDetail.setType(URI.create(problemBaseUri + "/email-send-failed"));
        problemDetail.setTitle("Email Send Failed");

        return problemDetail;
    }

    /**
     * 이메일 인증 예외 (일반)
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(EmailVerificationException.class)
    public ProblemDetail handleEmailVerificationException(EmailVerificationException ex) {
        log.warn("이메일 인증 예외: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        problemDetail.setType(URI.create(problemBaseUri + "/email-verification-error"));
        problemDetail.setTitle("Email Verification Error");

        return problemDetail;
    }

    /**
     * 이메일 미인증 예외
     * HTTP 403 Forbidden
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ProblemDetail handleEmailNotVerifiedException(EmailNotVerifiedException ex) {
        log.warn("이메일 미인증: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage());
        problemDetail.setType(URI.create(problemBaseUri + "/email-not-verified"));
        problemDetail.setTitle("Email Not Verified");

        return problemDetail;
    }

    /**
     * 인증 코드 만료 예외
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(VerificationCodeExpiredException.class)
    public ProblemDetail handleVerificationCodeExpiredException(VerificationCodeExpiredException ex) {
        log.warn("인증 코드 만료: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        problemDetail.setType(URI.create(problemBaseUri + "/verification-code-expired"));
        problemDetail.setTitle("Verification Code Expired");

        return problemDetail;
    }

    /**
     * 인증 시도 횟수 초과 예외
     * HTTP 429 Too Many Requests
     */
    @ExceptionHandler(TooManyAttemptsException.class)
    public ProblemDetail handleTooManyAttemptsException(TooManyAttemptsException ex) {
        log.warn("인증 시도 횟수 초과: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage());
        problemDetail.setType(URI.create(problemBaseUri + "/too-many-attempts"));
        problemDetail.setTitle("Too Many Attempts");

        // 차단 해제 시간 추가 (ISO-8601 형식으로 통일)
        if (ex.getBlockedUntil() != null) {
            problemDetail.setProperty("blockedUntil",
                    ex.getBlockedUntil().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        return problemDetail;
    }

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
                message = ex.getMessage(); // 원본 메시지에 구체적인 크기 정보 포함
                type = problemBaseUri + "/file-too-large";
                title = "File Too Large";
                log.warn("S3 upload failed - File too large: {}", ex.getMessage());
                break;

            case INVALID_FILE_TYPE:
                status = HttpStatus.UNSUPPORTED_MEDIA_TYPE; // 415
                message = ex.getMessage(); // 원본 메시지에 허용된 형식 정보 포함
                type = problemBaseUri + "/invalid-file-type";
                title = "Invalid File Type";
                log.warn("S3 upload failed - Invalid file type: {}", ex.getMessage());
                break;

            case VALIDATION_ERROR:
                status = HttpStatus.BAD_REQUEST; // 400
                message = ex.getMessage(); // 원본 메시지에 구체적인 검증 실패 이유 포함
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
     * 리소스 찾기 실패 처리
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
     * Academy 도메인 예외 처리
     */
    @ExceptionHandler(com.softwarecampus.backend.exception.academy.AcademyException.class)
    public ProblemDetail handleAcademyException(com.softwarecampus.backend.exception.academy.AcademyException ex) {
        log.warn("Academy exception occurred: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                ex.getErrorCode().getStatus(),
                ex.getMessage());
        problemDetail.setType(URI.create(problemBaseUri + "/academy-error"));
        problemDetail.setTitle(ex.getErrorCode().name());
        problemDetail.setProperty("code", ex.getErrorCode().getCode());

        return problemDetail;
    }

    /**
     * Course 도메인 - NotFoundException 처리
     */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleCourseNotFoundException(NotFoundException ex) {
        log.warn("Course resource not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create(problemBaseUri + "/course-not-found"));
        problemDetail.setTitle("Course Resource Not Found");

        return problemDetail;
    }

    /**
     * Course 도메인 - BadRequestException 처리
     */
    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleCourseBadRequestException(BadRequestException ex) {
        log.warn("Course bad request: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setType(URI.create(problemBaseUri + "/course-bad-request"));
        problemDetail.setTitle("Invalid Course Request");

        return problemDetail;
    }

    /**
     * JPA Entity 미존재 예외 처리
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage());
        problemDetail.setType(URI.create(problemBaseUri + "/entity-not-found"));
        problemDetail.setTitle("Entity Not Found");

        return problemDetail;
    }

    /**
     * 잘못된 요청/비즈니스 로직 위반 처리
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
