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

    /** 파일 크기 제한 (10MB) */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    /** 허용 파일 확장자 */
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "webp", // 이미지
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", // 문서
            "txt", "hwp", "zip" // 기타
    );
    /** 허용 Content-Type */
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "application/x-hwp", "application/zip");

    @Override
    @Transactional
    public List<QnaFileDetail> uploadFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<QnaFileDetail> fileDetails = new ArrayList<>();

        for (MultipartFile file : files) {
            // 파일 검증
            validateFile(file);

            // 1. S3에 파일 업로드
            String s3FileUrl = s3Service.uploadFile(file, S3_FOLDER.getPath(), FILE_TYPE);

            // 2. DB에 임시 Attachment 레코드 저장 (categoryId = null)
            Attachment attachment = Attachment.builder()
                    .originName(file.getOriginalFilename())
                    .filename(s3FileUrl) // S3 URL 저장
                    .categoryType(CATEGORY_TYPE)
                    .categoryId(null) // 임시 상태 (Q&A 생성 전)
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
            // 파일 ID 검증
            if (fileDetail.getId() == null || fileDetail.getId() <= 0) {
                throw new BadRequestException("잘못된 첨부파일 ID입니다: " + fileDetail.getId());
            }

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

        // 요청 ID와 실제 조회 결과 개수 미스매치 검증
        if (attachments.size() != fileIds.size()) {
            throw new BadRequestException(
                    String.format("일부 첨부파일을 찾을 수 없습니다. 요청: %d개, 조회: %d개",
                            fileIds.size(), attachments.size()));
        }

        for (Attachment attachment : attachments) {
            // 해당 Q&A에 속하는 파일인지 검증
            if (attachment.getCategoryType() != CATEGORY_TYPE || !qnaId.equals(attachment.getCategoryId())) {
                throw new ForbiddenException("해당 Q&A에 속하지 않는 첨부파일입니다: " + attachment.getId());
            }

            attachment.softDelete();
            log.info("Course Q&A 첨부파일 Soft Delete - attachmentId: {}, qnaId: {}", attachment.getId(), qnaId);
        }

        // S3 파일 물리적 삭제는 FileCleanupScheduler에 위임 (복구 가능성 유지)
        log.debug("Course Q&A 첨부파일 {}건 Soft Delete 완료 - S3 삭제는 스케줄러에서 처리", attachments.size());
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

    /**
     * 파일 유효성 검증
     * - 파일 크기 제한 (10MB)
     * - 허용 확장자 검증
     * - Content-Type 검증
     */
    private void validateFile(MultipartFile file) {
        // 1. 파일이 비어있는지 확인
        if (file.isEmpty()) {
            throw new BadRequestException("빈 파일은 업로드할 수 없습니다.");
        }

        // 2. 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    String.format("파일 크기는 %dMB를 초과할 수 없습니다. (현재: %.2fMB)",
                            MAX_FILE_SIZE / (1024 * 1024),
                            file.getSize() / (1024.0 * 1024.0)));
        }

        // 3. 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("파일 이름이 올바르지 않습니다.");
        }

        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(lastDotIndex + 1).toLowerCase();
        }

        if (extension.isBlank() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(
                    String.format("허용되지 않는 파일 형식입니다. 허용 형식: %s",
                            String.join(", ", ALLOWED_EXTENSIONS)));
        }

        // 4. Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    String.format("허용되지 않는 파일 타입입니다. (Content-Type: %s)", contentType));
        }

        log.debug("파일 검증 통과 - name: {}, size: {}bytes, type: {}",
                originalFilename, file.getSize(), contentType);
    }
}
