/**
 * 관리자 컨트롤러
 * 작성자: GitHub Copilot
 * 작성일: 2025-11-28
 */
package com.softwarecampus.backend.controller.admin;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.academy.AcademyRejectRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.service.admin.AccountAdminService;
import com.softwarecampus.backend.service.academy.AcademyService;
import com.softwarecampus.backend.service.academy.AcademyFileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 관리자 전용 API
 * - 회원 승인/거절
 * - 기관 승인/거절 (AcademyController에 구현됨)
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AccountAdminService accountAdminService;
    // 기관 관리 서비스 (작성자: GitHub Copilot, 작성일: 2025-11-28)
    private final AcademyService academyService;
    private final AcademyFileService academyFileService;

    /**
     * 회원 승인
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     * 수정일: 2025-11-29 - 비즈니스 로직을 서비스 계층으로 이동
     */
    @PatchMapping("/accounts/{accountId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> approveAccount(@PathVariable Long accountId) {
        AccountResponse response = accountAdminService.approveAccount(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * 훈련기관 등록 승인
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @PatchMapping("/academies/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademyResponse> approveAcademy(@PathVariable Long id) {
        AcademyResponse academyResponse = academyService.approveAcademy(id);
        return ResponseEntity.ok(academyResponse);
    }

    /**
     * 훈련기관 등록 거절
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @PatchMapping("/academies/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademyResponse> rejectAcademy(
            @PathVariable Long id,
            @Valid @RequestBody AcademyRejectRequest request) {
        AcademyResponse academyResponse = academyService.rejectAcademy(id, request.getReason());
        return ResponseEntity.ok(academyResponse);
    }

    /**
     * 기관 첨부파일 다운로드
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @GetMapping("/academies/{academyId}/files/{fileId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> downloadAcademyFile(
            @PathVariable Long academyId,
            @PathVariable Long fileId) {
        String presignedUrl = academyFileService.getFileUrl(fileId);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(presignedUrl))
            .build();
    }
}
