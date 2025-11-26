package com.softwarecampus.backend.infrastructure.redis;

/**
 * Redis Lua Script 공유 유틸리티 클래스
 * 
 * Redis Lua Script를 한 곳에서 관리하여 코드 중복 방지 및 유지보수성 향상
 * 
 * @since 2025-11-26
 */
public final class RedisScripts {
    
    private RedisScripts() {
        // 유틸리티 클래스: 인스턴스 생성 방지
    }
    
    /**
     * INCR + EXPIRE 원자적 처리 Lua 스크립트
     * 
     * <p><b>문제점:</b> increment()와 expire()를 따로 호출하면 원자성 보장 안 됨
     * <ul>
     *   <li>increment() 성공 후 서버 다운 시 expire() 실행 안 됨</li>
     *   <li>TTL 없는 키가 Redis에 영구 저장 (메모리 누수)</li>
     *   <li>로그인 실패 카운터가 무한정 유지 (사용자 영구 차단 위험)</li>
     * </ul>
     * 
     * <p><b>해결책:</b> Lua Script로 INCR + EXPIRE를 한 번에 실행
     * <ul>
     *   <li>Redis 서버에서 원자적 실행 (All or Nothing)</li>
     *   <li>네트워크 왕복 50% 감소 (2회 → 1회)</li>
     *   <li>count가 1일 때만 EXPIRE 설정하여 첫 생성 시에만 TTL 적용</li>
     *   <li>이미 존재하는 키의 TTL은 유지 (중요: TTL 리셋 방지)</li>
     * </ul>
     * 
     * <p><b>사용처:</b>
     * <ul>
     *   <li>{@link com.softwarecampus.backend.security.RateLimitFilter} - IP 기반 Rate Limiting</li>
     *   <li>{@link com.softwarecampus.backend.service.auth.LoginAttemptService} - 로그인 실패 추적</li>
     * </ul>
     * 
     * <p><b>스크립트 동작:</b>
     * <pre>
     * local count = redis.call('INCR', KEYS[1])  -- 카운터 증가
     * if count == 1 then                         -- 첫 생성 시에만
     *   redis.call('EXPIRE', KEYS[1], ARGV[1])   -- TTL 설정
     * end
     * return count                               -- 현재 카운트 반환
     * </pre>
     * 
     * <p><b>사용 예시:</b>
     * <pre>
     * Long count = redisTemplate.execute(
     *     new DefaultRedisScript&lt;&gt;(RedisScripts.INCR_WITH_EXPIRE, Long.class),
     *     Collections.singletonList(key),
     *     String.valueOf(ttlSeconds)
     * );
     * </pre>
     */
    public static final String INCR_WITH_EXPIRE = 
        "local count = redis.call('INCR', KEYS[1]) " +
        "if count == 1 then " +
        "  redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
        "end " +
        "return count";
}
