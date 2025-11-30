package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.CourseImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseImageRepository extends JpaRepository<CourseImage, Long> {

    // 1) 이미지 단건 조회 (타입 + ID)
    Optional<CourseImage> findByIdAndCourse_Category_CategoryType(Long id, CategoryType type);

    // 2) courseId + type 기반 이미지 목록 조회
    List<CourseImage> findByCourse_IdAndCourse_Category_CategoryTypeAndIsDeletedFalse(
            Long courseId,
            CategoryType type);

    // (선택) 기존 코드 호환용: type 없이 조회하던 방식
    List<CourseImage> findByCourseIdAndIsDeletedFalse(Long courseId);

    // 스케줄러용: 삭제된 지 일정 기간이 지난 파일 조회
    List<CourseImage> findByIsDeletedTrueAndDeletedAtBefore(java.time.LocalDateTime threshold);
}
