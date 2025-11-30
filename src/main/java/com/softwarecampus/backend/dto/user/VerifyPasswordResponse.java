package com.softwarecampus.backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 비밀번호 확인 응답 DTO
 */
@Getter
@AllArgsConstructor
public class VerifyPasswordResponse {

    private boolean verified;
    private String message;

    public static VerifyPasswordResponse success() {
        return new VerifyPasswordResponse(true, "비밀번호가 확인되었습니다.");
    }

    public static VerifyPasswordResponse failure() {
        return new VerifyPasswordResponse(false, "현재 비밀번호가 일치하지 않습니다.");
    }
}
