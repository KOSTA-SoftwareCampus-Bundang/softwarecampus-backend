package com.softwarecampus.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 재설정 요청 DTO (이메일 인증 코드 기반)
 * 
 * 사용 시나리오:
 * 1. 사용자가 이메일 인증 코드를 받음 (POST /api/auth/email/send-reset-code)
 * 2. 코드와 새 비밀번호를 함께 전송하여 비밀번호 재설정 (PUT /api/mypage/password)
 * 
 * 참고:
 * - confirmPassword 검증은 프론트엔드에서 처리
 * - 백엔드는 code + newPassword만 받아서 처리
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    
    @NotBlank(message = "인증 코드를 입력해주세요")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다")
    private String code;
    
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>/?~])[A-Za-z\\d!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>/?~]{8,20}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    private String newPassword;
}
