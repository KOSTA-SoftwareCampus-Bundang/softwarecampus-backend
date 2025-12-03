package com.softwarecampus.backend.controller.banner;

import com.softwarecampus.backend.dto.banner.BannerResponse;
import com.softwarecampus.backend.service.banner.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerMainController {

    private final BannerService bannerService;

    /**
     *  활성화된 배너 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<BannerResponse>> getActiveBanners() {
        List<BannerResponse> banners = bannerService.getActiveBanners();
        return ResponseEntity.ok(banners);
    }
}
