package com.softwarecampus.backend.exception.attachment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AttachmentErrorCode {

    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 2001, "요청한 첨부파일을 찾을 수 없습니다."),
    INVALID_CATEGORY_TYPE(HttpStatus.BAD_REQUEST, 2002, "첨부파일의 카테고리 타입이 올바르지 않습니다."),
    ALREADY_ACTIVE(HttpStatus.BAD_REQUEST, 2003, "첨부파일이 이미 활성 상태이므로 복구할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final int errorCode;
    private final String details;
}
