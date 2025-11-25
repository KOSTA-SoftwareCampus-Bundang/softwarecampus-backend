package com.softwarecampus.backend.service.academy.qna;

import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    /**
     *  파일 업로드 및 임시 저장 후 FileDetail 반환
     */
    List<QAFileDetail> uploadFiles(List<MultipartFile> files);

    /**
     * Q/A ID와 연결하여 최종 확정
     */
    void confirmAttachments(List<QAFileDetail> fileDetails, Long categoryId, AttachmentCategoryType type);

    /**
     *  특정 Q/A에 연결된 모든 파일을 soft delete 처리하고,
     *  Hard Delete를 위해 S3 URL이 포함된 엔티티 목록을 반환
     */
    List<Attachment> softDeleteAllByQAId(AttachmentCategoryType type ,Long categoryId);

    @Transactional
    List<Attachment> softDeleteAllByCategoryAndId(AttachmentCategoryType type, Long categoryId);

    /**
     *  Soft Delete된 목록을 받아 S3에서도 삭제 처리
     */
    void hardDeleteS3Files(List<Attachment> attachments);

    /**
     *  Q/A에 연결된 활성 파일 목록 조회
     */
    List<QAFileDetail> getActiveFileDetailsByQAId(AttachmentCategoryType type,Long categoryId);
}
