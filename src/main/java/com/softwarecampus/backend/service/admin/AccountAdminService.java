package com.softwarecampus.backend.service.admin;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.AccountUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface AccountAdminService {

    /**
     * 회원 목록 조회 (검색 키워드가 있으면 검색, 없으면 전체 조회)
     * 작성일: 2025-12-02 - 컨트롤러 분기 로직 서비스 계층으로 이동
     */
    Page<AccountResponse> getAccounts(String keyword, Pageable pageable);

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
    
    /**
     * 회원 승인
     * - 승인 상태 변경 및 승인 이메일 발송
     * 
     * @param accountId 승인할 회원 ID
     * @return 승인된 회원 정보
     */
    AccountResponse approveAccount(Long accountId);
}
