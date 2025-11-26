package com.softwarecampus.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token 갱신 요청 DTO
 * 
 * 보안 요구사항:
 * - refreshToken과 email 모두 필수
 * - 서버에서 인증된 사용자와 이메일 일치 여부 검증 필요
 * 
 * @since 2025-11-23 (Phase 13)
 */
public record RefreshTokenRequest(
    
    @NotBlank(message = "Refresh Token은 필수입니다.")
    String refreshToken,
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    String email
) {}
