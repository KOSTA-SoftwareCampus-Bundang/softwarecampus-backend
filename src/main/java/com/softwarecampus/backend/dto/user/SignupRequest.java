package com.softwarecampus.backend.dto.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.validation.ValidAccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * @param accountType 계정 타입 (필수, USER/ACADEMY/ADMIN)
 * @param academyId 기관 ID (ACADEMY 타입일 때 필수)
 */
@ValidAccountType
public record SignupRequest(
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>/?~])[A-Za-z\\d!@#$%^&*()\\-_=+\\[\\]{}|;:'\",.<>/?~]{8,20}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
    ) String password,
    
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
    String position,
    
    @NotNull(message = "계정 타입은 필수입니다")
    AccountType accountType,
    
    Long academyId
) {
}
