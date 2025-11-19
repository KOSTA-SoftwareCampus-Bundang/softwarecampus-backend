/**
 * S3 파일 업로드 실패 예외
 */
package com.softwarecampus.backend.exception;

public class S3UploadException extends RuntimeException {

    public S3UploadException(String message) {
        super(message);
    }

    public S3UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}

