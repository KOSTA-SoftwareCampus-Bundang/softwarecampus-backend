package com.softwarecampus.backend.dto.course;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * 과정 등록/수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRequestDTO {

    @NotNull(message = "기관 ID는 필수입니다")
    private Long academyId;      // 어떤 기관의 과정인지

    @NotNull(message = "카테고리 타입은 필수입니다")
    private CategoryType categoryType; // 상위 카테고리 (EMPLOYEE, JOB_SEEKER)
    @NotBlank(message = "카테고리 이름은 필수입니다")
    private String categoryName; // 하위 카테고리
    @NotBlank(message = "과정명은 필수입니다")
    private String name;         // 과정명

    private LocalDate recruitStart;
    private LocalDate recruitEnd;
    private LocalDate courseStart;
    private LocalDate courseEnd;

    private Integer cost;

    /** 모집 정원 (기본값: 30) */
    @Builder.Default
    private Integer capacity = 30;
    
    @Builder.Default
    private String classDay = "평일"; // 기본값: 평일
    
    private String location;

    @JsonProperty("isKdt")
    private boolean isKdt;
    
    @JsonProperty("isNailbaeum")
    private boolean isNailbaeum;
    
    @JsonProperty("isOffline")
    @Builder.Default
    private boolean isOffline = true; // 기본값: 오프라인

    private String requirement;

    /**
     * DTO → Entity 변환 (테스트 전용 - 실제 저장 불가)
     * @deprecated 실제 저장 시에는 toEntity(Academy, CourseCategory)를 사용하세요
     */
    @Deprecated
    public Course toEntity() {
        return Course.builder()
                .name(name)
                .recruitStart(recruitStart)
                .recruitEnd(recruitEnd)
                .courseStart(courseStart)
                .courseEnd(courseEnd)
                .cost(cost)
                .capacity(capacity)
                .classDay(classDay)
                .location(location)
                .isKdt(isKdt)
                .isNailbaeum(isNailbaeum)
                .isOffline(isOffline)
                .requirement(requirement)
                .isApproved(ApprovalStatus.PENDING)
                .build();
    }

    /**
     * Academy, Category 연동 시 사용할 버전
     */
    public Course toEntity(Academy academy, CourseCategory category) {
        return Course.builder()
                .academy(academy)
                .category(category)
                .name(name)
                .recruitStart(recruitStart)
                .recruitEnd(recruitEnd)
                .courseStart(courseStart)
                .courseEnd(courseEnd)
                .cost(cost)
                .capacity(capacity)
                .classDay(classDay)
                .location(location)
                .isKdt(isKdt)
                .isNailbaeum(isNailbaeum)
                .isOffline(isOffline)
                .requirement(requirement)
                .isApproved(ApprovalStatus.PENDING)
                .build();
    }
}
