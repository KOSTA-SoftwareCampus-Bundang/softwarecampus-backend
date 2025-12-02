package com.softwarecampus.backend.scheduler;

import com.softwarecampus.backend.domain.academy.qna.Attachment;
import com.softwarecampus.backend.domain.board.BoardAttach;
import com.softwarecampus.backend.domain.course.CourseImage;
import com.softwarecampus.backend.domain.course.CourseReviewAttachment;
import com.softwarecampus.backend.domain.course.CourseReviewFile;
import com.softwarecampus.backend.repository.academy.academyQA.AttachmentRepository;
import com.softwarecampus.backend.repository.board.BoardAttachRepository;
import com.softwarecampus.backend.repository.course.CourseImageRepository;
import com.softwarecampus.backend.repository.course.CourseReviewAttachmentRepository;
import com.softwarecampus.backend.repository.course.CourseReviewFileRepository;
import com.softwarecampus.backend.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Soft-Delete된 파일 및 고아 파일 정리 스케줄러
 * - 매일 새벽 5시 실행
 * - 삭제된 지 1일(설정값) 지난 파일 영구 삭제 (DB & S3)
 * - 고아 파일(임시 업로드 후 미확정) 정리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final CourseReviewFileRepository reviewFileRepository;
    private final CourseReviewAttachmentRepository reviewAttachmentRepository;
    private final BoardAttachRepository boardAttachRepository;
    private final AttachmentRepository attachmentRepository;
    private final CourseImageRepository courseImageRepository;
    private final S3Service s3Service;

    @Value("${app.file.cleanup.days:1}")
    private int cleanupDays;

    /** 고아 파일 정리 기준 시간 (시간 단위, 기본 24시간) */
    @Value("${app.file.orphan.cleanup.hours:24}")
    private int orphanCleanupHours;

    @Scheduled(cron = "0 0 5 * * ?") // 매일 새벽 5시
    @Transactional
    public void cleanupDeletedFiles() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(cleanupDays);
        log.info("파일 정리 스케줄러 시작 - 기준 시간: {} ({}일 이전)", threshold, cleanupDays);

        int totalDeleted = 0;
        int totalFailed = 0;

        // 1. CourseReviewFile
        var result1 = cleanupFiles(
                reviewFileRepository.findByIsDeletedTrueAndDeletedAtBefore(threshold),
                CourseReviewFile::getFileUrl,
                reviewFileRepository::delete,
                "CourseReviewFile");
        totalDeleted += result1[0];
        totalFailed += result1[1];

        // 2. CourseReviewAttachment
        var result2 = cleanupFiles(
                reviewAttachmentRepository.findByIsDeletedTrueAndDeletedAtBefore(threshold),
                CourseReviewAttachment::getFileUrl,
                reviewAttachmentRepository::delete,
                "CourseReviewAttachment");
        totalDeleted += result2[0];
        totalFailed += result2[1];

        // 3. BoardAttach
        var result3 = cleanupFiles(
                boardAttachRepository.findByIsDeletedTrueAndDeletedAtBefore(threshold),
                BoardAttach::getRealFilename,
                boardAttachRepository::delete,
                "BoardAttach");
        totalDeleted += result3[0];
        totalFailed += result3[1];

        // 4. Attachment (Academy Q&A)
        var result4 = cleanupFiles(
                attachmentRepository.findByIsDeletedTrueAndDeletedAtBefore(threshold),
                Attachment::getFilename,
                attachmentRepository::delete,
                "Attachment");
        totalDeleted += result4[0];
        totalFailed += result4[1];

        // 5. CourseImage
        var result5 = cleanupFiles(
                courseImageRepository.findByIsDeletedTrueAndDeletedAtBefore(threshold),
                CourseImage::getImageUrl,
                courseImageRepository::delete,
                "CourseImage");
        totalDeleted += result5[0];
        totalFailed += result5[1];

        // 6. 고아 파일 정리 (임시 업로드 후 미확정된 파일)
        var result6 = cleanupOrphanedFiles();
        totalDeleted += result6[0];
        totalFailed += result6[1];

        log.info("파일 정리 완료 - 전체 성공: {}, 전체 실패: {}", totalDeleted, totalFailed);
    }

    /**
     * 고아 파일 정리 (임시 업로드 후 Q&A 생성을 완료하지 않은 파일)
     * - categoryId가 null인 파일 중 생성된 지 일정 시간이 지난 파일 삭제
     * - 사용자가 파일 업로드 후 글 작성을 취소하거나 세션이 만료된 경우 발생
     * 
     * @return [성공 개수, 실패 개수]
     */
    @Transactional
    public int[] cleanupOrphanedFiles() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(orphanCleanupHours);
        List<Attachment> orphanedFiles = attachmentRepository.findOrphanedFiles(threshold);

        log.info("[OrphanedFiles] 고아 파일 삭제 대상: {}건 ({}시간 이전 임시 업로드)", 
                orphanedFiles.size(), orphanCleanupHours);

        int deletedCount = 0;
        int failedCount = 0;

        for (Attachment file : orphanedFiles) {
            try {
                // 1. S3 파일 삭제
                String fileUrl = file.getFilename();
                if (fileUrl != null && !fileUrl.isBlank()) {
                    try {
                        s3Service.deleteFile(fileUrl);
                    } catch (Exception e) {
                        log.warn("[OrphanedFiles] S3 파일 삭제 실패 (또는 이미 없음) - URL: {}, Error: {}",
                                fileUrl, e.getMessage());
                    }
                }

                // 2. DB 영구 삭제
                attachmentRepository.delete(file);
                deletedCount++;

                log.debug("[OrphanedFiles] 고아 파일 삭제 완료 - id: {}, originName: {}", 
                        file.getId(), file.getOriginName());

            } catch (Exception e) {
                log.error("[OrphanedFiles] 고아 파일 삭제 실패 - id: {}", file.getId(), e);
                failedCount++;
            }
        }

        log.info("[OrphanedFiles] 고아 파일 정리 완료 - 성공: {}, 실패: {}", deletedCount, failedCount);
        return new int[] { deletedCount, failedCount };
    }

    /**
     * 범용 파일 정리 메서드
     * 
     * @param <T>          파일 엔티티 타입
     * @param files        삭제 대상 파일 목록
     * @param urlExtractor 파일 URL 추출 함수
     * @param deleteAction DB 삭제 함수
     * @param entityType   엔티티 타입명 (로깅용)
     * @return [성공 개수, 실패 개수]
     */
    private <T> int[] cleanupFiles(
            List<T> files,
            Function<T, String> urlExtractor,
            Consumer<T> deleteAction,
            String entityType) {
        int deletedCount = 0;
        int failedCount = 0;

        log.info("[{}] 삭제 대상: {}건", entityType, files.size());

        for (T file : files) {
            try {
                // 1. S3 파일 삭제
                String fileUrl = urlExtractor.apply(file);
                if (fileUrl != null && !fileUrl.isBlank()) {
                    try {
                        s3Service.deleteFile(fileUrl);
                    } catch (Exception e) {
                        // S3 파일이 이미 없는 경우 등 예외 처리 (로그만 남기고 DB 삭제 진행)
                        log.warn("[{}] S3 파일 삭제 실패 (또는 이미 없음) - URL: {}, Error: {}",
                                entityType, fileUrl, e.getMessage());
                    }
                }

                // 2. DB 영구 삭제
                deleteAction.accept(file);
                deletedCount++;

            } catch (Exception e) {
                log.error("[{}] 파일 영구 삭제 실패", entityType, e);
                failedCount++;
            }
        }

        log.info("[{}] 정리 완료 - 성공: {}, 실패: {}", entityType, deletedCount, failedCount);
        return new int[] { deletedCount, failedCount };
    }
}
