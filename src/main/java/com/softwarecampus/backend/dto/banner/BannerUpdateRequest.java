package com.softwarecampus.backend.dto.banner;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class BannerUpdateRequest {

    private MultipartFile newImageFile;

    private String title;
    private String linkUrl;
    private int sequence;
    private Boolean isActivated;
}
