package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.dto.course.QnaFileDetail;
import com.softwarecampus.backend.exception.course.BadRequestException;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.repository.academy.academyQA.AttachmentRepository;
import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Folder;
import com.softwarecampus.backend.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Course Q&A 첨부파일 서비스 구현체
 * - 공용 Attachment 테이블 사용 (categoryType = COURSE_QNA)
 * - S3Folder.COURSE에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseQnaAttachmentServiceImpl implements CourseQnaAttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final S3Service s3Service;

    /** Course Q&A 첨부파일 카테고리 타입 */
    private static final AttachmentCategoryType CATEGORY_TYPE = AttachmentCategoryType.COURSE_QNA;
    /** S3 저장 폴더 */
    private static final S3Folder S3_FOLDER = S3Folder.COURSE;
    /** 파일 타입 설정 (게시판 첨부파일과 동일한 설정 사용) */
    private static final FileType.FileTypeEnum FILE_TYPE = FileType.FileTypeEnum.BOARD_ATTACH;

    @Override
    @Transactional
    public List<QnaFileDetail> uploadFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<QnaFileDetail> fileDetails = new ArrayList<>();

        for (MultipartFile file : files) {
            // 1. S3에 파일 업로드
            String s3FileUrl = s3Service.uploadFile(file, S3_FOLDER.getPath(), FILE_TYPE);

            // 2. DB에 임시 Attachment 레코드 저장 (categoryId = null)
            Attachment attachment = Attachment.builder()
                    .originName(file.getOriginalFilename())
                    .filename(s3FileUrl)  // S3 URL 저장
                    .categoryType(CATEGORY_TYPE)
                    .categoryId(null)  // 임시 상태 (Q&A 생성 전)
                    .build();

            attachmentRepository.save(attachment);

            log.info("Course Q&A 첨부파일 임시 업로드 완료 - id: {}, originName: {}", 
                    attachment.getId(), attachment.getOriginName());

            fileDetails.add(QnaFileDetail.builder()
                    .id(attachment.getId())
                    .originName(attachment.getOriginName())
                    .fileUrl(s3FileUrl)
                    .build());
        }

        return fileDetails;
    }

    @Override
    @Transactional
    public void confirmAttachments(List<QnaFileDetail> fileDetails, Long qnaId) {
        if (fileDetails == null || fileDetails.isEmpty()) {
            return;
        }

        for (QnaFileDetail fileDetail : fileDetails) {
            Attachment attachment = attachmentRepository.findById(fileDetail.getId())
                    .orElseThrow(() -> new BadRequestException("첨부파일을 찾을 수 없습니다: " + fileDetail.getId()));

            // 카테고리 타입 검증 (다른 타입의 파일을 연결하려는 시도 방지)
            if (attachment.getCategoryType() != CATEGORY_TYPE) {
                throw new BadRequestException("잘못된 첨부파일 타입입니다.");
            }

            // 이미 다른 Q&A에 연결된 파일인지 검증
            if (attachment.getCategoryId() != null) {
                throw new BadRequestException("이미 사용 중인 첨부파일입니다: " + fileDetail.getId());
            }

            attachment.updateCategoryId(qnaId);
            log.info("Course Q&A 첨부파일 확정 완료 - attachmentId: {}, qnaId: {}", attachment.getId(), qnaId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QnaFileDetail> getFilesByQnaId(Long qnaId) {
        return attachmentRepository.findByCategoryTypeAndCategoryIdAndIsDeletedFalse(CATEGORY_TYPE, qnaId)
                .stream()
                .map(a -> QnaFileDetail.builder()
                        .id(a.getId())
                        .originName(a.getOriginName())
                        .fileUrl(a.getFilename())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void softDeleteFiles(List<Long> fileIds, Long qnaId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        List<Attachment> attachments = attachmentRepository.findAllById(fileIds);

        for (Attachment attachment : attachments) {
            // 해당 Q&A에 속하는 파일인지 검증
            if (attachment.getCategoryType() != CATEGORY_TYPE || !qnaId.equals(attachment.getCategoryId())) {
                throw new ForbiddenException("해당 Q&A에 속하지 않는 첨부파일입니다: " + attachment.getId());
            }

            attachment.softDelete();
            log.info("Course Q&A 첨부파일 Soft Delete - attachmentId: {}, qnaId: {}", attachment.getId(), qnaId);
        }

        // S3 파일 즉시 삭제 (선택적 - 스케줄러에서 일괄 처리하도록 할 수도 있음)
        hardDeleteS3Files(attachments);
    }

    @Override
    @Transactional
    public List<Attachment> softDeleteAllByQnaId(Long qnaId) {
        List<Attachment> attachments = attachmentRepository
                .findByCategoryTypeAndCategoryIdAndIsDeletedFalse(CATEGORY_TYPE, qnaId);

        attachmentRepository.softDeleteAllByCategoryTypeAndCategoryId(CATEGORY_TYPE, qnaId);

        log.info("Course Q&A 전체 첨부파일 Soft Delete - qnaId: {}, count: {}", qnaId, attachments.size());

        return attachments;
    }

    @Override
    public void hardDeleteS3Files(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        for (Attachment attachment : attachments) {
            try {
                s3Service.deleteFile(attachment.getFilename());
                log.info("Course Q&A S3 파일 삭제 완료 - attachmentId: {}, url: {}", 
                        attachment.getId(), attachment.getFilename());
            } catch (Exception e) {
                // S3 삭제 실패 시 로그만 남기고 계속 진행 (스케줄러에서 재시도)
                log.error("Course Q&A S3 파일 삭제 실패 - attachmentId: {}, error: {}", 
                        attachment.getId(), e.getMessage());
            }
        }
    }
}
