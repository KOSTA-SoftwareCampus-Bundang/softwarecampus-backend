package com.softwarecampus.backend.repository.academy.academyQA;

import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.common.AttachmentCategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

        /**
         * Q/A 화면에 표시할 활성 파일 목록 조회
         */
        List<Attachment> findByCategoryTypeAndCategoryIdAndIsDeletedFalse(
                        AttachmentCategoryType categoryType, Long categoryId);

        /**
         * 특정 카테고리 ID에 연결된 소프트 삭제된 파일 목록 조회
         */
        List<Attachment> findByCategoryTypeAndCategoryIdAndIsDeletedTrue(
                        AttachmentCategoryType categoryType, Long categoryId);

        /**
         * 특정 목록을 Soft Delete 처리 (게시글 수정 시 파일 제거용)
         */
        @Modifying
        @Query("UPDATE Attachment a SET a.isDeleted = TRUE, a.deletedAt = CURRENT_TIMESTAMP WHERE a.id IN :ids AND a.isDeleted = FALSE")
        int softDeleteByIds(@Param("ids") List<Long> attachmentIds);

        /**
         * Q/A 게시글 삭제 시 연결된 모든 파일을 처리
         */
        @Modifying
        @Query("UPDATE Attachment a SET a.isDeleted = TRUE, a.deletedAt = CURRENT_TIMESTAMP WHERE a.categoryType = :type AND a.categoryId = :id AND a.isDeleted = FALSE")
        void softDeleteAllByCategoryTypeAndCategoryId(
                        @Param("type") AttachmentCategoryType categoryType,
                        @Param("id") Long categoryId);

        /**
         * 스케줄러용: 삭제된 지 일정 기간이 지난 파일 조회
         */
        List<Attachment> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime threshold);

        /**
         * 고아 파일 정리용: categoryId가 null이고 생성된 지 일정 기간이 지난 임시 파일 조회
         * - 파일 업로드 후 Q&A 생성을 완료하지 않은 경우 발생
         */
        @Query("SELECT a FROM Attachment a WHERE a.categoryId IS NULL AND a.isDeleted = FALSE AND a.createdAt < :threshold")
        List<Attachment> findOrphanedFiles(@Param("threshold") LocalDateTime threshold);
}
