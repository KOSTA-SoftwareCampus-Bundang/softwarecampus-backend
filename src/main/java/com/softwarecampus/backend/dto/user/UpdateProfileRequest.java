package com.softwarecampus.backend.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 수정 요청 DTO
 * 
 * 모든 필드는 선택사항(null 허용)이며, 제공된 필드만 업데이트됩니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    private String userName;

    @Pattern(
        regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
        message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)"
    )
    private String phoneNumber;

    @Size(max = 200, message = "주소는 200자 이하여야 합니다")
    private String address;

    @Size(max = 100, message = "소속은 100자 이하여야 합니다")
    private String affiliation;

    @Size(max = 50, message = "직책은 50자 이하여야 합니다")
    private String position;

    private String profileImage;
}
