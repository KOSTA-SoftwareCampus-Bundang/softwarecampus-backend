/**
 * S3 파일 업로드 실패 예외
 */
package com.softwarecampus.backend.exception;

public class S3UploadException extends RuntimeException {

    /**
     * S3 업로드 실패 원인 분류
     */
    public enum FailureReason {
        /** 파일 크기 초과 */
        FILE_TOO_LARGE,
        /** 잘못된 파일 형식/Content-Type */
        INVALID_FILE_TYPE,
        /** 파일 검증 실패 (빈 파일, null 등) */
        VALIDATION_ERROR,
        /** AWS 인증/권한 오류 */
        AUTHENTICATION_ERROR,
        /** S3 리소스를 찾을 수 없음 */
        RESOURCE_NOT_FOUND,
        /** 네트워크 연결 오류 */
        NETWORK_ERROR,
        /** AWS SDK 오류 */
        AWS_SDK_ERROR,
        /** 기타 서버 내부 오류 */
        INTERNAL_ERROR
    }

    private final FailureReason reason;

    public S3UploadException(String message) {
        super(message);
        this.reason = FailureReason.INTERNAL_ERROR;
    }

    public S3UploadException(String message, Throwable cause) {
        super(message, cause);
        this.reason = determineReasonFromCause(cause);
    }

    public S3UploadException(String message, FailureReason reason) {
        super(message);
        this.reason = reason;
    }

    public S3UploadException(String message, Throwable cause, FailureReason reason) {
        super(message, cause);
        this.reason = reason;
    }

    public FailureReason getReason() {
        return reason;
    }

    /**
     * Cause로부터 실패 원인 추론
     */
    private static FailureReason determineReasonFromCause(Throwable cause) {
        if (cause == null) {
            return FailureReason.INTERNAL_ERROR;
        }

        String causeMessage = cause.getMessage();
        if (causeMessage == null) {
            return FailureReason.INTERNAL_ERROR;
        }

        // AWS SDK 에러 메시지 패턴 분석
        String lowerMessage = causeMessage.toLowerCase();
        
        if (lowerMessage.contains("nosuchkey") || lowerMessage.contains("not found")) {
            return FailureReason.RESOURCE_NOT_FOUND;
        }
        if (lowerMessage.contains("accessdenied") || lowerMessage.contains("forbidden")) {
            return FailureReason.AUTHENTICATION_ERROR;
        }
        if (lowerMessage.contains("invalid") && lowerMessage.contains("credentials")) {
            return FailureReason.AUTHENTICATION_ERROR;
        }
        if (lowerMessage.contains("timeout") || lowerMessage.contains("connection")) {
            return FailureReason.NETWORK_ERROR;
        }
        if (lowerMessage.contains("entitytoolarge") || lowerMessage.contains("size")) {
            return FailureReason.FILE_TOO_LARGE;
        }

        return FailureReason.AWS_SDK_ERROR;
    }
}

