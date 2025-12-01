package com.softwarecampus.backend.controller.academy;

import com.softwarecampus.backend.dto.academy.AcademyCreateRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.dto.academy.AcademyUpdateRequest;
import com.softwarecampus.backend.service.academy.AcademyFileService;
import com.softwarecampus.backend.service.academy.AcademyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academies")
@RequiredArgsConstructor
public class AcademyController {

    private final AcademyService academyService;
    // 파일 서비스 (작성자: GitHub Copilot, 작성일: 2025-11-28)
    private final AcademyFileService academyFileService;

    /**
     * 훈련기관 등록 (파일 업로드 포함)
     * 수정자: GitHub Copilot
     * 수정일: 2025-11-28
     * 수정 내용: Multipart/form-data 지원, 재직증명서 파일 업로드 기능 추가
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AcademyResponse> createAcademy(
            @Valid @ModelAttribute AcademyCreateRequest request) {
        AcademyResponse academyResponse = academyService.createAcademy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(academyResponse);
    }

    /**
     * 훈련기관 이름으로 부분 일치 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<AcademyResponse>> searchAcademiesByName(@RequestParam String name) {
        List<AcademyResponse> response = academyService.searchAcademiesByName(name);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 조회 (훈련기관 이름만)
     */
    @GetMapping
    public ResponseEntity<List<AcademyResponse>> getAllAcademies() {
        List<AcademyResponse> academyResponse = academyService.getAllAcademyNames();
        return ResponseEntity.ok(academyResponse);
    }

    /**
     * 훈련기관 상세 정보 조회
     */
    @GetMapping("/{academyId}")
    public ResponseEntity<AcademyResponse> getAcademyDetails(@PathVariable Long academyId) {
        AcademyResponse academyDetails = academyService.getAcademyDetails(academyId);
        return ResponseEntity.ok(academyDetails);
    }

    /**
     * 훈련기관 정보 수정
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AcademyResponse> updateAcademy(@PathVariable Long id,
            @RequestBody AcademyUpdateRequest request) {
        AcademyResponse academyResponse = academyService.updateAcademy(id, request);
        return ResponseEntity.ok(academyResponse);
    }

    /**
     * 훈련기관 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAcademy(@PathVariable Long id) {
        academyService.deleteAcademy(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
