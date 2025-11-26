package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;
import com.softwarecampus.backend.exception.attachment.AttachmentErrorCode;
import com.softwarecampus.backend.exception.attachment.AttachmentException;
import com.softwarecampus.backend.repository.academy.academyQA.AttachmentRepository;
import com.softwarecampus.backend.service.academy.qna.AttachmentService;
import com.softwarecampus.backend.service.admin.QAAttachmentAdminServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class QAAttachmentAdminServiceImplTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private AttachmentService attachmentService;

    @InjectMocks
    private QAAttachmentAdminServiceImpl qaAttachmentAdminService;

    private final Long qaId = 1L;
    private final Long softDeletedAttachmentId = 100L;
    private final Long activeAttachmentId = 101L;
    private final Long permanentDeleteAttachmentId = 102L;
    private Attachment softDeletedAttachment;
    private Attachment activeAttachment;
    private Attachment otherTypeAttachment;

    @BeforeEach
    void setUp() {
        // Soft Deleted Attachment Mock (복구 대상)
        softDeletedAttachment = mock(Attachment.class);
        when(softDeletedAttachment.getId()).thenReturn(softDeletedAttachmentId);
        when(softDeletedAttachment.getOriginName()).thenReturn("deleted_file.pdf");
        when(softDeletedAttachment.getFilename()).thenReturn("s3/deleted/path.pdf");
        when(softDeletedAttachment.getIsDeleted()).thenReturn(true);
        when(softDeletedAttachment.getCategoryType()).thenReturn(AttachmentCategoryType.QNA);
        doNothing().when(softDeletedAttachment).restore(); // restore() 호출 시 아무 작업도 하지 않도록 설정

        // Active Attachment Mock (복구 불가 대상)
        activeAttachment = mock(Attachment.class);
        when(activeAttachment.getId()).thenReturn(activeAttachmentId);
        when(activeAttachment.getIsDeleted()).thenReturn(false);
        when(activeAttachment.getCategoryType()).thenReturn(AttachmentCategoryType.QNA);

        // Other Type Attachment Mock (카테고리 불일치 대상)
        otherTypeAttachment = mock(Attachment.class);
        when(otherTypeAttachment.getId()).thenReturn(200L);
        when(otherTypeAttachment.getIsDeleted()).thenReturn(true);
        when(otherTypeAttachment.getCategoryType()).thenReturn(AttachmentCategoryType.BANNER);
    }

    // -------------------------------------------------------------------------
    // 1. 삭제된 첨부파일 목록 조회 (getSoftDeletedFilesByQaId)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Q/A ID로 삭제된 첨부파일 목록을 성공적으로 조회해야 한다")
    void getSoftDeletedFilesByQaId_shouldReturnListOfFiles() {
        // Given
        List<Attachment> attachments = List.of(softDeletedAttachment);
        // findByCategoryTypeAndCategoryIdAndIsDeletedTrue 호출 시 Mock 목록 반환
        when(attachmentRepository.findByCategoryTypeAndCategoryIdAndIsDeletedTrue(
                eq(AttachmentCategoryType.QNA),
                eq(qaId)
        )).thenReturn(attachments);

        // When
        List<QAFileDetail> result = qaAttachmentAdminService.getSoftDeletedFilesByQaId(qaId);

        // Then
        // 1. 레포지토리 조회 메서드 호출 검증
        verify(attachmentRepository, times(1)).findByCategoryTypeAndCategoryIdAndIsDeletedTrue(
                eq(AttachmentCategoryType.QNA),
                eq(qaId)
        );

        // 2. 결과 검증
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(softDeletedAttachmentId, result.get(0).getId());
        Assertions.assertEquals("deleted_file.pdf", result.get(0).getOriginName());
    }

    // -------------------------------------------------------------------------
    // 2. 첨부파일 복구 (restoreAttachment)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Soft Delete된 첨부파일을 성공적으로 복구해야 한다")
    void restoreAttachment_shouldRestoreSuccessfully() {
        // Given
        when(attachmentRepository.findById(softDeletedAttachmentId)).thenReturn(Optional.of(softDeletedAttachment));

        // When
        QAFileDetail result = qaAttachmentAdminService.restoreAttachment(softDeletedAttachmentId);

        // Then
        // 1. Attachment 엔티티의 restore() 메서드 호출 검증 (DB isDeleted = false 처리)
        verify(softDeletedAttachment, times(1)).restore();

        // 2. 결과 검증
        Assertions.assertEquals(softDeletedAttachmentId, result.getId());
    }

    @Test
    @DisplayName("존재하지 않는 첨부파일 복구 시, AttachmentException을 발생시켜야 한다")
    void restoreAttachment_nonExistent_shouldThrowException() {
        // Given
        when(attachmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Then
        Assertions.assertThrows(AttachmentException.class, () -> {
            qaAttachmentAdminService.restoreAttachment(999L);
        }, "ATTACHMENT_NOT_FOUND 예외가 발생해야 합니다.");
    }

    @Test
    @DisplayName("이미 활성화된 첨부파일 복구 요청 시, ALREADY_ACTIVE 예외를 발생시켜야 한다")
    void restoreAttachment_alreadyActive_shouldThrowException() {
        // Given
        when(attachmentRepository.findById(activeAttachmentId)).thenReturn(Optional.of(activeAttachment));

        // Then
        AttachmentException exception = Assertions.assertThrows(AttachmentException.class, () -> {
            qaAttachmentAdminService.restoreAttachment(activeAttachmentId);
        });
        Assertions.assertEquals(AttachmentErrorCode.ALREADY_ACTIVE, exception.getErrorCode());
    }

    @Test
    @DisplayName("Q/A 타입이 아닌 첨부파일 복구 요청 시, INVALID_CATEGORY_TYPE 예외를 발생시켜야 한다")
    void restoreAttachment_invalidCategoryType_shouldThrowException() {
        // Given
        when(attachmentRepository.findById(otherTypeAttachment.getId())).thenReturn(Optional.of(otherTypeAttachment));

        // Then
        AttachmentException exception = Assertions.assertThrows(AttachmentException.class, () -> {
            qaAttachmentAdminService.restoreAttachment(otherTypeAttachment.getId());
        });
        Assertions.assertEquals(AttachmentErrorCode.INVALID_CATEGORY_TYPE, exception.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // 3. 첨부파일 영구 삭제 (permanentlyDeleteAttachment)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("첨부파일을 DB 및 S3에서 영구적으로 삭제해야 한다")
    void permanentlyDeleteAttachment_shouldHardDeleteAndDbDelete() {
        // Given
        Attachment attachmentToHardDelete = softDeletedAttachment; // Mock 재사용
        when(attachmentToHardDelete.getId()).thenReturn(permanentDeleteAttachmentId);
        when(attachmentRepository.findById(permanentDeleteAttachmentId)).thenReturn(Optional.of(attachmentToHardDelete));
        when(attachmentToHardDelete.getCategoryType()).thenReturn(AttachmentCategoryType.QNA);

        // When
        qaAttachmentAdminService.permanentlyDeleteAttachment(permanentDeleteAttachmentId);

        // Then
        // 1. attachmentService.hardDeleteS3Files 호출 검증 (S3 물리 삭제)
        // List.of(attachment) 형태로 호출되므로 List.of() 검증
        verify(attachmentService, times(1)).hardDeleteS3Files(eq(List.of(attachmentToHardDelete)));

        // 2. attachmentRepository.delete 호출 검증 (DB 영구 삭제)
        verify(attachmentRepository, times(1)).delete(eq(attachmentToHardDelete));
    }

    @Test
    @DisplayName("Q/A 타입이 아닌 첨부파일 영구 삭제 요청 시, INVALID_CATEGORY_TYPE 예외를 발생시켜야 한다")
    void permanentlyDeleteAttachment_invalidCategoryType_shouldThrowException() {
        // Given
        when(attachmentRepository.findById(otherTypeAttachment.getId())).thenReturn(Optional.of(otherTypeAttachment));

        // Then
        AttachmentException exception = Assertions.assertThrows(AttachmentException.class, () -> {
            qaAttachmentAdminService.permanentlyDeleteAttachment(otherTypeAttachment.getId());
        });
        Assertions.assertEquals(AttachmentErrorCode.INVALID_CATEGORY_TYPE, exception.getErrorCode());

        // 파일 삭제는 발생하지 않았어야 함
        verify(attachmentService, never()).hardDeleteS3Files(any());
        verify(attachmentRepository, never()).delete(any());
    }
}