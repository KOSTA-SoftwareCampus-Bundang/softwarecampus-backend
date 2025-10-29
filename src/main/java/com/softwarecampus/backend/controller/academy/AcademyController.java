package com.softwarecampus.backend.controller.academy;

import com.softwarecampus.backend.dto.academy.AcademyCreateRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.dto.academy.AcademyUpdateRequest;
import com.softwarecampus.backend.service.academy.AcademyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/academies")
@RequiredArgsConstructor
public class AcademyController {

    private final AcademyService academyService;

    /**
     * 훈련기관 등록
     */
    @PostMapping
    public ResponseEntity<AcademyResponse> createAcademy(@RequestBody AcademyCreateRequest request) {
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
     * 전체 조회
     */
    @GetMapping
    public ResponseEntity<List<AcademyResponse>> getAllAcademies() {
        List<AcademyResponse> academyResponse = academyService.getAllAcademies();
        return ResponseEntity.ok(academyResponse);
    }

    /**
     * 훈련기관 정보 수정
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AcademyResponse> updateAcademy(@PathVariable Long id, @RequestBody AcademyUpdateRequest request) {
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

    /**
     * 훈련기관 등록 승인
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<AcademyResponse> approveAcademy(@PathVariable Long id) {
        AcademyResponse academyResponse = academyService.approveAcademy(id);
        return ResponseEntity.ok(academyResponse);
    }

}
