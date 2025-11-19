# Phase 12.5-1: Redis 기본 설정

> **소요 시간:** 30분  
> **목표:** Docker로 로컬 Redis 구성 및 Spring Boot 연동

---

## 1. Docker Compose 구성

### docker-compose.yml 생성 (프로젝트 루트)
```yaml
version: '3.8'

services:
  redis:
    image: redis:7.2-alpine
    container_name: softwarecampus-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  # Optional: Redis GUI (개발 편의성)
  redis-commander:
    image: rediscommander/redis-commander:latest
    container_name: softwarecampus-redis-gui
    ports:
      - "8081:8081"
    environment:
      - REDIS_HOSTS=local:redis:6379
    depends_on:
      - redis
    restart: unless-stopped

volumes:
  redis-data:
    driver: local
```

### Redis 시작
```powershell
# Docker Compose 실행
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs redis

# Redis CLI 접속 테스트
docker exec -it softwarecampus-redis redis-cli
> PING
PONG
> SET test "Hello Redis"
OK
> GET test
"Hello Redis"
> exit
```

### Redis Commander 접속 (선택)
- URL: http://localhost:8081
- Redis 데이터를 브라우저에서 확인 가능

---

## 2. pom.xml 의존성 추가

```xml
<!-- Phase 12.5: Redis + Caching -->
<dependencies>
    <!-- 기존 의존성들... -->
    
    <!-- Redis (Lettuce 클라이언트 포함) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Spring Cache 추상화 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    
    <!-- Connection Pool (성능 향상) -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>
</dependencies>
```

---

## 3. application.properties 설정

```properties
# ===================================
# Phase 12.5: Redis Configuration
# ===================================

# Redis 서버 설정
spring.redis.host=${REDIS_HOST:localhost}
spring.redis.port=${REDIS_PORT:6379}
spring.redis.password=${REDIS_PASSWORD:}
spring.redis.database=0

# Lettuce Connection Pool
spring.redis.lettuce.pool.max-active=10
spring.redis.lettuce.pool.max-idle=10
spring.redis.lettuce.pool.min-idle=2
spring.redis.lettuce.pool.max-wait=-1ms

# Cache 설정
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
spring.cache.redis.cache-null-values=false

# Redis 연결 타임아웃
spring.redis.timeout=2000ms
```

---

## 4. RedisConfig.java 생성

**패키지:** `com.softwarecampus.backend.config`

```java
package com.softwarecampus.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정
 * - RedisTemplate 빈 구성
 * - JSON 직렬화 설정
 * 
 * @since 2025-11-19 (Phase 12.5)
 */
@Configuration
public class RedisConfig {
    
    /**
     * RedisTemplate 설정
     * - Key: String 직렬화
     * - Value: JSON 직렬화 (Java 객체 저장 가능)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // ObjectMapper 설정 (날짜/시간 타입 지원)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // JSON 직렬화
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // Key는 String으로
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value는 JSON으로
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
```

---

## 5. Redis 연결 테스트

### 테스트 코드 (임시)

**파일:** `src/test/java/com/softwarecampus/backend/config/RedisConfigTest.java`

```java
package com.softwarecampus.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisConfigTest {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Test
    void redisConnectionTest() {
        // Given
        String key = "test:connection";
        String value = "Redis is working!";
        
        // When
        redisTemplate.opsForValue().set(key, value);
        Object result = redisTemplate.opsForValue().get(key);
        
        // Then
        assertThat(result).isEqualTo(value);
        
        // Cleanup
        redisTemplate.delete(key);
    }
}
```

### 실행
```powershell
# 컴파일
mvn clean compile

# 테스트
mvn test -Dtest=RedisConfigTest

# 예상 결과
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

---

## 6. Redis CLI로 데이터 확인

```bash
# Redis 컨테이너 접속
docker exec -it softwarecampus-redis redis-cli

# 저장된 키 확인
127.0.0.1:6379> KEYS *
(empty array)  # 아직 아무것도 없음

# 테스트 데이터 저장
127.0.0.1:6379> SET mykey "Hello from Redis"
OK

# 조회
127.0.0.1:6379> GET mykey
"Hello from Redis"

# TTL 설정 (10초 후 삭제)
127.0.0.1:6379> SETEX tempkey 10 "This will expire"
OK

127.0.0.1:6379> TTL tempkey
(integer) 8

# 10초 후
127.0.0.1:6379> GET tempkey
(nil)  # 자동 삭제됨
```

---

## 7. 문제 해결

### 문제 1: Redis 연결 실패
```
Caused by: io.lettuce.core.RedisConnectionException: 
Unable to connect to localhost:6379
```

**해결:**
```powershell
# Redis 실행 중인지 확인
docker-compose ps

# Redis 로그 확인
docker-compose logs redis

# 재시작
docker-compose restart redis
```

### 문제 2: Connection Pool 에러
```
Caused by: java.lang.ClassNotFoundException: 
org.apache.commons.pool2.impl.GenericObjectPool
```

**해결:**
```xml
<!-- pom.xml에 추가 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### 문제 3: 직렬화 에러
```
SerializationException: Could not read JSON
```

**해결:**
- RedisConfig의 ObjectMapper 설정 확인
- JavaTimeModule 등록 확인

---

## ✅ 완료 체크리스트

- [ ] docker-compose.yml 작성
- [ ] `docker-compose up -d` 성공
- [ ] Redis PING 응답 확인
- [ ] pom.xml 의존성 추가
- [ ] application.properties Redis 설정
- [ ] RedisConfig.java 생성
- [ ] RedisTemplate 빈 주입 확인
- [ ] RedisConfigTest 통과
- [ ] Redis CLI로 데이터 확인

---

## 📝 다음 단계

✅ Redis 연결 완료!

다음: **Phase 12.5-2 - UserDetails 캐싱**
- CustomUserDetailsService에 @Cacheable 추가
- CacheConfig 설정
- 캐시 성능 측정
