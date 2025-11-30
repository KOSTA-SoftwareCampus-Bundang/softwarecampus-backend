package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseCurriculum;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 과정 상세 조회 응답 DTO
 * - 기본 정보 + 커리큘럼 정보 포함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDetailResponseDTO {

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

    // 상세 정보 추가
    private List<CurriculumDTO> curriculums;

    @Getter
    @Builder
    public static class CurriculumDTO {
        private int chapterNumber;
        private String chapterName;
        private String chapterDetail;
        private int chapterTime;

        public static CurriculumDTO from(CourseCurriculum entity) {
            return CurriculumDTO.builder()
                    .chapterNumber(entity.getChapterNumber())
                    .chapterName(entity.getChapterName())
                    .chapterDetail(entity.getChapterDetail())
                    .chapterTime(entity.getChapterTime())
                    .build();
        }
    }

    public static CourseDetailResponseDTO fromEntity(Course course) {
        return CourseDetailResponseDTO.builder()
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
                .curriculums(course.getCurriculums().stream()
                        .map(CurriculumDTO::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
