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

    /**
     * 계정 ID로 기관 ID 조회
     * 
     * @param accountId 계정 ID
     * @return 기관 ID
     * @throws IllegalArgumentException 사용자를 찾을 수 없거나 기관 정보가 없는 경우
     */
    Long getAcademyIdByAccountId(Long accountId);
}
