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

import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
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
    
    /**
     * 신뢰할 수 있는 프록시 IP 목록
     * 
     * 운영 환경: 실제 프록시/로드밸런서 IP로 설정
     * 개발 환경: localhost, 127.0.0.1 등
     * 
     * application.yml 예시:
     * rate:
     *   limit:
     *     trusted-proxies: 10.0.0.1,10.0.0.2,127.0.0.1
     */
    @Value("${rate.limit.trusted-proxies:}")
    private String trustedProxies;
    
    /**
     * X-Forwarded-For 헤더 신뢰 여부
     * 
     * true: 프록시 뒤에 있음, X-Forwarded-For 사용
     * false: 직접 연결, RemoteAddr만 사용 (기본값)
     * 
     * Spring의 ForwardedHeaderFilter 또는
     * server.forward-headers-strategy=NATIVE 사용 권장
     */
    @Value("${rate.limit.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;
    
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final String LOGIN_RATE_LIMIT_PREFIX = "ratelimit:login:";
    private static final String PASSWORD_VERIFY_RATE_LIMIT_PREFIX = "ratelimit:password:";
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
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
        
        String requestUri = normalizeRequestPath(request.getRequestURI());
        String clientIp = getClientIp(request);
        
        try {
            // 1. 비밀번호 검증 API - 가장 엄격한 제한 (IP + username)
            if (pathMatcher.match("/api/auth/verify-password", requestUri)) {
                if (!checkPasswordVerificationLimit(request, response, clientIp)) {
                    return;
                }
            }
            
            // 2. 로그인 API - 엄격한 제한 (IP)
            if (pathMatcher.match("/api/auth/login", requestUri)) {
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
     * 요청 경로 정규화
     * - 쿼리 파라미터 제거
     * - 후행 슬래시 제거
     * 
     * @param uri 원본 URI
     * @return 정규화된 경로
     */
    private String normalizeRequestPath(String uri) {
        if (uri == null) {
            return "";
        }
        
        // 쿼리 파라미터 제거
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }
        
        // 후행 슬래시 제거 (루트 경로 "/" 제외)
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        
        return uri;
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
        long requests = incrementCounter(key, 60); // 1분 TTL
        
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
        long requests = incrementCounter(key, 60); // 1분 TTL
        
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
        long requests = incrementCounter(key, 60); // 1분 TTL
        
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
     * Redis 실행 실패 시 null을 반환할 수 있으므로,
     * null-safe하게 0L로 변환하여 반환합니다.
     * 
     * @param key        Redis 키
     * @param ttlSeconds TTL (초)
     * @return 현재 카운트 (실패 시 0L)
     */
    private long incrementCounter(String key, int ttlSeconds) {
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(RedisScripts.INCR_WITH_EXPIRE, Long.class),
                Collections.singletonList(key),
                String.valueOf(ttlSeconds)
        );
        return Objects.requireNonNullElse(result, 0L);
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
     * 클라이언트의 실제 IP 주소를 추출
     * 
     * IP 스푸핑 방지 전략:
     * 1. trust-forwarded-headers=false (기본값)
     *    - X-Forwarded-For/X-Real-IP 헤더 무시
     *    - request.getRemoteAddr() 사용 (직접 연결 IP)
     *    - 프록시 없는 환경, 로컬 개발, 테스트 환경 권장
     * 
     * 2. trust-forwarded-headers=true + trusted-proxies 설정
     *    - request.getRemoteAddr()가 신뢰할 수 있는 프록시인지 검증
     *    - 검증 성공 시 X-Forwarded-For/X-Real-IP에서 클라이언트 IP 추출
     *    - 검증 실패 시 request.getRemoteAddr() 사용 (스푸핑 차단)
     *    - 운영 환경 권장 (Nginx, AWS ALB/ELB 등)
     * 
     * 권장 설정:
     * - Spring의 ForwardedHeaderFilter 또는
     * - server.forward-headers-strategy=NATIVE 사용
     * 
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        
        // 1. 프록시 헤더 신뢰하지 않는 경우 (기본값)
        if (!trustForwardedHeaders) {
            return remoteAddr;
        }
        
        // 2. 프록시 헤더를 신뢰하는 경우
        //    - 신뢰할 수 있는 프록시 IP 목록 검증 필요
        if (trustedProxies != null && !trustedProxies.isEmpty()) {
            String[] trustedProxyList = trustedProxies.split(",");
            boolean isTrustedProxy = false;
            
            for (String trustedProxy : trustedProxyList) {
                if (remoteAddr.equals(trustedProxy.trim())) {
                    isTrustedProxy = true;
                    break;
                }
            }
            
            // 신뢰할 수 없는 프록시에서 온 요청 → RemoteAddr 사용 (스푸핑 차단)
            if (!isTrustedProxy) {
                return remoteAddr;
            }
        }
        
        // 3. 신뢰할 수 있는 프록시로 검증됨 → 프록시 헤더에서 클라이언트 IP 추출
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = remoteAddr;
        }
        
        // X-Forwarded-For: client, proxy1, proxy2 형식에서 첫 번째 IP 추출
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
