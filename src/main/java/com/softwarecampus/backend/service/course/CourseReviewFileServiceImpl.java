package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseReviewFile;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.ReviewFileResponse;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.exception.course.NotFoundException;
import com.softwarecampus.backend.repository.course.CourseReviewFileRepository;
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

    private final CourseReviewFileRepository reviewFileRepository;
    private final AccountRepository accountRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public ReviewFileResponse uploadReviewFile(CategoryType type, Long courseId, Long reviewId, Long userId, MultipartFile file) {
        // 업로더 조회
        Account uploader = accountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // DB에 placeholder로 저장
        CourseReviewFile reviewFile = CourseReviewFile.builder()
                .reviewId(reviewId)
                .uploader(uploader)
                .build();
        reviewFileRepository.save(reviewFile);

        try {
            // S3 업로드
            String url = s3Service.uploadFile(
                    file,
                    S3Folder.COURSE.getPath(),                  // COURSE 폴더 사용
                    FileType.FileTypeEnum.COURSE_IMAGE         // Enum COURSE_IMAGE 사용
            );
            reviewFile.setFileUrl(url);

        } catch (Exception e) {
            reviewFileRepository.delete(reviewFile);
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }

        return ReviewFileResponse.from(reviewFile);
    }

    @Override
    @Transactional
    public void deleteReviewFile(CategoryType type, Long courseId, Long reviewId, Long fileId, Long userId) {
        CourseReviewFile file = reviewFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("삭제할 파일이 존재하지 않습니다."));

        if (!file.getUploader().getId().equals(userId) && !isAdmin(userId)) {
            throw new ForbiddenException("본인 또는 관리자만 삭제할 수 있습니다.");
        }

        file.markDeleted();
    }

    @Override
    @Transactional
    public void restoreReviewFile(CategoryType type, Long courseId, Long reviewId, Long fileId, Long adminId) {
        if (!isAdmin(adminId)) throw new ForbiddenException("관리자만 복구할 수 있습니다.");

        CourseReviewFile file = reviewFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("복구할 파일이 존재하지 않습니다."));

        file.restore();
    }

    @Override
    @Transactional
    public void hardDeleteReviewFile(CategoryType type, Long courseId, Long reviewId, Long fileId, Long adminId) {
        if (!isAdmin(adminId)) throw new ForbiddenException("관리자만 첨부파일을 삭제할 수 있습니다.");

        CourseReviewFile file = reviewFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("삭제할 파일이 존재하지 않습니다."));

        reviewFileRepository.delete(file);
    }

    private boolean isAdmin(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        return account.getAccountType() == com.softwarecampus.backend.domain.common.AccountType.ADMIN;
    }
}
