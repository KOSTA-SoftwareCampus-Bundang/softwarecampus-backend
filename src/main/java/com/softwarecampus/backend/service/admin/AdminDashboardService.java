package com.softwarecampus.backend.service.admin;

import com.softwarecampus.backend.dto.admin.DashboardStatsResponse;
import com.softwarecampus.backend.dto.admin.InstitutionDashboardStatsResponse;

/**
 * 관리자 대시보드 서비스 인터페이스
 * 작성일: 2025-12-02
 * 
 * 대시보드 통계 조회 로직을 서비스 계층으로 분리하여
 * Controller-Service-Repository 레이어 규칙을 준수
 */
public interface AdminDashboardService {

    /**
     * 관리자 대시보드 통계 조회
     * 
     * @return 대시보드 통계 응답 DTO
     */
    DashboardStatsResponse getDashboardStats();

    /**
     * 기관 대시보드 통계 조회
     * 
     * @param academyId 기관 ID
     * @return 기관용 대시보드 통계 응답 DTO
     */
    InstitutionDashboardStatsResponse getInstitutionDashboardStats(Long academyId);
}
