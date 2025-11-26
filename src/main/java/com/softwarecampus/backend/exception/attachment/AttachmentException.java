package com.softwarecampus.backend.exception.attachment;

import lombok.Getter;

@Getter
public class AttachmentException extends RuntimeException {

    private final AttachmentErrorCode errorCode;

    public AttachmentException(AttachmentErrorCode errorCode) {
        super(errorCode.getDetails());
        this.errorCode = errorCode;
    }
    public AttachmentException(AttachmentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
