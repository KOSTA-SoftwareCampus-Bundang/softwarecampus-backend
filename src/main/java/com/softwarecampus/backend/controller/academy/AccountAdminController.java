package com.softwarecampus.backend.controller.academy;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.AccountUpdateRequest;
import com.softwarecampus.backend.service.academy.AccountAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/accounts")
@RequiredArgsConstructor
@Validated
public class AccountAdminController {

    private final AccountAdminService accountAdminService;

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
}
