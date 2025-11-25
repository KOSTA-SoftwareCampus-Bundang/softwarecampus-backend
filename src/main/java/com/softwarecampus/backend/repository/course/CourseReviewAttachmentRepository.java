package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseReviewAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseReviewAttachmentRepository extends JpaRepository<CourseReviewAttachment, Long> {

    // 특정 리뷰의 첨부파일 목록
    List<CourseReviewAttachment> findByReviewIdAndIsDeletedFalse(Long reviewId);

    // Soft-Delete 되더라도 전체 조회 (관리자 복구용)
    List<CourseReviewAttachment> findByReviewId(Long reviewId);
}
