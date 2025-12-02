package com.softwarecampus.backend.controller.banner;

import com.softwarecampus.backend.dto.banner.BannerCreateRequest;
import com.softwarecampus.backend.dto.banner.BannerResponse;
import com.softwarecampus.backend.dto.banner.BannerUpdateRequest;
import com.softwarecampus.backend.service.banner.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/banners")
@RequiredArgsConstructor
public class BannerAdminController {

    private final BannerService bannerService;

    /**
     * 배너 등록
     */
    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<BannerResponse> createBanner(
            @RequestPart("imageFile") MultipartFile imageFile,
            @Validated @ModelAttribute BannerCreateRequest request) {
        request.setImageFile(imageFile);

        BannerResponse response = bannerService.createBanner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 배너 수정
     */
    @PutMapping(value = "/{bannerId}", consumes = { "multipart/form-data" })
    public ResponseEntity<BannerResponse> updateBanner(
            @PathVariable Long bannerId,
            @RequestPart(value = "newImageFile", required = false) MultipartFile newImageFile,
            @Validated @ModelAttribute BannerUpdateRequest request) {
        request.setNewImageFile(newImageFile);

        BannerResponse response = bannerService.updateBanner(bannerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 배너 삭제
     */
    @DeleteMapping("/{bannerId}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long bannerId) {
        bannerService.deleteBanner(bannerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 관리자용 전체 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<BannerResponse>> getAllBannersForAdmin() {
        List<BannerResponse> banners = bannerService.getAllBannersForAdmin();
        return ResponseEntity.ok(banners);
    }

    /**
     * 배너 순서 변경
     */
    @PatchMapping("/{bannerId}/order")
    public ResponseEntity<Void> updateBannerOrder(
            @PathVariable Long bannerId,
            @RequestParam int newOrder) {
        bannerService.updateBannerOrder(bannerId, newOrder);
        return ResponseEntity.ok().build();
    }

    /**
     * 배너 활성화/비활성화 토글
     */
    @PatchMapping("/{bannerId}/toggle")
    public ResponseEntity<Void> toggleBannerActivation(@PathVariable Long bannerId) {
        bannerService.toggleBannerActivation(bannerId);
        return ResponseEntity.ok().build();
    }
}
