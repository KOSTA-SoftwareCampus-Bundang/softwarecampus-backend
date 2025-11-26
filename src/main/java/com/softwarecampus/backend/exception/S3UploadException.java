/**
 * S3 파일 업로드 실패 예외
 */
package com.softwarecampus.backend.exception;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

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
     * AWS SDK 예외 타입을 우선 확인한 후, 메시지 기반 분류 수행
     */
    private static FailureReason determineReasonFromCause(Throwable cause) {
        if (cause == null) {
            return FailureReason.INTERNAL_ERROR;
        }

        // 1. AWS SDK 예외 타입 기반 분류 (instanceof 체크)
        if (cause instanceof NoSuchKeyException || cause instanceof NoSuchBucketException) {
            return FailureReason.RESOURCE_NOT_FOUND;
        }
        
        if (cause instanceof S3Exception) {
            S3Exception s3Exception = (S3Exception) cause;
            int statusCode = s3Exception.statusCode();
            
            // HTTP 상태 코드 기반 분류
            if (statusCode == 403) {
                return FailureReason.AUTHENTICATION_ERROR;
            }
            if (statusCode == 404) {
                return FailureReason.RESOURCE_NOT_FOUND;
            }
            if (statusCode == 413) {
                return FailureReason.FILE_TOO_LARGE;
            }
        }
        
        if (cause instanceof SdkClientException) {
            return FailureReason.NETWORK_ERROR;
        }

        // 2. 메시지 기반 분류 (구체적인 문구 사용)
        String causeMessage = cause.getMessage();
        if (causeMessage == null) {
            return FailureReason.INTERNAL_ERROR;
        }

        String lowerMessage = causeMessage.toLowerCase();
        
        // 리소스 없음
        if (lowerMessage.contains("no such key") || lowerMessage.contains("nosuchkey")) {
            return FailureReason.RESOURCE_NOT_FOUND;
        }
        
        // 인증/권한 오류
        if (lowerMessage.contains("access denied") || 
            lowerMessage.contains("accessdenied") ||
            lowerMessage.contains("forbidden") ||
            lowerMessage.contains("invalid credentials")) {
            return FailureReason.AUTHENTICATION_ERROR;
        }
        
        // 네트워크 오류
        if (lowerMessage.contains("timed out") || 
            lowerMessage.contains("connection refused") ||
            lowerMessage.contains("connection failed")) {
            return FailureReason.NETWORK_ERROR;
        }
        
        // 파일 크기 초과
        if (lowerMessage.contains("entity too large") || 
            lowerMessage.contains("entitytoolarge") ||
            lowerMessage.contains("file size") ||
            lowerMessage.contains("maximum size exceeded")) {
            return FailureReason.FILE_TOO_LARGE;
        }

        return FailureReason.AWS_SDK_ERROR;
    }
}

