package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseImage;
import com.softwarecampus.backend.domain.course.CourseImageType;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
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
            CourseImageType imageType) {
        // 1) Course 검증
        Course course = courseRepository.findByIdAndCategory_CategoryType(courseId, type)
                .orElseThrow(() -> new IllegalArgumentException("Course not found for type: " + type));

        // 2) THUMBNAIL 또는 HEADER 타입인 경우, 기존 동일 타입 이미지를 CONTENT로 변경 (1개만 유지)
        if (imageType == CourseImageType.THUMBNAIL || imageType == CourseImageType.HEADER) {
            final CourseImageType targetType = imageType;
            course.getImages().stream()
                    .filter(img -> img.isActive() && img.getImageType() == targetType)
                    .forEach(img -> img.setImageType(CourseImageType.CONTENT));
        }

        // 3) S3 업로드 먼저 수행
        String url;
        try {
            url = s3Service.uploadFile(
                    file,
                    S3Folder.COURSE.getPath(),
                    FileType.FileTypeEnum.COURSE_IMAGE);
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }

        // 4) 업로드 성공 후 CourseImage 엔티티 생성
        CourseImage image = CourseImage.builder()
                .imageUrl(url)
                .originalFilename(file.getOriginalFilename())
                .imageType(imageType)
                .isThumbnail(imageType == CourseImageType.THUMBNAIL) // 하위 호환
                .build();

        // 5) 양방향 관계 설정 및 저장 (편의 메서드 사용)
        course.addImage(image);
        courseImageRepository.save(image);

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
        List<CourseImage> images = courseImageRepository
                .findByCourse_IdAndCourse_Category_CategoryTypeAndIsDeletedFalse(courseId, type);

        return images.stream()
                .map(CourseImageResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void hardDeleteCourseImage(CategoryType type, Long imageId) {
        // 1. 현재 로그인한 사용자 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new ForbiddenException("권한이 없습니다.");
        }

        // 2. UserDetails에서 이메일 가져오기
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername(); // loadUserByUsername에서 설정한 username = email

        // 3. 이메일로 Account 조회 (Soft Delete 제외)
        Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 4. 관리자 권한 체크
        if (account.getAccountType() != AccountType.ADMIN) {
            throw new ForbiddenException("관리자만 첨부파일을 삭제할 수 있습니다.");
        }

        // 5. 이미지 조회
        CourseImage image = courseImageRepository.findByIdAndCourse_Category_CategoryType(imageId, type)
                .orElseThrow(() -> new NotFoundException("해당 타입에 삭제할 이미지가 존재하지 않습니다."));

        // 6. S3 파일 삭제
        if (image.getImageUrl() != null && !image.getImageUrl().isEmpty()) {
            try {
                s3Service.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                // 로깅만 하고 DB 삭제 진행
                log.warn("S3 파일 삭제 실패 (imageId: {}, url: {}): {}",
                        imageId, image.getImageUrl(), e.getMessage());
            }
        }

        // 7. Hard Delete (DB)
        courseImageRepository.delete(image);
    }
}
