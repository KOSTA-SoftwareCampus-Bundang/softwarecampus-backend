package com.softwarecampus.backend.exception.academy;

import lombok.Getter;

@Getter
public class AcademyException extends RuntimeException {

    private final AcademyErrorCode errorCode;
    public AcademyException(AcademyErrorCode errorCode) {

      this.errorCode = errorCode;
    }
}
