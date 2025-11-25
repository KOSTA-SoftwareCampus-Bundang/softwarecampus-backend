package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.course.ReviewSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewSectionRepository extends JpaRepository<ReviewSection, Long> {

    List<ReviewSection> findByReviewIdAndIsDeletedFalse(Long reviewId);

    void deleteByReviewId(Long reviewId);
}
