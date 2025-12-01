package com.softwarecampus.backend.service.academy;

import com.softwarecampus.backend.dto.academy.AcademyCreateRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.dto.academy.AcademyUpdateRequest;

import java.util.List;

public interface AcademyService {
    // 등록
    AcademyResponse createAcademy(AcademyCreateRequest request);

    // 훈련기관 이름으로 조회
    List<AcademyResponse> searchAcademiesByName(String name);

    // 전체 목록 조회 (이름만 보내줌)
    List<AcademyResponse> getAllAcademyNames();

    // 훈련기관 상세 정보 조회
    AcademyResponse getAcademyDetails(Long id);

    // 정보 수정
    AcademyResponse updateAcademy(Long id, AcademyUpdateRequest request);

    // 삭제
    void deleteAcademy(Long id);

    // 승인 처리
    AcademyResponse approveAcademy(Long id);

    /**
     * 기관 거절 처리
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     * 
     * @param id     기관 ID
     * @param reason 거절 사유
     * @return 거절 처리된 기관 정보
     */
    AcademyResponse rejectAcademy(Long id, String reason);

    /**
     * 관리자용 기관 목록 조회 (검색, 필터링, 페이징)
     */
    org.springframework.data.domain.Page<AcademyResponse> getAdminAcademies(
            com.softwarecampus.backend.domain.common.ApprovalStatus status,
            String keyword,
            org.springframework.data.domain.Pageable pageable);
}
