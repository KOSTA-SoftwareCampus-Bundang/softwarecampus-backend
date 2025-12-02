package com.softwarecampus.backend.service.banner;

import com.softwarecampus.backend.dto.banner.BannerCreateRequest;
import com.softwarecampus.backend.dto.banner.BannerResponse;
import com.softwarecampus.backend.dto.banner.BannerUpdateRequest;
import java.util.List;

public interface BannerService {

    BannerResponse createBanner(BannerCreateRequest request);

    BannerResponse updateBanner(Long bannerId, BannerUpdateRequest request);

    void deleteBanner(Long bannerId);

    List<BannerResponse> getActiveBanners();

    List<BannerResponse> getAllBannersForAdmin();

    void updateBannerOrder(Long bannerId, int newOrder);

    /**
     * 두 배너의 순서를 원자적으로 교환
     */
    void swapBannerOrder(Long bannerId1, Long bannerId2);

    void toggleBannerActivation(Long bannerId);
}
