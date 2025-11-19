package com.softwarecampus.backend.dto.banner;

import com.softwarecampus.backend.domain.banner.Banner;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BannerResponse {

    private Long id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private int sequence;
    private Boolean isActivated;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;

    public static BannerResponse from(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .sequence(banner.getSequence())
                .isActivated(banner.getIsActivated())
                // 상속받은 필드 매핑
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .isDeleted(banner.getIsDeleted())
                .build();
    }
}
