package com.softwarecampus.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.infrastructure.redis.RedisScripts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limiting 필터
 * 
 * 다층 방어 전략:
 * 1. 전역: 모든 API - 100 req/min per IP (DDoS 방어)
 * 2. 로그인: /api/auth/login - 5 req/min per IP (브루트포스 방어)
 * 3. 비밀번호 검증: /api/auth/verify-password - 10 req/min per (IP + username)
 * 
 * @since 2025-11-19 (Phase 12.5)
 * @updated 2025-12-01 엔드포인트별 세밀한 제한 추가
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = false)
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${rate.limit.requests-per-minute:100}")
    private int requestsPerMinute;
    
    @Value("${rate.limit.login.requests-per-minute:5}")
    private int loginRequestsPerMinute;
    
    @Value("${rate.limit.password-verification.requests-per-minute:10}")
    private int passwordVerificationRequestsPerMinute;
    
    @Value("${rate.limit.enabled:true}")
    private boolean enabled;
    
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final String LOGIN_RATE_LIMIT_PREFIX = "ratelimit:login:";
    private static final String PASSWORD_VERIFY_RATE_LIMIT_PREFIX = "ratelimit:password:";
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Rate Limiting 비활성화 시 (개발 환경)
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String requestUri = request.getRequestURI();
        String clientIp = getClientIp(request);
        
        try {
            // 1. 비밀번호 검증 API - 가장 엄격한 제한 (IP + username)
            if (requestUri.endsWith("/api/auth/verify-password")) {
                if (!checkPasswordVerificationLimit(request, response, clientIp)) {
                    return;
                }
            }
            
            // 2. 로그인 API - 엄격한 제한 (IP)
            if (requestUri.endsWith("/api/auth/login")) {
                if (!checkLoginLimit(response, clientIp)) {
                    return;
                }
            }
            
            // 3. 전역 Rate Limiting (모든 API)
            if (!checkGlobalLimit(response, clientIp)) {
                return;
            }
            
            // 정상 요청 - 다음 필터로
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Rate limit check failed: {}", e.getMessage());
            // Redis 오류 시에도 요청은 허용 (가용성 우선)
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * 비밀번호 검증 Rate Limit 체크
     * 
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @param clientIp 클라이언트 IP
     * @return 요청 허용 여부
     */
    private boolean checkPasswordVerificationLimit(
            HttpServletRequest request,
            HttpServletResponse response,
            String clientIp
    ) throws IOException {
        String username = getAuthenticatedUsername();
        
        if (username == null) {
            // 인증되지 않은 요청은 @PreAuthorize에서 차단됨
            return true;
        }
        
        String key = PASSWORD_VERIFY_RATE_LIMIT_PREFIX + clientIp + ":" + username;
        Long requests = incrementCounter(key, 60); // 1분 TTL
        
        if (requests > passwordVerificationRequestsPerMinute) {
            log.warn("Password verification rate limit exceeded - IP: {}, Username: {}, Count: {}", 
                    clientIp, username, requests);
            sendRateLimitResponse(response, 60, 
                    "비밀번호 검증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            return false;
        }
        
        return true;
    }
    
    /**
     * 로그인 Rate Limit 체크
     * 
     * @param response HTTP 응답
     * @param clientIp 클라이언트 IP
     * @return 요청 허용 여부
     */
    private boolean checkLoginLimit(HttpServletResponse response, String clientIp) 
            throws IOException {
        String key = LOGIN_RATE_LIMIT_PREFIX + clientIp;
        Long requests = incrementCounter(key, 60); // 1분 TTL
        
        if (requests > loginRequestsPerMinute) {
            log.warn("Login rate limit exceeded - IP: {}, Count: {}", clientIp, requests);
            sendRateLimitResponse(response, 60,
                    "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
            return false;
        }
        
        return true;
    }
    
    /**
     * 전역 Rate Limit 체크
     * 
     * @param response HTTP 응답
     * @param clientIp 클라이언트 IP
     * @return 요청 허용 여부
     */
    private boolean checkGlobalLimit(HttpServletResponse response, String clientIp) 
            throws IOException {
        String key = RATE_LIMIT_PREFIX + clientIp;
        Long requests = incrementCounter(key, 60); // 1분 TTL
        
        if (requests > requestsPerMinute) {
            log.warn("Global rate limit exceeded - IP: {}, Count: {}", clientIp, requests);
            sendRateLimitResponse(response, 60,
                    "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Redis 카운터 증가 (Lua Script로 원자적 실행)
     * 
     * @param key        Redis 키
     * @param ttlSeconds TTL (초)
     * @return 현재 카운트
     */
    private Long incrementCounter(String key, int ttlSeconds) {
        return redisTemplate.execute(
                new DefaultRedisScript<>(RedisScripts.INCR_WITH_EXPIRE, Long.class),
                Collections.singletonList(key),
                String.valueOf(ttlSeconds)
        );
    }
    
    /**
     * Rate Limit 초과 응답 전송
     * 
     * @param response       HTTP 응답
     * @param retryAfter     재시도 대기 시간 (초)
     * @param message        에러 메시지
     */
    private void sendRateLimitResponse(
            HttpServletResponse response,
            long retryAfter,
            String message
    ) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("retryAfter", retryAfter);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
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
    
    /**
     * 클라이언트 IP 추출
     * 
     * 프록시/로드밸런서 환경 고려:
     * 1. X-Forwarded-For 헤더 확인
     * 2. X-Real-IP 헤더 확인
     * 3. RemoteAddr 사용
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // X-Forwarded-For: client, proxy1, proxy2
        // 맨 첫번째 IP만 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
