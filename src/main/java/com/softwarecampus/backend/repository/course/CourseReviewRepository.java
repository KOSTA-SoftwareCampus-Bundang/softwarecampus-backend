package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    /** ✅ 특정 과정에 승인된 리뷰 목록 조회 */
    List<CourseReview> findByCourse_IdAndReviewApproved(Long courseId, ApprovalStatus reviewApproved);

    /** ✅ 특정 사용자 소유의 리뷰 단건 조회 (수정/삭제용) */
    Optional<CourseReview> findByIdAndAccount_Id(Long reviewId, Long accountId);

    /** ✅ 특정 과정의 리뷰 개수 */
    long countByCourse_IdAndReviewApproved(Long courseId, ApprovalStatus reviewApproved);
}
