package com.softwarecampus.backend.dto.academy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 기관 등록 요청 DTO
 * Multipart/form-data 요청을 받기 위해 @ModelAttribute와 함께 사용
 * 수정일: 2025-12-03 - 파일 첨부를 선택적으로 변경 (개발 환경 호환)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademyCreateRequest {

    @NotBlank(message = "기관명은 필수입니다")
    private String name;
    
    @NotBlank(message = "주소는 필수입니다")
    private String address;
    
    @NotBlank(message = "사업자등록번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다 (예: 123-45-67890)")
    private String businessNumber;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String email;
    
    /**
     * 첨부파일 목록 (사업자등록증, 교육기관 인증서 등)
     * 선택 사항 - 파일이 없어도 기관 등록 가능
     */
    private List<MultipartFile> files;
}
