package com.softwarecampus.backend.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO
 * 
 * @param email 이메일 (필수, 이메일 형식)
 * @param password 비밀번호 (필수, 8~20자, 영문+숫자+특수문자)
 * @param userName 사용자명 (필수, 2~50자)
 * @param phoneNumber 전화번호 (필수, 휴대폰 형식)
 * @param address 주소 (선택)
 * @param affiliation 소속 (선택)
 * @param position 직책 (선택)
 */
public record SignupRequest(
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
    )    String password,
    
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 50, message = "사용자명은 2~50자여야 합니다")
    String userName,
    
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(
        regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
        message = "올바른 휴대폰 번호 형식이 아닙니다 (예: 010-1234-5678)"
    )
    String phoneNumber,
    
    String address,
    String affiliation,
    String position
) {
}
