package com.softwarecampus.backend.service.user.login;

import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.LoginResponse;

/**
 * 로그인 Service 인터페이스
 * 
 * @author 태윤
 */
public interface LoginService {
    
    /**
     * 로그인 처리
     * 
     * @param request 로그인 요청 (email, password)
     * @return 로그인 응답 (accessToken, refreshToken, account)
     * @throws com.softwarecampus.backend.exception.user.InvalidCredentialsException 이메일 없음 또는 비밀번호 불일치
     */
    LoginResponse login(LoginRequest request);
}
