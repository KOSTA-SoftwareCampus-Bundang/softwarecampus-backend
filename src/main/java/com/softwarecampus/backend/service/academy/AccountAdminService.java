package com.softwarecampus.backend.service.academy;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.AccountUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface AccountAdminService {

    /**
     *  전체 활성 회원 목록 조회
     */
    Page<AccountResponse> getAllActiveAccounts(Pageable pageable);

    /**
     *  회원 목록 검색
     */
    Page<AccountResponse> searchAccounts(String keyword, Pageable pageable);

    /**
     *  특정 회원 상세 정보 조회
     */
    AccountResponse getAccountDetail(Long accountId);

    /**
     *  회원 정보 수정 (관리자용)
     */
    AccountResponse updateAccount(Long accountId, AccountUpdateRequest request);

    /**
     *  회원 삭제
     */
    void deleteAccount(Long accountId);
}
