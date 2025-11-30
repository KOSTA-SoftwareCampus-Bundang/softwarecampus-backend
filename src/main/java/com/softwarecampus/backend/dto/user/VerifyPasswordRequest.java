package com.softwarecampus.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현재 비밀번호 확인 요청 DTO
 * 
 * 사용 시나리오:
 * - 마이페이지 비밀번호 변경 Step 1: 현재 비밀번호 확인
 * - POST /api/auth/verify-password
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPasswordRequest {

    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    private String currentPassword;
}
