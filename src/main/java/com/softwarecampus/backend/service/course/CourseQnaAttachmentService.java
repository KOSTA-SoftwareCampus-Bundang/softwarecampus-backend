package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.dto.course.QnaFileDetail;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Course Q&A 첨부파일 서비스 인터페이스
 * - 공용 Attachment 테이블 사용 (categoryType = COURSE_QNA)
 * - 고아 파일 방지를 위한 임시 저장 → 확정 플로우
 */
public interface CourseQnaAttachmentService {

    /**
     * 파일 업로드 및 임시 저장
     * - S3에 파일 업로드
     * - DB에 categoryId = null로 임시 저장
     * - Q&A 생성/수정 완료 시 confirmAttachments()로 연결
     *
     * @param files 업로드할 파일 목록
     * @return 업로드된 파일 정보 목록
     */
    List<QnaFileDetail> uploadFiles(List<MultipartFile> files);

    /**
     * 임시 저장된 파일을 Q&A에 연결하여 확정
     * - categoryId를 Q&A ID로 설정
     *
     * @param fileDetails 확정할 파일 정보 목록
     * @param qnaId       연결할 Q&A ID
     */
    void confirmAttachments(List<QnaFileDetail> fileDetails, Long qnaId);

    /**
     * Q&A에 연결된 활성 파일 목록 조회
     *
     * @param qnaId Q&A ID
     * @return 첨부파일 목록
     */
    List<QnaFileDetail> getFilesByQnaId(Long qnaId);

    /**
     * 특정 파일들을 Soft Delete 처리
     * - Q&A 수정 시 삭제 요청된 파일 처리
     *
     * @param fileIds 삭제할 파일 ID 목록
     * @param qnaId   해당 Q&A ID (권한 검증용)
     */
    void softDeleteFiles(List<Long> fileIds, Long qnaId);

    /**
     * Q&A 삭제 시 연결된 모든 파일 Soft Delete
     *
     * @param qnaId 삭제할 Q&A ID
     * @return Soft Delete된 파일 목록 (S3 삭제용)
     */
    List<Attachment> softDeleteAllByQnaId(Long qnaId);

    /**
     * S3에서 파일 물리적 삭제
     * - Soft Delete된 파일들의 실제 S3 파일 삭제
     *
     * @param attachments 삭제할 파일 엔티티 목록
     */
    void hardDeleteS3Files(List<Attachment> attachments);
}
