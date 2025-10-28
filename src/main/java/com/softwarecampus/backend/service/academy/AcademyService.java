package com.softwarecampus.backend.service.academy;

import com.softwarecampus.backend.dto.academy.AcademyCreateRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.dto.academy.AcademyUpdateRequest;

import java.util.List;

public interface AcademyService {
    // 등록
    AcademyResponse createAcademy(AcademyCreateRequest request);

    // 단일 조회
    AcademyResponse getAcademyById(Long id);

    // 목록 조회
    List<AcademyResponse> getAllAcademies();

    // 정보 수정
    AcademyResponse updateAcademy(Long id, AcademyUpdateRequest request);

    // 삭제
    void deleteAcademy(Long id);

    // 승인 처리

}
