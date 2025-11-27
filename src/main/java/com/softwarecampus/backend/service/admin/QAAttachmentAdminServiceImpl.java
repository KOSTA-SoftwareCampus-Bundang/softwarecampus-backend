package com.softwarecampus.backend.service.admin;

import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;
import com.softwarecampus.backend.exception.attachment.AttachmentErrorCode;
import com.softwarecampus.backend.exception.attachment.AttachmentException;
import com.softwarecampus.backend.repository.academy.academyQA.AttachmentRepository;
import com.softwarecampus.backend.service.academy.qna.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QAAttachmentAdminServiceImpl implements QAAttachmentAdminService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;

    private final AttachmentCategoryType QNA_TYPE = AttachmentCategoryType.QNA;

    /**
     *  특정 Q/A 게시글에 연결된 삭제된 첨부파일 목록을 조회
     */
    @Override
    public List<QAFileDetail> getSoftDeletedFilesByQaId(Long qaId) {
        return attachmentRepository.findByCategoryTypeAndCategoryIdAndIsDeletedTrue(QNA_TYPE, qaId)
                .stream()
                .map(a -> QAFileDetail.builder()
                        .id(a.getId())
                        .originName(a.getOriginName())
                        .filename(a.getFilename())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     *  특정 첨부파일을 복구
     */
    @Override
    @Transactional
    public QAFileDetail restoreAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentException(AttachmentErrorCode.ATTACHMENT_NOT_FOUND));

        if (!attachment.getIsDeleted()) {
            throw new AttachmentException(AttachmentErrorCode.ALREADY_ACTIVE);
        }

        if (!attachment.getCategoryType().equals(QNA_TYPE)) {
            throw new AttachmentException(
                    AttachmentErrorCode.INVALID_CATEGORY_TYPE,
                    "Q/A 타입의 첨부파일만 관리자 복구가 가능합니다."
            );
        }

        // 복구
        attachment.restore();
        log.info("첨부파일 복구 완료: ID={}",  attachmentId);

        return QAFileDetail.builder()
                .id(attachment.getId())
                .originName(attachment.getOriginName())
                .filename(attachment.getFilename())
                .build();
    }

    /**
     *  특정 첨부파일을 DB 및 S3에서 영구 삭제합니다.
     */
    @Override
    @Transactional
    public void permanentlyDeleteAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentException(AttachmentErrorCode.ATTACHMENT_NOT_FOUND));

        if (!attachment.getCategoryType().equals(QNA_TYPE)) {
            throw new AttachmentException(
                    AttachmentErrorCode.INVALID_CATEGORY_TYPE,
                    "Q/A 타입의 첨부파일만 관리자 영구 삭제가 가능합니다."
            );
        }

        // S3에서 파일 삭제
        attachmentService.hardDeleteS3Files(List.of(attachment));

        // DB에서 레코드 영구 삭제
        attachmentRepository.delete(attachment);
        log.warn("첨부파일 영구 삭제 완료: ID={}",  attachmentId);
    }
}
