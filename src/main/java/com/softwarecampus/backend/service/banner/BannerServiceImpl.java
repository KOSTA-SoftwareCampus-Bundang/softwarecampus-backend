package com.softwarecampus.backend.service.banner;

import com.softwarecampus.backend.domain.banner.Banner;
import com.softwarecampus.backend.dto.banner.BannerCreateRequest;
import com.softwarecampus.backend.dto.banner.BannerResponse;
import com.softwarecampus.backend.dto.banner.BannerUpdateRequest;
import com.softwarecampus.backend.repository.banner.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final FileService fileService;

    private static final String BANNER_IMAGE_DIR = "banner";

    /**
     *  배너 등록
     */
    @Transactional
    public BannerResponse createBanner(BannerCreateRequest request) {
        // 파일 S3 업로드 및 URL 획득
        String imageUrl = fileService.uploadFile(request.getImageFile(), BANNER_IMAGE_DIR);

        Banner banner = new Banner(
                null,
                request.getTitle(),
                imageUrl,
                request.getLinkUrl(),
                request.getSequence(),
                request.getIsActivated()
        );

        Banner savedBanner = bannerRepository.save(banner);
        return BannerResponse.from(savedBanner);
    }

    /**
     *  배너 수정
     */
    @Transactional
    public BannerResponse updateBanner(Long bannerId, BannerUpdateRequest request) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found!"));

        if (!banner.isActive()) {
            throw new IllegalStateException("Banner is not active.");
        }

        String newImageUrl = banner.getImageUrl();

        if (request.getNewImageFile() != null && !request.getNewImageFile().isEmpty()) {

            fileService.deleteFile(banner.getImageUrl());
            newImageUrl = fileService.uploadFile(request.getNewImageFile(), BANNER_IMAGE_DIR);
        }

        banner.update(
                request.getTitle(),
                newImageUrl, // ⬅️ 새 URL 또는 기존 URL
                request.getLinkUrl(),
                request.getSequence(),
                request.getIsActivated()
        );
        return BannerResponse.from(banner);
    }

    /**
     *  배너 삭제
     */
    @Transactional
    public void deleteBanner(Long bannerId) {

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found!"));

        if (!banner.isActive()) {
            throw new IllegalStateException("Banner is not active.");
        }
        fileService.deleteFile(banner.getImageUrl());
        banner.markDeleted();
    }

    /**
     *  메인 페이지 활성 배너 목록
     */
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByIsActivatedTrueAndIsDeletedFalseOrderBySequenceAsc().stream()
                .map(BannerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     *  관리자용 전체 배너 목록 조회
     */
    public List<BannerResponse> getAllBannersForAdmin() {
        return bannerRepository.findByIsDeletedFalseOrderBySequenceAsc().stream()
                .map(BannerResponse::from)
                .collect(Collectors.toList());
    }
}
