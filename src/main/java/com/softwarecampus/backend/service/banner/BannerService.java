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

    void toggleBannerActivation(Long bannerId);
}
