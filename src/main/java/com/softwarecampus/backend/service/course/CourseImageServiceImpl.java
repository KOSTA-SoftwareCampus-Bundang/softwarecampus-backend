package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseImage;
import com.softwarecampus.backend.repository.course.CourseImageRepository;
import com.softwarecampus.backend.repository.course.CourseRepository;
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

    private final CourseRepository courseRepository;
    private final CourseImageRepository courseImageRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public CourseImage uploadCourseImage(Long courseId, MultipartFile file, boolean isThumbnail) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        // 1) S3 업로드
        String url = s3Service.uploadFile(
                file,
                S3Folder.COURSE.getPath(),
                FileType.FileTypeEnum.COURSE_IMAGE
        );


        // 2) DB 저장
        CourseImage courseImage = CourseImage.builder()
                .course(course)
                .imageUrl(url)
                .isThumbnail(isThumbnail)
                .build();

        return courseImageRepository.save(courseImage);
    }

    @Override
    @Transactional
    public void deleteCourseImage(Long imageId) {
        CourseImage image = courseImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        image.markDeleted();
    }

    @Override
    public List<CourseImage> getCourseImages(Long courseId) {
        return courseImageRepository.findByCourseIdAndIsDeletedFalse(courseId);
    }
}
