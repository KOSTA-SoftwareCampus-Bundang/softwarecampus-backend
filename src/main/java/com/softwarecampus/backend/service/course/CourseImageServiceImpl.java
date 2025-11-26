package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseImage;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.course.CourseImageResponse;
import com.softwarecampus.backend.exception.course.ForbiddenException;
import com.softwarecampus.backend.exception.course.NotFoundException;
import com.softwarecampus.backend.repository.course.CourseImageRepository;
import com.softwarecampus.backend.repository.course.CourseRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.service.common.FileType;
import com.softwarecampus.backend.service.common.S3Folder;
import com.softwarecampus.backend.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseImageServiceImpl implements CourseImageService {

    private final AccountRepository accountRepository;
    private final CourseRepository courseRepository;
    private final CourseImageRepository courseImageRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public CourseImageResponse uploadCourseImage(
            CategoryType type,
            Long courseId,
            MultipartFile file,
            boolean isThumbnail
    ) {
        // 1) Course 검증
        Course course = courseRepository.findByIdAndCategory_CategoryType(courseId, type)
                .orElseThrow(() -> new IllegalArgumentException("Course not found for type: " + type));

        // 2) DB에 placeholder로 저장 (URL 없음)
        CourseImage image = courseImageRepository.save(
                CourseImage.builder()
                        .course(course)
                        .isThumbnail(isThumbnail)
                        .build()
        );

        try {
            // 3) S3 업로드
            String url = s3Service.uploadFile(
                    file,
                    S3Folder.COURSE.getPath(),
                    FileType.FileTypeEnum.COURSE_IMAGE
            );

            // 4) 업로드 성공 시 DB 레코드 URL 업데이트
            image.setImageUrl(url);

        } catch (Exception e) {
            // 5) 업로드 실패 시 placeholder 삭제
            courseImageRepository.delete(image);
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }

        return CourseImageResponse.from(image);
    }


    @Override
    @Transactional
    public void deleteCourseImage(CategoryType type, Long imageId) {
        // type 검증
        CourseImage image = courseImageRepository.findByIdAndCourse_Category_CategoryType(imageId, type)
                .orElseThrow(() -> new IllegalArgumentException("Image not found for type: " + type));

        // soft delete
        image.markDeleted();
    }

    @Override
    public List<CourseImageResponse> getCourseImages(CategoryType type, Long courseId) {
        List<CourseImage> images =
                courseImageRepository.findByCourse_IdAndCourse_Category_CategoryTypeAndIsDeletedFalse(courseId, type);

        return images.stream()
                .map(CourseImageResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void hardDeleteCourseImage(CategoryType type, Long imageId, Long adminId) {
        // 1. 이미지 조회
        CourseImage image = courseImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("삭제할 이미지가 존재하지 않습니다."));

        // 2. 관리자 권한 체크
        Account admin = accountRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("관리자를 찾을 수 없습니다."));
        if (admin.getAccountType() != AccountType.ADMIN) {
            throw new ForbiddenException("관리자만 첨부파일을 삭제할 수 있습니다.");
        }

        // 3. Hard Delete
        courseImageRepository.delete(image);
    }
}
