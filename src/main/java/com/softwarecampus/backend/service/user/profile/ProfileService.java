package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.dto.user.AccountResponse;

/**
 * 계정 조회 Service 인터페이스
 */
public interface ProfileService {
    
    /**
     * ID로 계정 조회
     * 
     * @param accountId 계정 ID
     * @return 계정 정보
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException 계정이 존재하지 않는 경우
     */
    AccountResponse getAccountById(Long accountId);
    
    /**
     * 이메일로 계정 조회
     * 
     * @param email 이메일
     * @return 계정 정보
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException 계정이 존재하지 않는 경우
     */
    AccountResponse getAccountByEmail(String email);
}
