package com.softwarecampus.backend.service.admin;

import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;

import java.util.List;

public interface QAAttachmentAdminService {

    /**
     *  특정 Q/A 게시글에 연결된 삭제된 첨부파일 목록을 조회
     */
    List<QAFileDetail> getSoftDeletedFilesByQaId(Long qaId);

    /**
     *  특정 첨부파일을 복구합니다.
     */
    QAFileDetail restoreAttachment(Long attachmentId);

    /**
     *  특정 첨부파일을 DB 및 S3에서 영구 삭제
     */
    void permanentlyDeleteAttachment(Long attachmentId);
}
