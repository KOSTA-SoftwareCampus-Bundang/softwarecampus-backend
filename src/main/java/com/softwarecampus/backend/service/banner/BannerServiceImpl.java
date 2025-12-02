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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerServiceImpl implements BannerService {

        private final BannerRepository bannerRepository;
        private final AttachmentService attachmentService;

        private static final AttachmentCategoryType BANNER_TYPE = AttachmentCategoryType.BANNER;

        /**
         * 배너 등록
         */
        @Override
        @Transactional
        public BannerResponse createBanner(BannerCreateRequest request) {
                Banner banner = new Banner(
                                null,
                                request.getTitle(),
                                null,
                                request.getLinkUrl(),
                                null, // description
                                request.getSequence(),
                                request.getIsActivated());

                Banner savedBanner = bannerRepository.save(banner);

                String finalImageUrl = null;
                QAFileDetail imageAttachment = request.getImageAttachment();

                if (imageAttachment != null) {
                        // 단일 파일이지만 List로 감싸서 호출
                        attachmentService.confirmAttachments(
                                        List.of(imageAttachment), savedBanner.getId(), BANNER_TYPE);

                        List<QAFileDetail> confirmedFiles = attachmentService.getActiveFileDetailsByQAId(BANNER_TYPE,
                                        savedBanner.getId());
                        if (!confirmedFiles.isEmpty()) {
                                finalImageUrl = confirmedFiles.get(0).getFilename();
                        }
                }

                savedBanner.update(
                                request.getTitle(),
                                finalImageUrl,
                                request.getLinkUrl(),
                                null, // description
                                request.getSequence(),
                                request.getIsActivated());

                return BannerResponse.from(savedBanner);
        }

        /**
         * 배너 수정
         */
        @Transactional
        public BannerResponse updateBanner(@NonNull Long bannerId, BannerUpdateRequest request) {
                // 배너 조회
                Banner banner = bannerRepository.findById(bannerId)
                                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

                if (banner.getIsDeleted()) {
                        throw new BannerException(BannerErrorCode.BANNER_ALREADY_DELETED);
                }

                MultipartFile newImageFile = request.getNewImageFile();

                String updatedImageUrl = banner.getImageUrl();

                // 새로운 배너 등록된 경우
                if (newImageFile != null && !newImageFile.isEmpty()) {
                        List<QAFileDetail> uploadedFileDetails = attachmentService.uploadFiles(List.of(newImageFile));

                        if (!uploadedFileDetails.isEmpty()) {
                                QAFileDetail newFileDetail = uploadedFileDetails.get(0);

                                attachmentService.confirmAttachments(uploadedFileDetails, banner.getId(), BANNER_TYPE);

                                updatedImageUrl = newFileDetail.getFilename();
                        }
                }
                banner.update(
                                request.getTitle(),
                                updatedImageUrl,
                                request.getLinkUrl(),
                                banner.getDescription(),
                                request.getSequence(),
                                request.getIsActivated());

                Banner updatedBanner = bannerRepository.save(banner);
                return BannerResponse.from(updatedBanner);
        }

        /**
         * 배너 삭제
         */
        @Override
        @Transactional
        public void deleteBanner(@NonNull Long bannerId) {

                Banner banner = bannerRepository.findById(bannerId)
                                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

                if (!banner.getIsActivated() || banner.getIsDeleted()) {
                        BannerErrorCode errorCode = banner.getIsDeleted() ? BannerErrorCode.BANNER_ALREADY_DELETED
                                        : BannerErrorCode.BANNER_NOT_ACTIVE;
                        throw new BannerException(errorCode);
                }

                List<Attachment> attachmentsToHardDelete = attachmentService.softDeleteAllByCategoryAndId(BANNER_TYPE,
                                bannerId);

                banner.markDeleted();

                attachmentService.hardDeleteS3Files(attachmentsToHardDelete);
        }

        /**
         * 메인 페이지 활성 배너 목록
         */
        public List<BannerResponse> getActiveBanners() {
                return bannerRepository.findByIsActivatedTrueAndIsDeletedFalseOrderBySequenceAsc().stream()
                                .map(BannerResponse::from)
                                .collect(Collectors.toList());
        }

        /**
         * 관리자용 전체 배너 목록 조회
         */
        public List<BannerResponse> getAllBannersForAdmin() {
                return bannerRepository.findByIsDeletedFalseOrderBySequenceAsc().stream()
                                .map(BannerResponse::from)
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void updateBannerOrder(@NonNull Long bannerId, int newOrder) {
                Banner banner = bannerRepository.findById(bannerId)
                                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

                // 특정 목적 메서드 사용으로 코드 명확성 향상
                banner.updateSequence(newOrder);
        }

        /**
         * 두 배너의 순서를 원자적으로 교환
         * 트랜잭션 내에서 두 배너의 순서를 동시에 변경하여 데이터 일관성 보장
         */
        @Override
        @Transactional
        public void swapBannerOrder(@NonNull Long bannerId1, @NonNull Long bannerId2) {
                Banner banner1 = bannerRepository.findById(bannerId1)
                                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

                Banner banner2 = bannerRepository.findById(bannerId2)
                                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

                // 삭제된 배너 체크
                if (banner1.getIsDeleted() || banner2.getIsDeleted()) {
                        throw new BannerException(BannerErrorCode.BANNER_ALREADY_DELETED);
                }

                // 두 배너의 순서를 교환
                int tempOrder = banner1.getSequence();
                banner1.updateSequence(banner2.getSequence());
                banner2.updateSequence(tempOrder);
        }

        @Override
        @Transactional
        public void toggleBannerActivation(@NonNull Long bannerId) {
                Banner banner = bannerRepository.findById(bannerId)
                                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

                // 특정 목적 메서드 사용으로 코드 명확성 향상
                banner.toggleActivation();
        }
}
