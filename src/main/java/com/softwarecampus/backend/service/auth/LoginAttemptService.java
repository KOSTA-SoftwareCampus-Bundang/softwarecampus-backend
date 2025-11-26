package com.softwarecampus.backend.service.auth;

import com.softwarecampus.backend.infrastructure.redis.RedisScripts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 로그인 시도 추적 서비스
 * 브루트포스 공격 방어
 * 
 * @since 2025-11-19 (Phase 12.5)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = false)
public class LoginAttemptService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${rate.limit.login.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${rate.limit.login.block-duration:300}")
    private int blockDuration; // 초
    
    private static final String LOGIN_ATTEMPT_PREFIX = "loginattempt:";
    
    /**
     * Lua Script: INCR + EXPIRE 원자적 처리
     * 
     * @see RedisScripts#INCR_WITH_EXPIRE 상세 설명 참조
     */
    private static final String LUA_SCRIPT = RedisScripts.INCR_WITH_EXPIRE;
    
    /**
     * 로그인 실패 기록
     * 
     * @param ip 클라이언트 IP
     */
    public void loginFailed(String ip) {
        String key = LOGIN_ATTEMPT_PREFIX + ip;
        
        // Lua Script로 INCR + EXPIRE 원자적 실행
        Long attempts = redisTemplate.execute(
            new DefaultRedisScript<>(LUA_SCRIPT, Long.class),
            Collections.singletonList(key),
            String.valueOf(blockDuration) // TTL (초)
        );
        
        if (attempts != null) {
            log.warn("Login failed for IP: {} (attempt {}/{})", 
                ip, attempts, maxAttempts);
        }
    }
    
    /**
     * 로그인 성공 - 카운터 리셋
     * 
     * @param ip 클라이언트 IP
     */
    public void loginSucceeded(String ip) {
        String key = LOGIN_ATTEMPT_PREFIX + ip;
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("Login attempt counter reset for IP: {}", ip);
        }
    }
    
    /**
     * 로그인 차단 여부 확인
     * 
     * @param ip 클라이언트 IP
     * @return true: 차단됨, false: 허용
     */
    public boolean isBlocked(String ip) {
        String key = LOGIN_ATTEMPT_PREFIX + ip;
        String attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts != null) {
            int attemptCount = Integer.parseInt(attempts);
            return attemptCount >= maxAttempts;
        }
        
        return false;
    }
    
    /**
     * 남은 차단 시간 조회 (초)
     * 
     * @param ip 클라이언트 IP
     * @return 남은 시간 (초), 차단되지 않으면 0
     */
    public long getRemainingBlockTime(String ip) {
        if (!isBlocked(ip)) {
            return 0;
        }
        
        String key = LOGIN_ATTEMPT_PREFIX + ip;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        
        return ttl != null && ttl > 0 ? ttl : 0;
    }
}
