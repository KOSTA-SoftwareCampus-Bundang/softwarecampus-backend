package com.softwarecampus.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기관용 대시보드 통계 응답 DTO
 * 작성일: 2025-12-02
 * 
 * 관리자용 DashboardStatsResponse와 분리하여
 * 기관 대시보드에 필요한 필드만 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionDashboardStatsResponse {
    /** 기관 소속 총 과정 수 */
    private long totalCourses;
    
    /** 승인 대기 중인 과정 수 */
    private long pendingCourses;
    
    /** 기관 과정에 달린 총 리뷰 수 */
    private long totalReviews;
    
    /** 승인 대기 중인 리뷰 수 */
    private long pendingReviews;
}
