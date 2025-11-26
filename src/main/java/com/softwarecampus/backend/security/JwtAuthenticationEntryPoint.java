package com.softwarecampus.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 인증 실패 시 401 Unauthorized 응답을 반환하는 EntryPoint
 * 
 * Spring Security는 기본적으로 인증 실패 시 403 Forbidden을 반환하지만,
 * JWT 인증에서는 401 Unauthorized가 더 적합합니다.
 * 
 * @since 2025-11-19
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.debug("인증 실패: {}", authException.getMessage());
        
        // 401 Unauthorized 응답
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
