package com.softwarecampus.backend.security;

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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Rate Limiting 필터
 * IP 기반 요청 속도 제한 (DDoS 방어)
 * 
 * @since 2025-11-19 (Phase 12.5)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = false)
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${rate.limit.requests-per-minute:100}")
    private int requestsPerMinute;
    
    @Value("${rate.limit.enabled:true}")
    private boolean enabled;
    
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    
    /**
     * Lua Script: INCR + EXPIRE 원자적 처리
     * 
     * @see RedisScripts#INCR_WITH_EXPIRE 상세 설명 참조
     */
    private static final String LUA_SCRIPT = RedisScripts.INCR_WITH_EXPIRE;
    
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
        
        // 1. 클라이언트 IP 추출
        String clientIp = getClientIp(request);
        String key = RATE_LIMIT_PREFIX + clientIp;
        
        try {
            // 2. Lua Script로 INCR + EXPIRE 원자적 실행
            Long requests = redisTemplate.execute(
                new DefaultRedisScript<>(LUA_SCRIPT, Long.class),
                Collections.singletonList(key),
                String.valueOf(60) // 60초 TTL
            );
            
            // 3. 제한 초과 확인
            if (requests != null && requests > requestsPerMinute) {
                log.warn("Rate limit exceeded for IP: {} ({})", clientIp, requests);
                
                // 429 Too Many Requests
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
                );
                return;
            }
            
            // 5. 정상 요청 - 다음 필터로
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Rate limit check failed: {}", e.getMessage());
            // Redis 오류 시에도 요청은 허용 (가용성 우선)
            filterChain.doFilter(request, response);
        }
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
