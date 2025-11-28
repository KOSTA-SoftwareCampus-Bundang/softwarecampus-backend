/**
 * 관리자 컨트롤러
 * 작성자: GitHub Copilot
 * 작성일: 2025-11-28
 */
package com.softwarecampus.backend.controller.admin;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.academy.AcademyRejectRequest;
import com.softwarecampus.backend.dto.academy.AcademyResponse;
import com.softwarecampus.backend.service.user.email.EmailSendService;
import com.softwarecampus.backend.service.academy.AcademyService;
import com.softwarecampus.backend.service.academy.AcademyFileService;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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

    private final AccountRepository accountRepository;
    private final EmailSendService emailSendService;
    // 기관 관리 서비스 (작성자: GitHub Copilot, 작성일: 2025-11-28)
    private final AcademyService academyService;
    private final AcademyFileService academyFileService;

    /**
     * 회원 승인
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @PatchMapping("/accounts/{accountId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<AccountResponse> approveAccount(@PathVariable Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. ID: " + accountId));
        
        account.setAccountApproved(ApprovalStatus.APPROVED);
        
        // 승인 완료 이메일 발송
        emailSendService.sendAccountApprovalEmail(
            account.getEmail(),
            account.getUserName()
        );
        
        return ResponseEntity.ok(new AccountResponse(
            account.getId(),
            account.getEmail(),
            account.getUserName(),
            account.getPhoneNumber(),
            account.getAccountType(),
            account.getAccountApproved(),
            account.getAddress(),
            account.getAffiliation(),
            account.getPosition()
        ));
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
