package com.softwarecampus.backend.dto.user;


import com.softwarecampus.backend.domain.common.ApprovalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountUpdateRequest {
    @NotBlank
    @Size(max = 50)
    private String userName;

    @NotBlank
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    private String phoneNumber;

    private String affiliation;
    private String position;
    private String address;

    // 관리자만 수정 가능: 기관 승인 상태
    private ApprovalStatus accountApproved;
}
