package com.softwarecampus.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO
 * 
 * @param email 이메일 (필수, 이메일 형식)
 * @param password 비밀번호 (필수)
 * 
 * @author 태윤
 */
public record LoginRequest(
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    String password
) {}
