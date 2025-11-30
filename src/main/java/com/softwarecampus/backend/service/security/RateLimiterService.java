package com.softwarecampus.backend.service.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiter 서비스
 * 
 * Bucket4j를 사용하여 IP 및 계정 기반 요청 제한 구현
 * 
 * 제한 정책:
 * - 비밀번호 검증: 10 req/min per (IP + username)
 * - 로그인: 5 req/min per IP
 * 
 * 특징:
 * - Token Bucket 알고리즘 사용
 * - 메모리 기반 (향후 Redis로 확장 가능)
 * - Thread-safe (ConcurrentHashMap)
 * 
 * @author GitHub Copilot
 */
@Slf4j
@Service
public class RateLimiterService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Rate Limit 타입 정의
     */
    @Getter
    @RequiredArgsConstructor
    public enum RateLimitType {
        PASSWORD_VERIFICATION(10, Duration.ofMinutes(1)),
        LOGIN(5, Duration.ofMinutes(1));

        private final int capacity;
        private final Duration refillDuration;
    }

    /**
     * Rate Limit 체크 및 토큰 소비
     * 
     * @param key  제한 키 (IP, IP+username 등)
     * @param type Rate Limit 타입
     * @return 요청 허용 여부
     */
    public boolean tryConsume(String key, RateLimitType type) {
        Bucket bucket = resolveBucket(key, type);
        boolean consumed = bucket.tryConsume(1);
        
        if (!consumed) {
            log.warn("Rate limit exceeded for key: {}, type: {}", key, type);
        }
        
        return consumed;
    }

    /**
     * 남은 대기 시간 (초) 계산
     * 
     * @param key  제한 키
     * @param type Rate Limit 타입
     * @return 대기 시간 (초)
     */
    public long getSecondsToWaitForRefill(String key, RateLimitType type) {
        Bucket bucket = resolveBucket(key, type);
        var probe = bucket.estimateAbilityToConsume(1);
        
        if (probe.canBeConsumed()) {
            return 0;
        }
        
        return probe.getNanosToWaitForRefill() / 1_000_000_000;
    }

    /**
     * Bucket 조회 또는 생성
     * 
     * @param key  제한 키
     * @param type Rate Limit 타입
     * @return Bucket
     */
    private Bucket resolveBucket(String key, RateLimitType type) {
        String bucketKey = type.name() + ":" + key;
        return buckets.computeIfAbsent(bucketKey, k -> createBucket(type));
    }

    /**
     * 비밀번호 검증용 Rate Limiter
     * 
     * 제한: 10 requests per minute per key
     * 
     * @param key IP + username 조합
     * @return Bucket
     */
    public Bucket resolveBucketForPasswordVerification(String key) {
        return resolveBucket(key, RateLimitType.PASSWORD_VERIFICATION);
    }

    /**
     * 로그인용 Rate Limiter
     * 
     * 제한: 5 requests per minute per IP
     * 
     * @param key IP address
     * @return Bucket
     */
    public Bucket resolveBucketForLogin(String key) {
        return resolveBucket(key, RateLimitType.LOGIN);
    }

    /**
     * Bucket 생성
     * 
     * @param type Rate Limit 타입
     * @return Bucket
     */
    private Bucket createBucket(RateLimitType type) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(type.getCapacity())
                .refillGreedy(type.getCapacity(), type.getRefillDuration())
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Bucket 제거 (메모리 정리용)
     * 
     * @param key 제한 키
     */
    public void removeBucket(String key) {
        buckets.remove(key);
        log.debug("Bucket removed for key: {}", key);
    }

    /**
     * 모든 Bucket 제거 (테스트용)
     */
    public void clearAllBuckets() {
        buckets.clear();
        log.info("All rate limit buckets cleared");
    }
}
