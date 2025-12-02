package com.softwarecampus.backend.service.admin;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CourseReview;
import com.softwarecampus.backend.domain.course.ReviewLike.LikeType;
import com.softwarecampus.backend.dto.course.CourseReviewResponse;
import com.softwarecampus.backend.dto.course.ReviewAttachmentResponse;
import com.softwarecampus.backend.dto.course.ReviewSectionResponse;
import com.softwarecampus.backend.exception.course.NotFoundException;
import com.softwarecampus.backend.repository.course.CourseReviewRepository;
import com.softwarecampus.backend.repository.course.ReviewLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 관리자 후기 관리 서비스 구현
 * 작성일: 2025-12-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewAdminServiceImpl implements CourseReviewAdminService {

        private final CourseReviewRepository courseReviewRepository;
        private final ReviewLikeRepository reviewLikeRepository;

        @Override
        @Transactional
        public CourseReviewResponse approveReview(Long reviewId) {
                // 1. 후기 조회 (Soft Delete 준수)
                CourseReview review = courseReviewRepository.findByIdAndIsDeletedFalse(reviewId)
                                .orElseThrow(() -> new NotFoundException("후기를 찾을 수 없습니다."));

                // 2. 이미 승인된 후기인지 확인
                if (review.getApprovalStatus() == ApprovalStatus.APPROVED) {
                        log.warn("이미 승인된 후기입니다. reviewId: {}", reviewId);
                        throw new IllegalStateException("이미 승인된 후기입니다.");
                }

                // 3. 승인 상태로 변경
                review.setApprovalStatus(ApprovalStatus.APPROVED);
                log.info("후기 승인 완료. reviewId: {}", reviewId);

                // 4. 응답 DTO 생성
                return buildReviewResponse(review);
        }

        @Override
        @Transactional
        public CourseReviewResponse rejectReview(Long reviewId, String reason) {
                // 1. 후기 조회 (Soft Delete 준수)
                CourseReview review = courseReviewRepository.findByIdAndIsDeletedFalse(reviewId)
                                .orElseThrow(() -> new NotFoundException("후기를 찾을 수 없습니다."));

                // 2. 거부 상태로 변경
                review.setApprovalStatus(ApprovalStatus.REJECTED);
                log.info("후기 거부 완료. reviewId: {}, reason: {}", reviewId, reason);

                // 3. 응답 DTO 생성
                return buildReviewResponse(review);
        }

        /**
         * CourseReview 엔티티를 CourseReviewResponse DTO로 변환
         */
        private CourseReviewResponse buildReviewResponse(CourseReview review) {
                // 좋아요/싫어요 수 계산
                int likeCount = reviewLikeRepository.countByReviewIdAndType(review.getId(), LikeType.LIKE);
                int dislikeCount = reviewLikeRepository.countByReviewIdAndType(review.getId(), LikeType.DISLIKE);

                return CourseReviewResponse.builder()
                                .reviewId(review.getId())
                                .writerId(review.getWriter().getId())
                                .writerName(review.getWriter().getUserName())
                                .courseId(review.getCourse().getId())
                                .courseName(review.getCourse().getName())
                                .comment(review.getComment())
                                .approvalStatus(review.getApprovalStatus().name())
                                .rejectionReason(review.getRejectionReason())
                                .averageScore(review.calculateAverageScore())
                                .sections(review.getSections().stream()
                                                .filter(section -> !section.getIsDeleted())
                                                .map(section -> ReviewSectionResponse.builder()
                                                                .sectionType(section.getSectionType().name())
                                                                .score(section.getScore())
                                                                .comment(section.getComment())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .attachments(review.getAttachments().stream()
                                                .filter(attachment -> !attachment.getIsDeleted())
                                                .map(attachment -> ReviewAttachmentResponse.builder()
                                                                .attachmentId(attachment.getId())
                                                                .fileUrl(attachment.getFileUrl())
                                                                .originalName(attachment.getOriginalName())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .likeCount(likeCount)
                                .dislikeCount(dislikeCount)
                                .myLikeType("NONE") // 관리자 API에서는 특정 사용자의 좋아요 상태가 필요없음
                                .createdAt(review.getCreatedAt())
                                .build();
        }
}
