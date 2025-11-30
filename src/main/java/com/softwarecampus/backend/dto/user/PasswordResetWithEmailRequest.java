package com.softwarecampus.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 요청 DTO (비로그인용 - 이메일 포함)
 * 
 * 사용 시나리오 (비밀번호 찾기):
 * 1. 사용자가 이메일 입력 후 인증 코드 발송 (POST /api/auth/email/send-reset-code)
 * 2. 이메일로 받은 인증 코드 확인 (POST /api/auth/email/verify-reset)
 * 3. 이 DTO로 비밀번호 재설정 (POST /api/auth/reset-password)
 * 
 * @param email       계정 이메일
 * @param code        이메일로 받은 6자리 인증 코드
 * @param newPassword 새 비밀번호 (8~20자, 영문+숫자+특수문자)
 */
public record PasswordResetWithEmailRequest(
        @NotBlank(message = "이메일을 입력해주세요") @Email(message = "올바른 이메일 형식이 아닙니다") String email,

        @NotBlank(message = "인증 코드를 입력해주세요") @Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다") @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다") String code,

        @NotBlank(message = "새 비밀번호를 입력해주세요") @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>/?~])[A-Za-z\\d!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>/?~]{8,20}$", message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다") String newPassword) {
}
