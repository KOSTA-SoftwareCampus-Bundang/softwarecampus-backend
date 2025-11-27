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
import org.mockito.junit.jupiter.MockitoExtension; // MockitoExtension으로 변경
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// SpringExtension 대신 MockitoExtension을 사용했습니다.
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
    private static final String NEW_URL = "s3/path/new_image.png"; // 새로운 URL 상수 정의

    private Banner activeBanner;
    private Banner deletedBanner;
    private Banner inactiveBanner;
    private QAFileDetail mockFileDetail;

    @BeforeEach
    void setUp() {
        // 활성 배너 Mock (정상 수정/삭제 대상)
        activeBanner = spy(new Banner(
                bannerId, "Title A", "http://old.url/image.jpg",
                "http://link.com", 1, true
        ));

        // 삭제된 배너 Mock (수정/삭제 불가 대상)
        deletedBanner = spy(new Banner(
                2L, "Deleted", "http://del.url", "http://link.com", 2, true
        ));
        deletedBanner.markDeleted();

        // 비활성 배너 Mock (수정/삭제 불가 대상)
        inactiveBanner = new Banner(
                3L, "Inactive", "http://inactive.url", "http://link.com", 3, false
        );

        // 파일 상세 정보 Mock
        mockFileDetail = QAFileDetail.builder()
                .id(10L)
                .originName("banner.png")
                .filename("s3/path/banner-temp-123.png")
                .build();
    }

    // -------------------------------------------------------------------------
    // 1. 배너 등록 (createBanner)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("파일을 포함한 배너 등록 시, 배너를 저장하고 첨부파일을 확정해야 한다")
    void createBanner_withFile_shouldSaveBannerAndConfirmAttachment() {
        // Given
        BannerCreateRequest request = BannerCreateRequest.builder()
                .title("New Banner")
                .linkUrl("http://new.link")
                .sequence(10)
                .isActivated(true)
                .imageAttachment(mockFileDetail)
                .build();

        // BannerRepository.save() 호출 시, ID가 셋팅된 Banner Mock 반환
        Banner savedBanner = spy(new Banner(
                bannerId, "New Banner", null, "http://new.link", 10, true
        ));
        when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

        // 파일 확정 후, 확정된 파일 정보 반환 Mock
        QAFileDetail confirmedFile = QAFileDetail.builder()
                .id(10L)
                .originName("banner.png")
                .filename("s3/path/banner-final-123.png") // 최종 URL
                .build();
        when(attachmentService.getActiveFileDetailsByQAId(eq(BANNER_TYPE), eq(bannerId)))
                .thenReturn(List.of(confirmedFile));

        // When
        BannerResponse response = bannerService.createBanner(request);

        // Then
        // 1. Banner 엔티티 저장 검증 (최초 1회)
        verify(bannerRepository, times(1)).save(any(Banner.class));

        // 2. 첨부파일 확정 로직 호출 검증
        verify(attachmentService, times(1)).confirmAttachments(
                eq(List.of(mockFileDetail)),
                eq(bannerId),
                eq(BANNER_TYPE)
        );

        // 3. Banner 엔티티의 update() 호출 검증 (최종 URL 반영)
        verify(savedBanner, times(1)).update(
                eq("New Banner"),
                eq("s3/path/banner-final-123.png"), // 최종 URL이 반영되었는지 확인
                eq("http://new.link"),
                eq(10),
                eq(true)
        );

        // 4. 결과 응답 확인
        assertNotNull(response);
        assertEquals("s3/path/banner-final-123.png", response.getImageUrl());
    }

    @Test
    @DisplayName("파일이 없는 배너 등록 시, 파일 확정 로직을 호출하지 않아야 한다")
    void createBanner_withoutFile_shouldNotCallConfirmAttachment() {
        // Given
        BannerCreateRequest request = BannerCreateRequest.builder()
                .title("No File Banner")
                .sequence(1)
                .isActivated(true)
                .imageAttachment(null) // 파일 없음
                .build();

        Banner savedBanner = spy(new Banner(
                bannerId, "No File Banner", null, null, 1, true
        ));
        when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

        // When
        bannerService.createBanner(request);

        // Then
        // 1. 파일 확정 로직 호출되지 않음 검증
        verify(attachmentService, never()).confirmAttachments(any(), any(), any());

        // 2. 최종 URL 반영 시, URL이 null로 update 되었는지 검증
        verify(savedBanner, times(1)).update(
                eq("No File Banner"),
                eq(null),
                eq(null),
                eq(1),
                eq(true)
        );
    }

    // -------------------------------------------------------------------------
    // 2. 배너 수정 (updateBanner) - 수정된 로직에 맞춰 재작성
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("새 파일 첨부 시 - uploadFiles 및 confirmAttachments 호출 후 새 URL로 업데이트 성공")
    void updateBanner_withNewFile_shouldUploadAndConfirm() {
        // Given
        MultipartFile newFile = new MockMultipartFile("file", "new_banner.jpg", "image/jpeg", "new content".getBytes());
        final String expectedLinkUrl = "http://updated.link";
        final String expectedTitle = "Updated Title";

        BannerUpdateRequest request = BannerUpdateRequest.builder()
                .title(expectedTitle)
                .linkUrl(expectedLinkUrl)
                .newImageFile(newFile)
                .sequence(2)
                .isActivated(true)
                .build();

        // AttachmentService의 uploadFiles가 반환할 임시 파일 상세 정보
        QAFileDetail newFileDetail = QAFileDetail.builder()
                .id(99L)
                .originName(newFile.getOriginalFilename())
                .filename(NEW_URL) // 이 URL로 배너가 업데이트 되어야 함
                .build();
        List<QAFileDetail> uploadedFiles = List.of(newFileDetail);

        // 1. 배너 조회 Mock
        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(activeBanner));

        // 2. 파일 업로드 Mock
        when(attachmentService.uploadFiles(anyList())).thenReturn(uploadedFiles);

        // 3. 배너 저장 Mock (실제 업데이트된 Banner 객체 반환)
        when(bannerRepository.save(any(Banner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BannerResponse response = bannerService.updateBanner(bannerId, request);

        // Then
        // 1. 새 파일 업로드 (임시 레코드 생성) 호출 검증
        verify(attachmentService, times(1)).uploadFiles(eq(List.of(newFile)));

        // 2. 파일 확정 (배너 ID에 연결) 호출 검증
        verify(attachmentService, times(1)).confirmAttachments(
                eq(uploadedFiles),
                eq(bannerId),
                eq(BANNER_TYPE)
        );

        // 3. 파일 삭제 로직 호출 안 됨 검증 (기존 파일 보존)
        verify(attachmentService, never()).softDeleteAllByCategoryAndId(any(), any());
        verify(attachmentService, never()).hardDeleteS3Files(any());

        // 4. Banner 엔티티의 update() 호출 검증 (새 URL 반영)
        verify(activeBanner, times(1)).update(
                eq(expectedTitle),
                eq(NEW_URL), // 새 URL이 반영되었는지 확인
                eq(expectedLinkUrl),
                eq(2),
                eq(true)
        );

        // 5. 결과 응답 확인
        assertEquals(NEW_URL, response.getImageUrl());
    }

    @Test
    @DisplayName("새 파일 미첨부 시 - 파일 관련 로직 호출 없고 기존 URL 및 정보 유지 성공")
    void updateBanner_withoutNewFile_shouldNotCallFileServices() {
        // Given
        final String originalUrl = activeBanner.getImageUrl();
        final String expectedTitle = "Only Title Changed";
        final String expectedLinkUrl = activeBanner.getLinkUrl();

        BannerUpdateRequest request = BannerUpdateRequest.builder()
                .title(expectedTitle)
                .linkUrl(expectedLinkUrl)
                .newImageFile(null) // 파일 변경 없음
                .sequence(activeBanner.getSequence())
                .isActivated(activeBanner.getIsActivated())
                .build();

        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(activeBanner));
        when(bannerRepository.save(any(Banner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        bannerService.updateBanner(bannerId, request);

        // Then
        // 1. 파일 업로드/확정/삭제 로직 모두 호출되지 않음 검증
        verify(attachmentService, never()).uploadFiles(anyList());
        verify(attachmentService, never()).confirmAttachments(any(), any(), any());
        verify(attachmentService, never()).softDeleteAllByCategoryAndId(any(), any());
        verify(attachmentService, never()).hardDeleteS3Files(any());

        // 2. Banner 엔티티의 update() 호출 검증 (기존 URL 유지)
        verify(activeBanner, times(1)).update(
                eq(expectedTitle),
                eq(originalUrl), // 기존 URL 유지 확인
                eq(expectedLinkUrl),
                eq(1),
                eq(true)
        );
    }

    @Test
    @DisplayName("삭제된 배너 수정 요청 시, BANNER_ALREADY_DELETED 예외를 발생시켜야 한다")
    void updateBanner_alreadyDeleted_shouldThrowException() {
        // Given
        BannerUpdateRequest request = BannerUpdateRequest.builder().title("Fail").sequence(1).isActivated(true).linkUrl("http://link.com").build();
        when(bannerRepository.findById(deletedBanner.getId())).thenReturn(Optional.of(deletedBanner));

        // Then
        BannerException exception = assertThrows(BannerException.class, () -> {
            bannerService.updateBanner(deletedBanner.getId(), request);
        });
        assertEquals(BannerErrorCode.BANNER_ALREADY_DELETED, exception.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // 3. 배너 삭제 (deleteBanner)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("활성 배너 삭제 시, 파일 softDelete 및 hardDelete를 수행하고 배너를 softDelete 해야 한다")
    void deleteBanner_shouldSoftDeleteAndHardDeleteFiles() {
        // Given
        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(activeBanner));

        // 파일 Soft Delete 결과 Mock
        Attachment attachmentToDelete = mock(Attachment.class);
        List<Attachment> attachmentsToProcess = List.of(attachmentToDelete);
        when(attachmentService.softDeleteAllByCategoryAndId(eq(BANNER_TYPE), eq(bannerId)))
                .thenReturn(attachmentsToProcess);

        // When
        bannerService.deleteBanner(bannerId);

        // Then
        // 1. 파일 Soft Delete 호출 검증
        verify(attachmentService, times(1)).softDeleteAllByCategoryAndId(eq(BANNER_TYPE), eq(bannerId));

        // 2. Banner 엔티티의 markDeleted() 호출 검증 (DB Soft Delete)
        verify(activeBanner, times(1)).markDeleted();

        // 3. 파일 Hard Delete 호출 검증 (S3 물리 삭제)
        verify(attachmentService, times(1)).hardDeleteS3Files(eq(attachmentsToProcess));
    }

    @Test
    @DisplayName("존재하지 않는 배너 삭제 시, BANNER_NOT_FOUND 예외를 발생시켜야 한다")
    void deleteBanner_nonExistent_shouldThrowException() {
        // Given
        when(bannerRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Then
        assertThrows(BannerException.class, () -> {
            bannerService.deleteBanner(999L);
        }, "BANNER_NOT_FOUND 예외가 발생해야 합니다.");

        // 파일 서비스가 호출되지 않았는지 확인
        verify(attachmentService, never()).softDeleteAllByCategoryAndId(any(), any());
    }

    @Test
    @DisplayName("이미 삭제된 배너 삭제 요청 시, BANNER_ALREADY_DELETED 예외를 발생시켜야 한다")
    void deleteBanner_alreadyDeleted_shouldThrowException() {
        // Given
        when(bannerRepository.findById(deletedBanner.getId())).thenReturn(Optional.of(deletedBanner));

        // Then
        BannerException exception = assertThrows(BannerException.class, () -> {
            bannerService.deleteBanner(deletedBanner.getId());
        });
        assertEquals(BannerErrorCode.BANNER_ALREADY_DELETED, exception.getErrorCode());

        // 파일 서비스가 호출되지 않았는지 확인
        verify(attachmentService, never()).softDeleteAllByCategoryAndId(any(), any());
    }

    // -------------------------------------------------------------------------
    // 4. 배너 목록 조회 (getActiveBanners, getAllBannersForAdmin)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getActiveBanners 호출 시, 활성화된 배너만 조회해야 한다")
    void getActiveBanners_shouldReturnOnlyActiveBanners() {
        // Given
        List<Banner> activeBanners = List.of(activeBanner);
        when(bannerRepository.findByIsActivatedTrueAndIsDeletedFalseOrderBySequenceAsc()).thenReturn(activeBanners);

        // When
        List<BannerResponse> result = bannerService.getActiveBanners();

        // Then
        verify(bannerRepository, times(1)).findByIsActivatedTrueAndIsDeletedFalseOrderBySequenceAsc();
        assertEquals(1, result.size());
        assertEquals(activeBanner.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("getAllBannersForAdmin 호출 시, 삭제되지 않은 모든 배너를 조회해야 한다")
    void getAllBannersForAdmin_shouldReturnAllNonDeletedBanners() {
        // Given
        // active, inactive 배너 포함, deleted 배너 제외
        List<Banner> allBanners = List.of(activeBanner, inactiveBanner);
        when(bannerRepository.findByIsDeletedFalseOrderBySequenceAsc()).thenReturn(allBanners);

        // When
        List<BannerResponse> result = bannerService.getAllBannersForAdmin();

        // Then
        verify(bannerRepository, times(1)).findByIsDeletedFalseOrderBySequenceAsc();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(b -> b.getId().equals(activeBanner.getId())));
        assertTrue(result.stream().anyMatch(b -> b.getId().equals(inactiveBanner.getId())));
        assertFalse(result.stream().anyMatch(b -> b.getId().equals(deletedBanner.getId())));
    }
}