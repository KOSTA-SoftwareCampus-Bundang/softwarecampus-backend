package com.softwarecampus.backend.dto.banner;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class BannerUpdateRequest {

    private MultipartFile newImageFile;

    @NotNull(message = "제목은 필수입니다.")
    private String title;
    private String linkUrl;
    @NotNull(message = "순서는 필수입니다.")
    private Integer sequence;
    @NotNull(message = "활성화 상태는 필수입니다.")
    private Boolean isActivated;
}
