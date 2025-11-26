package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;

/**
 * 계정 조회 및 프로필 관리 Service 인터페이스
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
    
    /**
     * 프로필 수정
     * 
     * @param email 이메일
     * @param request 수정할 프로필 정보
     * @return 수정된 계정 정보
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException 계정이 존재하지 않는 경우
     * @throws com.softwarecampus.backend.exception.user.PhoneNumberAlreadyExistsException 전화번호 중복
     */
    AccountResponse updateProfile(String email, UpdateProfileRequest request);
    
    /**
     * 계정 삭제 (소프트 삭제)
     * 
     * @param email 이메일
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException 계정이 존재하지 않는 경우
     */
    void deleteAccount(String email);
}
