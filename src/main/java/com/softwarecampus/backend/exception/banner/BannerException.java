package com.softwarecampus.backend.exception.banner;

import lombok.Getter;

@Getter
public class BannerException extends RuntimeException {

    private final BannerErrorCode errorCode;
    public BannerException(BannerErrorCode errorCode)
    {
        this.errorCode = errorCode;
    }
}
