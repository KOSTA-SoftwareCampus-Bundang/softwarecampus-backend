package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.CourseReviewFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseReviewFileRepository extends JpaRepository<CourseReviewFile, Long> {

    List<CourseReviewFile> findByReviewIdAndIsDeletedFalse(Long reviewId);

    Optional<CourseReviewFile> findByIdAndReviewId(Long fileId, Long reviewId);
}
