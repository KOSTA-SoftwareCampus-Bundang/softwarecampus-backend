/**
 * 기관 거절 요청 DTO
 * 작성자: GitHub Copilot
 * 작성일: 2025-11-28
 */
package com.softwarecampus.backend.dto.academy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademyRejectRequest {
    
    @NotBlank(message = "거절 사유는 필수입니다")
    @Size(max = 500, message = "거절 사유는 500자 이내로 입력해주세요")
    private String reason;
}
