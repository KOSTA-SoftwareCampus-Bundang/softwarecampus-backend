package com.softwarecampus.backend.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.softwarecampus.backend.domain.common.VerificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이메일 인증 코드 발송 요청
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationRequest {
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String email;
    
    // 컨트롤러에서 자동 설정되므로 클라이언트는 보내지 않아도 됨
    @JsonIgnore
    private VerificationType type;
}
