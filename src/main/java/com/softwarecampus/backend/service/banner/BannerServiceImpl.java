package com.softwarecampus.backend.service.banner;

import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.banner.Banner;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;
import com.softwarecampus.backend.dto.banner.BannerCreateRequest;
import com.softwarecampus.backend.dto.banner.BannerResponse;
import com.softwarecampus.backend.dto.banner.BannerUpdateRequest;
import com.softwarecampus.backend.exception.banner.BannerErrorCode;
import com.softwarecampus.backend.exception.banner.BannerException;
import com.softwarecampus.backend.repository.banner.BannerRepository;
import com.softwarecampus.backend.service.academy.qna.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final AttachmentService attachmentService;

    private static final AttachmentCategoryType BANNER_TYPE = AttachmentCategoryType.BANNER;

    /**
     *  배너 등록
     */
    @Override
    @Transactional
    public BannerResponse createBanner(BannerCreateRequest request) {
        Banner banner = new Banner(
                null,
                request.getTitle(),
                null,
                request.getLinkUrl(),
                request.getSequence(),
                request.getIsActivated()
        );

        Banner savedBanner = bannerRepository.save(banner);

        String finalImageUrl = null;
        QAFileDetail imageAttachment = request.getImageAttachment();

        if (imageAttachment != null) {
            // 단일 파일이지만 List로 감싸서 호출
            attachmentService.confirmAttachments(
                    List.of(imageAttachment), savedBanner.getId(), BANNER_TYPE
            );

            List<QAFileDetail> confirmedFiles = attachmentService.getActiveFileDetailsByQAId(BANNER_TYPE, savedBanner.getId());
            if (!confirmedFiles.isEmpty()) {
                finalImageUrl = confirmedFiles.get(0).getFilename();
            }
        }

        savedBanner.update(
                request.getTitle(),
                finalImageUrl,
                request.getLinkUrl(),
                request.getSequence(),
                request.getIsActivated()
        );

        return BannerResponse.from(savedBanner);
    }

    /**
     *  배너 수정
     */
    @Transactional
    public BannerResponse updateBanner(Long bannerId, BannerUpdateRequest request) {
        // 배너 조회
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

        // 배너 상태 확인 : 비활성 또는 삭제된 배너는 수정 불가
        if (!banner.getIsActivated() || banner.getIsDeleted()) {
            BannerErrorCode errorCode = banner.getIsDeleted() ? BannerErrorCode.BANNER_ALREADY_DELETED : BannerErrorCode.BANNER_NOT_ACTIVE;
            throw new BannerException(errorCode);
        }

        String newImageUrl = banner.getImageUrl();

        // 새로운 파일이 제공된 경우
        MultipartFile newAttachment = request.getNewImageFile();
        if (newAttachment != null && !newAttachment.isEmpty()) {
            List<Attachment> attachmentsToHardDelete =
                    attachmentService.softDeleteAllByCategoryAndId(BANNER_TYPE, bannerId);
            attachmentService.hardDeleteS3Files(attachmentsToHardDelete);

            // 새 파일의 URL을 조회하여 newImageUrl 변수에 업데이트
            List<QAFileDetail> updatedFiles = attachmentService.getActiveFileDetailsByQAId(BANNER_TYPE, bannerId);
            newImageUrl = updatedFiles.isEmpty() ? null : updatedFiles.get(0).getFilename();
        }
        // Banner 엔티티 업데이트
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
    @Override
    @Transactional
    public void deleteBanner(Long bannerId) {

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

        if (!banner.getIsActivated() || banner.getIsDeleted()) {
            BannerErrorCode errorCode = banner.getIsDeleted() ? BannerErrorCode.BANNER_ALREADY_DELETED : BannerErrorCode.BANNER_NOT_ACTIVE;
            throw new BannerException(errorCode);
        }

        List<Attachment> attachmentsToHardDelete =
                attachmentService.softDeleteAllByCategoryAndId(BANNER_TYPE, bannerId);

        banner.markDeleted();

        attachmentService.hardDeleteS3Files(attachmentsToHardDelete);
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
