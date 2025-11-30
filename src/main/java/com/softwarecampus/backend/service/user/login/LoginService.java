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
    
    /**
     * 현재 비밀번호 검증 (비밀번호 변경 전 본인 확인용)
     * 
     * @param email 로그인한 사용자 이메일
     * @param currentPassword 입력한 현재 비밀번호
     * @return 검증 성공 여부
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException 계정이 존재하지 않는 경우
     * @throws com.softwarecampus.backend.exception.user.InvalidPasswordException 비밀번호가 일치하지 않는 경우
     */
    boolean verifyPassword(String email, String currentPassword);
}
