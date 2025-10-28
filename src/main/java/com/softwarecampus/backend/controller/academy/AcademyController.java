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

    private AcademyService academyService;

    /**
     * 훈련기관 등록
     */
    @PostMapping
    public ResponseEntity<AcademyResponse> createAcademy(@RequestBody AcademyCreateRequest request) {
        AcademyResponse academyResponse = academyService.createAcademy(request);
        return ResponseEntity.ok(academyResponse);
    }

    /**
     * 단일 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<AcademyResponse> getAcademyById(@PathVariable Long id) {
        AcademyResponse academyResponse = academyService.getAcademyById(id);
        return ResponseEntity.ok(academyResponse);
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
    @PutMapping("/{id}")
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

}
