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
     * @throws com.softwarecampus.backend.exception.user.InvalidInputException 이메일 형식 오류, ADMIN 차단, ACADEMY academyId 누락
     */
    AccountResponse signup(SignupRequest request);
    
    /**
     * 이메일 중복 확인
     * 
     * @param email 확인할 이메일
     * @return true: 사용 가능, false: 사용 불가
     * @throws com.softwarecampus.backend.exception.user.InvalidInputException 이메일 형식 오류
     */
    boolean isEmailAvailable(String email);
}
