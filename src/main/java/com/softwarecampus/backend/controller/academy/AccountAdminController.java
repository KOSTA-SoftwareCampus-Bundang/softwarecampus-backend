package com.softwarecampus.backend.controller.academy;

import com.softwarecampus.backend.dto.academy.qna.QAFileDetail;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.AccountUpdateRequest;
import com.softwarecampus.backend.service.admin.AccountAdminService;
import com.softwarecampus.backend.service.admin.QAAttachmentAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/accounts")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AccountAdminController {

    private final AccountAdminService accountAdminService;
    private final QAAttachmentAdminService qAAttachmentAdminService;

    /**
     *  회원 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<AccountResponse>> getAccountList(@PageableDefault(size=20) Pageable pageable){
        // Pageable은 Spring Data가 자동으로 쿼리 파라미터에서 생성합니다.
        Page<AccountResponse> response = accountAdminService.getAllActiveAccounts(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     *  회원 목록 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<AccountResponse>> searchAccountList(@RequestParam(required = false) String keyword,
                                                                   @PageableDefault(size=20) Pageable pageable){
        Page<AccountResponse> response = accountAdminService.searchAccounts(keyword, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     *  회원 상세 정보 조회
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountDetail(@PathVariable @Positive Long accountId) {
        AccountResponse response = accountAdminService.getAccountDetail(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     *  회원 정보 수정
     */
    @PutMapping("/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable @Positive Long accountId,
                                                         @RequestBody @Valid AccountUpdateRequest request) {
        AccountResponse response = accountAdminService.updateAccount(accountId, request);
        return ResponseEntity.ok(response);
    }

    /**
     *  회원 삭제
     */
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable @Positive Long accountId) {
        accountAdminService.deleteAccount(accountId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     *  특정 Q/A 게시글에 연결된 삭제된 파일 목록 조회
     */
    @GetMapping("/deleted")
    public ResponseEntity<List<QAFileDetail>> getSoftDeletedFilesByQaId(@RequestParam Long qaId) {
        log.info("관리자: Q/A ID {} 목록 조회 요청.", qaId);

        List<QAFileDetail> fileDetails = qAAttachmentAdminService.getSoftDeletedFilesByQaId(qaId);
        return ResponseEntity.ok(fileDetails);
    }

    /**
     *  특정 첨부파일 복구
     */
    @PatchMapping("/{attachmentId}/restore")
    public ResponseEntity<QAFileDetail> restoreAttachment(@PathVariable Long attachmentId) {
        log.info("관리자: 첨부파일 ID {} 복구 요청 수신", attachmentId);

        QAFileDetail restoredFile = qAAttachmentAdminService.restoreAttachment(attachmentId);
        return ResponseEntity.ok(restoredFile);
    }

    /**
     *  특정 첨부파일 영구 삭제
     */
    @DeleteMapping("/{attachmentId}/permanently")
    public ResponseEntity<Void> permanentlyDeleteAttachment(@PathVariable Long attachmentId) {
        log.info("관리자: 첨부파일 ID {} 영구 삭제 요청 수신", attachmentId);

        qAAttachmentAdminService.permanentlyDeleteAttachment(attachmentId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
