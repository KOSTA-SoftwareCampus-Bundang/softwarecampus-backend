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
    private String description; // 배너 부제목/설명
    private String imageUrl;
    private String linkUrl;
    private Integer sequence;
    private Boolean isActivated;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;

    public static BannerResponse from(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .description(banner.getDescription())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .sequence(banner.getSequence())
                .isActivated(banner.getIsActivated())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .isDeleted(banner.getIsDeleted())
                .build();
    }
}
