package com.softwarecampus.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현재 비밀번호 검증 요청 DTO
 * - 비밀번호 변경 전 본인 확인용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPasswordRequest {
    
    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    private String currentPassword;
}
