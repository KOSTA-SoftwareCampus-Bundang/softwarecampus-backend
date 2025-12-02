package com.softwarecampus.backend.repository.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;

import com.softwarecampus.backend.domain.course.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

        // 과정별 리뷰 목록
        List<CourseReview> findByCourseIdAndIsDeletedFalse(Long courseId);

        // 과정별 리뷰 목록 (Pageable)
        Page<CourseReview> findByCourseIdAndIsDeletedFalse(Long courseId, Pageable pageable);

        // 승인된 리뷰만 조회
        List<CourseReview> findByCourseIdAndApprovalStatusAndIsDeletedFalse(Long courseId, ApprovalStatus status);

        // 승인된 리뷰만 조회 (Pageable)
        Page<CourseReview> findByCourseIdAndApprovalStatusAndIsDeletedFalse(Long courseId, ApprovalStatus status,
                        Pageable pageable);

        // 특정 유저가 특정 과정에 작성한 리뷰 (중복 작성 방지)
        Optional<CourseReview> findByWriterIdAndCourseIdAndIsDeletedFalse(Long accountId, Long courseId);

        // 유저 전체 리뷰 조회
        List<CourseReview> findByWriterIdAndIsDeletedFalse(Long accountId);

        /**
         * courseId 기준으로 삭제되지 않은 리뷰 전체 조회
         */
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "writer", "sections", "attachments",
                        "likes" })
        List<CourseReview> findAllByCourse_IdAndIsDeletedFalse(Long courseId);

        Optional<CourseReview> findByIdAndCourseIdAndIsDeletedFalse(Long reviewId, Long courseId);

        // ID로 삭제되지 않은 리뷰 조회 (관리자용) - N+1 문제 방지를 위해 연관 엔티티 함께 조회
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "writer", "course" })
        Optional<CourseReview> findByIdAndIsDeletedFalse(Long id);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "writer", "sections", "attachments",
                        "likes" })
        Optional<CourseReview> findWithDetailsByIdAndIsDeletedFalse(Long id);

        /**
         * 관리자용 리뷰 검색 (상태별, 검색어별)
         */
        @Query("SELECT r FROM CourseReview r " +
                        "WHERE r.deletedAt IS NULL " +
                        "AND (:status IS NULL OR r.approvalStatus = :status) " +
                        "AND (:keyword IS NULL OR LOWER(r.comment) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(r.writer.userName) LIKE CONCAT('%', LOWER(:keyword), '%'))")
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "writer", "course" })
        Page<CourseReview> searchAdminReviews(@Param("status") ApprovalStatus status,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        long countByDeletedAtIsNull();

        long countByApprovalStatusAndDeletedAtIsNull(ApprovalStatus status);

        /**
         * 기관용 리뷰 검색 (상태별, 검색어별) - 본인 기관의 과정에 달린 리뷰만 조회
         */
        @Query("SELECT r FROM CourseReview r " +
                        "WHERE r.deletedAt IS NULL " +
                        "AND r.course.academy.id = :academyId " +
                        "AND (:status IS NULL OR r.approvalStatus = :status) " +
                        "AND (:keyword IS NULL OR LOWER(r.comment) LIKE CONCAT('%', LOWER(:keyword), '%') OR LOWER(r.writer.userName) LIKE CONCAT('%', LOWER(:keyword), '%'))")
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "writer", "course" })
        Page<CourseReview> searchInstitutionReviews(@Param("academyId") Long academyId,
                        @Param("status") ApprovalStatus status,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        // 기관별 리뷰 수 (삭제되지 않은 리뷰만)
        @Query("SELECT COUNT(r) FROM CourseReview r WHERE r.deletedAt IS NULL AND r.course.academy.id = :academyId")
        long countByAcademyIdAndDeletedAtIsNull(@Param("academyId") Long academyId);

        // 기관별 승인 대기 리뷰 수 (삭제되지 않은 리뷰만)
        @Query("SELECT COUNT(r) FROM CourseReview r WHERE r.deletedAt IS NULL AND r.course.academy.id = :academyId AND r.approvalStatus = :status")
        long countByAcademyIdAndApprovalStatusAndDeletedAtIsNull(@Param("academyId") Long academyId,
                        @Param("status") ApprovalStatus status);
}
