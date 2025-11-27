package com.softwarecampus.backend.dto.home;

import lombok.*;

import java.util.List;

/**
 * 메인페이지 - 전체 응답 DTO
 * 모든 섹션 데이터를 하나의 응답으로 반환
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeResponseDTO {

    /**
     * 재직자 베스트 과정
     */
    private List<HomeCourseDTO> employeeBest;

    /**
     * 취업예정자 베스트 과정
     */
    private List<HomeCourseDTO> jobSeekerBest;

    /**
     * 마감 임박 과정
     */
    private List<HomeCourseDTO> closingSoon;
}
