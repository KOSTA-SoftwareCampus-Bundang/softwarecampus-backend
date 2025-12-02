package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 과정 조회 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponseDTO {

    private Long id;
    private String name;
    private Long academyId;
    private String academyName;
    private Long categoryId;
    private String categoryName;
    private CategoryType categoryType;

    private LocalDate recruitStart;
    private LocalDate recruitEnd;
    private LocalDate courseStart;
    private LocalDate courseEnd;

    private Integer cost;
    private String classDay;
    private String location;

    private boolean isKdt;
    private boolean isNailbaeum;
    private boolean isOffline;

    private String requirement;

    private ApprovalStatus approvalStatus;
    private LocalDateTime approvedAt;

    private Double rating;
    private Integer reviewCount;

    // 과정 등록자 정보
    private Long requesterId;
    private String requesterName;

    /**
     * Entity → DTO 변환
     */
    public static CourseResponseDTO fromEntity(Course course) {
        // 평점 계산 (삭제되지 않은 승인된 리뷰들의 평균)
        double rating = 0.0;
        int reviewCount = 0;

        if (course.getReviews() != null) {
            var activeReviews = course.getReviews().stream()
                    .filter(r -> r.isActive() && r.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .toList();

            reviewCount = activeReviews.size();

            if (reviewCount > 0) {
                rating = activeReviews.stream()
                        .mapToDouble(com.softwarecampus.backend.domain.course.CourseReview::calculateAverageScore)
                        .average()
                        .orElse(0.0);
                // 소수점 1자리 반올림
                rating = Math.round(rating * 10.0) / 10.0;
            }
        }

        return CourseResponseDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .academyId(course.getAcademy() != null ? course.getAcademy().getId() : null)
                .academyName(course.getAcademy() != null ? course.getAcademy().getName() : null)
                .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                .categoryName(course.getCategory() != null ? course.getCategory().getCategoryName() : null)
                .categoryType(course.getCategory() != null ? course.getCategory().getCategoryType() : null)
                .recruitStart(course.getRecruitStart())
                .recruitEnd(course.getRecruitEnd())
                .courseStart(course.getCourseStart())
                .courseEnd(course.getCourseEnd())
                .cost(course.getCost())
                .classDay(course.getClassDay())
                .location(course.getLocation())
                .isKdt(course.isKdt())
                .isNailbaeum(course.isNailbaeum())
                .isOffline(course.isOffline())
                .requirement(course.getRequirement())
                .approvalStatus(course.getIsApproved())
                .approvedAt(course.getApprovedAt())
                .rating(rating)
                .reviewCount(reviewCount)
                .requesterId(course.getRequester() != null ? course.getRequester().getId() : null)
                .requesterName(course.getRequester() != null ? course.getRequester().getUserName() : null)
                .build();
    }
}
