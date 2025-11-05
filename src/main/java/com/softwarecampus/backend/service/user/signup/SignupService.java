package com.softwarecampus.backend.service.user.signup;

import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.dto.user.AccountResponse;

/**
 * 회원가입 Service 인터페이스
 */
public interface SignupService {
    
    /**
     * 회원가입
     * 
     * @param request 회원가입 요청 DTO
     * @return 생성된 계정 정보
     * @throws com.softwarecampus.backend.exception.user.DuplicateEmailException 이메일이 이미 존재하는 경우
     */
    AccountResponse signup(SignupRequest request);
}
