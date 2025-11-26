package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;
import com.softwarecampus.backend.repository.academy.academyQA.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final S3Service s3Service;

    private static final AttachmentCategoryType QNA_TYPE = AttachmentCategoryType.QNA;
    private static final FileType.FileTypeEnum QNA_FILE_TYPE = FileType.FileTypeEnum.BOARD_ATTACH;
    private static final S3Folder QNA_S3_FOLDER = S3Folder.academy;

    /**
     *  파일 업로드 및 임시 저장
     */
    @Override
    @Transactional
    public List<QAFileDetail> uploadFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();

        List<QAFileDetail> fileDetails = new ArrayList<>();
        for (MultipartFile file : files) {

            // 🟢 1. S3Service를 사용하여 파일 업로드.
            String s3FileUrl = s3Service.uploadFile(file, QNA_S3_FOLDER, QNA_FILE_TYPE);

            // 🟢 2. DB에 임시 Attachment 레코드 저장
            Attachment attachment = Attachment.builder()
                    .originName(file.getOriginalFilename())
                    .filename(s3FileUrl) // S3 URL 저장
                    .categoryType(QNA_TYPE)
                    .categoryId(null) // 임시 ID로 마킹
                    .build();

            attachmentRepository.save(attachment);

            fileDetails.add(QAFileDetail.builder()
                    .id(attachment.getId())
                    .originName(attachment.getOriginName())
                    .filename(s3FileUrl)
                    .build());
        }
        return fileDetails;
    }

    /**
     *  임시 저장된 게시글 ID를 연결하여 파일을 확정
     */
    @Override
    @Transactional
    public void confirmAttachments(List<QAFileDetail> fileDetails, Long categoryId, AttachmentCategoryType type) {
        if (fileDetails == null || fileDetails.isEmpty()) return;

        for (QAFileDetail fileDetail : fileDetails) {
            Attachment attachment = attachmentRepository.findById(fileDetail.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));

            attachment.updateCategoryId(categoryId);
            attachment.updateCategoryType(type);
        }
    }

    /**
     *  특정 Q/A에 연결된 모든 파일을 Soft Delete 처리하고, Hard Delete를 위한 목록 반환
     */
    @Transactional
    @Override
    public List<Attachment> softDeleteAllByCategoryAndId(AttachmentCategoryType type, Long categoryId) {
        List<Attachment> attachmentsToHardDelete =
                attachmentRepository.findByCategoryTypeAndCategoryIdAndIsDeletedFalse(type, categoryId);

        attachmentRepository.softDeleteAllByCategoryTypeAndCategoryId(type, categoryId);
        return attachmentsToHardDelete;
    }

    /**
     *  Soft Delete된 파일 목록을 받아 S3에서도 물리적으로 삭제 처리
     */
    @Override
    public void hardDeleteS3Files(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return;

        for (Attachment attachment : attachments) {
            try {
                s3Service.deleteFile(attachment.getFilename());
                log.info("Attachment deleted successfully");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     *  Q/A에 연결된 활성 파일 목록 조회
     */
    @Override
    public List<QAFileDetail> getActiveFileDetailsByQAId(AttachmentCategoryType type, Long categoryId) {
        return attachmentRepository.findByCategoryTypeAndCategoryIdAndIsDeletedFalse(type, categoryId)
                .stream()
                .map(a -> QAFileDetail.builder()
                        .id(a.getId())
                        .originName(a.getOriginName())
                        .filename(a.getFilename()) // S3 URL
                        .build())
                .collect(Collectors.toList());
    }
}
