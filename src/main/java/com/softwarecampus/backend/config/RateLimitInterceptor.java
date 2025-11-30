package com.softwarecampus.backend.config;

import com.softwarecampus.backend.service.RateLimiterService;
import com.softwarecampus.backend.util.HttpRequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limiting Interceptor
 * 
 * 기능:
 * - 비밀번호 검증 API에 대한 Rate Limiting 적용
 * - IP + 사용자명 기반 제한 (10 req/min)
 * - HTTP 429 (Too Many Requests) 응답
 * - Retry-After 헤더 추가
 * 
 * @author GitHub Copilot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        
        String requestUri = request.getRequestURI();
        
        // 비밀번호 검증 API에만 Rate Limiting 적용
        if (requestUri.endsWith("/api/auth/verify-password")) {
            return handlePasswordVerificationRateLimit(request, response);
        }
        
        // 로그인 API에 Rate Limiting 적용
        if (requestUri.endsWith("/api/auth/login")) {
            return handleLoginRateLimit(request, response);
        }
        
        return true;
    }

    /**
     * 비밀번호 검증 API Rate Limiting 처리
     * 
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @return true: 요청 허용, false: 요청 차단
     */
    private boolean handlePasswordVerificationRateLimit(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        
        String clientIp = HttpRequestUtils.getClientIp(request);
        String username = getAuthenticatedUsername();
        
        if (username == null) {
            log.warn("인증되지 않은 사용자의 비밀번호 검증 시도 - IP: {}", clientIp);
            return true; // @PreAuthorize에서 차단됨
        }
        
        String rateLimitKey = HttpRequestUtils.createRateLimitKey(clientIp, username);
        
        if (rateLimiterService.tryConsume(rateLimitKey, 
                RateLimiterService.RateLimitType.PASSWORD_VERIFICATION)) {
            log.debug("비밀번호 검증 요청 허용 - Key: {}", rateLimitKey);
            return true;
        } else {
            long waitSeconds = rateLimiterService.getSecondsToWaitForRefill(rateLimitKey,
                    RateLimiterService.RateLimitType.PASSWORD_VERIFICATION);
            
            log.warn("비밀번호 검증 Rate Limit 초과 - Key: {}, Wait: {}초", rateLimitKey, waitSeconds);
            
            sendRateLimitExceededResponse(response, waitSeconds, 
                    "비밀번호 검증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            return false;
        }
    }

    /**
     * 로그인 API Rate Limiting 처리
     * 
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @return true: 요청 허용, false: 요청 차단
     */
    private boolean handleLoginRateLimit(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        
        String clientIp = HttpRequestUtils.getClientIp(request);
        
        if (rateLimiterService.tryConsume(clientIp, RateLimiterService.RateLimitType.LOGIN)) {
            log.debug("로그인 요청 허용 - IP: {}", clientIp);
            return true;
        } else {
            long waitSeconds = rateLimiterService.getSecondsToWaitForRefill(clientIp,
                    RateLimiterService.RateLimitType.LOGIN);
            
            log.warn("로그인 Rate Limit 초과 - IP: {}, Wait: {}초", clientIp, waitSeconds);
            
            sendRateLimitExceededResponse(response, waitSeconds,
                    "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
            return false;
        }
    }

    /**
     * Rate Limit 초과 응답 전송
     * 
     * @param response    HTTP 응답
     * @param waitSeconds 대기 시간(초)
     * @param message     에러 메시지
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response, long waitSeconds, String message)
            throws Exception {
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("retryAfter", waitSeconds);
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * 인증된 사용자명 조회
     * 
     * @return 사용자명 (이메일) 또는 null
     */
    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }
}
