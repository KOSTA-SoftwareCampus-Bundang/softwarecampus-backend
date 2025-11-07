package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.course.CategoryType;
import com.softwarecampus.backend.domain.course.Course;
import com.softwarecampus.backend.domain.course.CourseCategory;
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

    private Long academyId;      // 어떤 기관의 과정인지

    private CategoryType categoryType; // 상위 카테고리 (EMPLOYEE, JOB_SEEKER)
    private String categoryName; // 하위 카테고리

    private String name;         // 과정명

    private LocalDate recruitStart;
    private LocalDate recruitEnd;
    private LocalDate courseStart;
    private LocalDate courseEnd;

    private Integer cost;
    private String classDay;
    private String location;

    private boolean isKdt;
    private boolean isNailbaeum;
    private boolean isOffline = true;

    private String requirement;

    /**
     * DTO → Entity 변환 (임시: academy/category 없이 생성 가능)
     */
    public Course toEntity() {
        return Course.builder()
                .name(name)
                .recruitStart(recruitStart)
                .recruitEnd(recruitEnd)
                .courseStart(courseStart)
                .courseEnd(courseEnd)
                .cost(cost)
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
