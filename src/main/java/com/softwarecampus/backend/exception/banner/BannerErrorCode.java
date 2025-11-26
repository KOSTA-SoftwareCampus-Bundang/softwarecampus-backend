package com.softwarecampus.backend.exception.banner;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BannerErrorCode {

    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, 4001, "요청한 배너를 찾을 수 없습니다."),
    BANNER_NOT_ACTIVE(HttpStatus.BAD_REQUEST, 4002, "배너가 활성 상태가 아닙니다."),
    BANNER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, 4003, "배너가 이미 삭제된 상태입니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
