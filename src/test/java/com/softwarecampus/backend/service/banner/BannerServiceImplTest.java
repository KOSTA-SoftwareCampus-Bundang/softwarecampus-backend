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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerServiceImplTest {

        @Mock
        private BannerRepository bannerRepository;

        @Mock
        private AttachmentService attachmentService;

        @InjectMocks
        private BannerServiceImpl bannerService;

        private final Long bannerId = 1L;
        private final AttachmentCategoryType BANNER_TYPE = AttachmentCategoryType.BANNER;
        private static final String NEW_URL = "s3/path/new_image.png";

        private Banner activeBanner;
        private Banner deletedBanner;
        private Banner inactiveBanner;
        private QAFileDetail mockFileDetail;

        @BeforeEach
        void setUp() {
                activeBanner = spy(new Banner(
                                bannerId, "Title A", "http://old.url/image.jpg",
                                "http://link.com", "Description A", 1, true));

                deletedBanner = spy(new Banner(
                                2L, "Deleted", "http://del.url", "http://link.com", "Description Deleted", 2, true));
                deletedBanner.markDeleted();

                inactiveBanner = new Banner(
                                3L, "Inactive", "http://inactive.url", "http://link.com", "Description Inactive", 3,
                                false);

                mockFileDetail = QAFileDetail.builder()
                                .id(10L)
                                .originName("banner.png")
                                .filename("s3/path/banner-temp-123.png")
                                .build();
        }

        @Test
        @DisplayName("파일을 포함한 배너 등록 시, 배너를 저장하고 첨부파일을 확정해야 한다")
        void createBanner_withFile_shouldSaveBannerAndConfirmAttachment() {
                BannerCreateRequest request = BannerCreateRequest.builder()
                                .title("New Banner")
                                .linkUrl("http://new.link")
                                .sequence(10)
                                .isActivated(true)
                                .imageAttachment(mockFileDetail)
                                .build();

                Banner savedBanner = spy(new Banner(
                                bannerId, "New Banner", null, "http://new.link", null, 10, true));
                when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

                QAFileDetail confirmedFile = QAFileDetail.builder()
                                .id(10L)
                                .originName("banner.png")
                                .filename("s3/path/banner-final-123.png")
                                .build();
                when(attachmentService.getActiveFileDetailsByQAId(eq(BANNER_TYPE), eq(bannerId)))
                                .thenReturn(List.of(confirmedFile));

                BannerResponse response = bannerService.createBanner(request);

                verify(bannerRepository, times(1)).save(any(Banner.class));

                verify(attachmentService, times(1)).confirmAttachments(
                                eq(List.of(mockFileDetail)),
                                eq(bannerId),
                                eq(BANNER_TYPE));

                verify(savedBanner, times(1)).update(
                                eq("New Banner"),
                                eq("s3/path/banner-final-123.png"),
                                eq("http://new.link"),
                                eq(null),
                                eq(10),
                                eq(true));

                assertNotNull(response);
                assertEquals("s3/path/banner-final-123.png", response.getImageUrl());
        }

        @Test
        @DisplayName("파일이 없는 배너 등록 시, 파일 확정 로직을 호출하지 않아야 한다")
        void createBanner_withoutFile_shouldNotCallConfirmAttachment() {
                BannerCreateRequest request = BannerCreateRequest.builder()
                                .title("No File Banner")
                                .sequence(1)
                                .isActivated(true)
                                .imageAttachment(null)
                                .build();

                Banner savedBanner = spy(new Banner(
                                bannerId, "No File Banner", null, null, null, 1, true));
                when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

                bannerService.createBanner(request);

                verify(attachmentService, never()).confirmAttachments(any(), any(), any());

                verify(savedBanner, times(1)).update(
                                eq("No File Banner"),
                                eq(null),
                                eq(null),
                                eq(null),
                                eq(1),
                                eq(true));
        }

        @Test
        @DisplayName("새 파일 첨부 시 - uploadFiles 및 confirmAttachments 호출 후 새 URL로 업데이트 성공")
        void updateBanner_withNewFile_shouldUploadAndConfirm() {
                MultipartFile newFile = new MockMultipartFile("file", "new_banner.jpg", "image/jpeg",
                                "new content".getBytes());
                final String expectedLinkUrl = "http://updated.link";
                final String expectedTitle = "Updated Title";

                BannerUpdateRequest request = BannerUpdateRequest.builder()
                                .title(expectedTitle)
                                .linkUrl(expectedLinkUrl)
                                .newImageFile(newFile)
                                .sequence(2)
                                .isActivated(true)
                                .build();

                QAFileDetail newFileDetail = QAFileDetail.builder()
                                .id(99L)
                                .originName(newFile.getOriginalFilename())
                                .filename(NEW_URL)
                                .build();
                List<QAFileDetail> uploadedFiles = List.of(newFileDetail);

                when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(activeBanner));
                when(attachmentService.uploadFiles(anyList())).thenReturn(uploadedFiles);
                when(bannerRepository.save(any(Banner.class))).thenAnswer(invocation -> invocation.getArgument(0));

                BannerResponse response = bannerService.updateBanner(bannerId, request);

                verify(attachmentService, times(1)).uploadFiles(eq(List.of(newFile)));

                verify(attachmentService, times(1)).confirmAttachments(
                                eq(uploadedFiles),
                                eq(bannerId),
                                eq(BANNER_TYPE));

                verify(attachmentService, never()).softDeleteAllByCategoryAndId(any(), any());
                verify(attachmentService, never()).hardDeleteS3Files(any());

                verify(activeBanner, times(1)).update(
                                eq(expectedTitle),
                                eq(NEW_URL),
                                eq(expectedLinkUrl),
                                eq("Description A"),
                                eq(2),
                                eq(true));

                assertEquals(NEW_URL, response.getImageUrl());
        }

        @Test
        @DisplayName("새 파일 미첨부 시 - 파일 관련 로직 호출 없고 기존 URL 및 정보 유지 성공")
        void updateBanner_withoutNewFile_shouldNotCallFileServices() {
                final String originalUrl = activeBanner.getImageUrl();
                final String expectedTitle = "Only Title Changed";
                final String expectedLinkUrl = activeBanner.getLinkUrl();

                BannerUpdateRequest request = BannerUpdateRequest.builder()
                                .title(expectedTitle)
                                .linkUrl(expectedLinkUrl)
                                .newImageFile(null)
                                .sequence(activeBanner.getSequence())
                                .isActivated(activeBanner.getIsActivated())
                                .build();

                when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(activeBanner));
                when(bannerRepository.save(any(Banner.class))).thenAnswer(invocation -> invocation.getArgument(0));

                bannerService.updateBanner(bannerId, request);

                verify(attachmentService, never()).uploadFiles(anyList());
                verify(attachmentService, never()).confirmAttachments(any(), any(), any());
                verify(attachmentService, never()).softDeleteAllByCategoryAndId(any(), any());
                verify(attachmentService, never()).hardDeleteS3Files(any());

                verify(activeBanner, times(1)).update(
                                eq(expectedTitle),
                                eq(originalUrl),
                                eq(expectedLinkUrl),
                                eq("Description A"),
                                eq(1),
                                eq(true));
        }

        @Test
        @DisplayName("삭제된 배너 수정 요청 시, BANNER_ALREADY_DELETED 예외를 발생시켜야 한다")
        void updateBanner_alreadyDeleted_shouldThrowException() {
                BannerUpdateRequest request = BannerUpdateRequest.builder().title("Fail").sequence(1).isActivated(true)
                                .linkUrl("http://link.com").build();
                when(bannerRepository.findById(deletedBanner.getId())).thenReturn(Optional.of(deletedBanner));

                BannerException exception = assertThrows(BannerException.class, () -> {
                        bannerService.updateBanner(deletedBanner.getId(), request);
                });
                assertEquals(BannerErrorCode.BANNER_ALREADY_DELETED, exception.getErrorCode());
        }

        @Test
        @DisplayName("활성 배너 삭제 시, 파일 softDelete 및 hardDelete를 수행하고 배너를 softDelete 해야 한다")
        void deleteBanner_shouldSoftDeleteAndHardDeleteFiles() {
                when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(activeBanner));

                Attachment attachmentToDelete = mock(Attachment.class);
                List<Attachment> attachmentsToProcess = List.of(attachmentToDelete);
                when(attachmentService.softDeleteAllByCategoryAndId(eq(BANNER_TYPE), eq(bannerId)))
                                .thenReturn(attachmentsToProcess);

                bannerService.deleteBanner(bannerId);

                verify(attachmentService, times(1)).softDeleteAllByCategoryAndId(eq(BANNER_TYPE), eq(bannerId));
                verify(activeBanner, times(1)).markDeleted();
                verify(attachmentService, times(1)).hardDeleteS3Files(eq(attachmentsToProcess));
        }

        @Test
        @DisplayName("존재하지 않는 배너 삭제 시, BANNER_NOT_FOUND 예외를 발생시켜야 한다")
        void deleteBanner_nonExistent_shouldThrowException() {
                when(bannerRepository.findById(anyLong())).thenReturn(Optional.empty());

                assertThrows(BannerException.class, () -> {
                        bannerService.deleteBanner(999L);
                }, "BANNER_NOT_FOUND 예외가 발생해야 합니다.");

                verify(attachmentService, never()).softDeleteAllByCategoryAndId(any(), any());
        }

        @Test
        @DisplayName("이미 삭제된 배너 삭제 요청 시, BANNER_ALREADY_DELETED 예외를 발생시켜야 한다")
        void deleteBanner_alreadyDeleted_shouldThrowException() {
                when(bannerRepository.findById(deletedBanner.getId())).thenReturn(Optional.of(deletedBanner));

                BannerException exception = assertThrows(BannerException.class, () -> {
                        bannerService.deleteBanner(deletedBanner.getId());
                });
                assertEquals(BannerErrorCode.BANNER_ALREADY_DELETED, exception.getErrorCode());

                verify(attachmentService, never()).softDeleteAllByCategoryAndId(any(), any());
        }

        @Test
        @DisplayName("getActiveBanners 호출 시, 활성화된 배너만 조회해야 한다")
        void getActiveBanners_shouldReturnOnlyActiveBanners() {
                List<Banner> activeBanners = List.of(activeBanner);
                when(bannerRepository.findByIsActivatedTrueAndIsDeletedFalseOrderBySequenceAsc())
                                .thenReturn(activeBanners);

                List<BannerResponse> result = bannerService.getActiveBanners();

                verify(bannerRepository, times(1)).findByIsActivatedTrueAndIsDeletedFalseOrderBySequenceAsc();
                assertEquals(1, result.size());
                assertEquals(activeBanner.getId(), result.get(0).getId());
        }

        @Test
        @DisplayName("getAllBannersForAdmin 호출 시, 삭제되지 않은 모든 배너를 조회해야 한다")
        void getAllBannersForAdmin_shouldReturnAllNonDeletedBanners() {
                List<Banner> allBanners = List.of(activeBanner, inactiveBanner);
                when(bannerRepository.findByIsDeletedFalseOrderBySequenceAsc()).thenReturn(allBanners);

                List<BannerResponse> result = bannerService.getAllBannersForAdmin();

                verify(bannerRepository, times(1)).findByIsDeletedFalseOrderBySequenceAsc();
                assertEquals(2, result.size());
                assertTrue(result.stream().anyMatch(b -> b.getId().equals(activeBanner.getId())));
                assertTrue(result.stream().anyMatch(b -> b.getId().equals(inactiveBanner.getId())));
                assertFalse(result.stream().anyMatch(b -> b.getId().equals(deletedBanner.getId())));
        }
}