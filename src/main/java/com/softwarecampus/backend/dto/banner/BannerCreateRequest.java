package com.softwarecampus.backend.dto.banner;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class BannerCreateRequest {

    @NotBlank(message = "")
    private MultipartFile imageFile;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    private String linkUrl;

    @NotBlank(message = "순서는 필수입니다.")
    private int sequence;

    @NotBlank(message = "활성화 상태는 필수입니다.")
    private Boolean isActivated;


}
