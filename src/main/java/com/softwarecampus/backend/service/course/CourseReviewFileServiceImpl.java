package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CourseReview;
import com.softwarecampus.backend.domain.course.CourseReviewFile;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.ReviewFileResponse;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.exception.course.NotFoundException;
import com.softwarecampus.backend.repository.course.CourseReviewFileRepository;
import com.softwarecampus.backend.repository.course.CourseReviewRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Folder;
import com.softwarecampus.backend.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewFileServiceImpl implements CourseReviewFileService {

    private final CourseReviewRepository courseReviewRepository;
    private final CourseReviewFileRepository reviewFileRepository;
    private final AccountRepository accountRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public ReviewFileResponse uploadReviewFile(Long courseId, Long reviewId, Long userId, MultipartFile file) {
        // 1 파일 null/empty 체크
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        // 2 업로더 조회
        Account uploader = accountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 3 리뷰 존재 및 정합성 검증
        CourseReview review = courseReviewRepository.findByIdAndCourseIdAndIsDeletedFalse(reviewId, courseId)
                .orElseThrow(() -> new NotFoundException("해당 리뷰가 존재하지 않거나 코스/카테고리가 일치하지 않습니다."));

        // 4 DB에 placeholder로 저장
        CourseReviewFile reviewFile = CourseReviewFile.builder()
                .reviewId(review.getId())
                .uploader(uploader)
                .build();
        reviewFileRepository.save(reviewFile);

        try {
            // 5 S3 업로드
            String url = s3Service.uploadFile(
                    file,
                    S3Folder.COURSE.getPath(),
                    FileType.FileTypeEnum.COURSE_IMAGE);
            reviewFile.setFileUrl(url);

        } catch (Exception e) {
            // reviewFileRepository.delete(reviewFile); // 트랜잭션 롤백으로 인해 불필요
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }

        return ReviewFileResponse.from(reviewFile);
    }

    @Override
    @Transactional
    public void deleteReviewFile(Long courseId, Long reviewId, Long fileId, Long userId) {
        // 0️⃣ 코스 정합성 검증
        courseReviewRepository.findByIdAndCourseIdAndIsDeletedFalse(reviewId, courseId)
                .orElseThrow(() -> new NotFoundException("해당 리뷰가 존재하지 않거나 코스/카테고리가 일치하지 않습니다."));

        // 1️⃣ 파일 조회
        CourseReviewFile file = reviewFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("삭제할 파일이 존재하지 않습니다."));

        // 이미 삭제된 파일인지 검증
        if (file.getIsDeleted()) {
            throw new NotFoundException("이미 삭제된 파일입니다.");
        }

        // 2️⃣ 요청한 reviewId에 속한 파일인지 검증
        if (!file.getReviewId().equals(reviewId)) {
            throw new NotFoundException("요청한 리뷰에 속한 파일이 아닙니다.");
        }

        // 3️⃣ 권한 체크: 업로더 본인 또는 관리자
        if (!file.getUploader().getId().equals(userId) && !isAdmin(userId)) {
            throw new ForbiddenException("본인 또는 관리자만 삭제할 수 있습니다.");
        }

        // 4️⃣ 소프트 삭제 처리
        file.markDeleted();
    }

    @Override
    @Transactional
    public void restoreReviewFile(Long courseId, Long reviewId, Long fileId, Long adminId) {
        // 1️⃣ 관리자 권한 체크
        if (!isAdmin(adminId))
            throw new ForbiddenException("관리자만 복구할 수 있습니다.");

        // 0️⃣ 코스 정합성 검증
        courseReviewRepository.findByIdAndCourseIdAndIsDeletedFalse(reviewId, courseId)
                .orElseThrow(() -> new NotFoundException("해당 리뷰가 존재하지 않거나 코스/카테고리가 일치하지 않습니다."));

        // 2️⃣ 파일 조회
        CourseReviewFile file = reviewFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("복구할 파일이 존재하지 않습니다."));

        // 삭제되지 않은 파일인지 검증
        if (!file.getIsDeleted()) {
            throw new IllegalStateException("삭제되지 않은 파일은 복구할 수 없습니다.");
        }

        // 3️⃣ 요청한 reviewId에 속한 파일인지 검증
        if (!file.getReviewId().equals(reviewId)) {
            throw new NotFoundException("요청한 리뷰에 속한 파일이 아닙니다.");
        }

        // 4️⃣ 복구 처리
        file.restore();
    }

    @Override
    @Transactional
    public void hardDeleteReviewFile(Long courseId, Long reviewId, Long fileId, Long adminId) {
        // 1️⃣ 관리자 권한 체크
        if (!isAdmin(adminId))
            throw new ForbiddenException("관리자만 첨부파일을 삭제할 수 있습니다.");

        // 0️⃣ 코스 정합성 검증
        courseReviewRepository.findByIdAndCourseIdAndIsDeletedFalse(reviewId, courseId)
                .orElseThrow(() -> new NotFoundException("해당 리뷰가 존재하지 않거나 코스/카테고리가 일치하지 않습니다."));

        // 2️⃣ 파일 조회
        CourseReviewFile file = reviewFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("삭제할 파일이 존재하지 않습니다."));

        // 삭제되지 않은 파일인지 검증 (hard delete는 soft delete 후에만)
        if (!file.getIsDeleted()) {
            throw new IllegalStateException("삭제되지 않은 파일입니다. 먼저 소프트 삭제를 수행하세요.");
        }

        // 3️⃣ 요청한 reviewId에 속한 파일인지 검증
        if (!file.getReviewId().equals(reviewId)) {
            throw new NotFoundException("요청한 리뷰에 속한 파일이 아닙니다.");
        }

        // 4️⃣ 소프트 삭제 (관리자도 Soft Delete 정책 준수)
        file.markDeleted();
    }

    private boolean isAdmin(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        return account.getAccountType() == com.softwarecampus.backend.domain.common.AccountType.ADMIN;
    }
}
