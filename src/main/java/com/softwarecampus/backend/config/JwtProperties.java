package com.softwarecampus.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정 프로퍼티
 * application.properties의 jwt.* 설정값을 바인딩
 * 
 * @since 2025-11-19
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /**
     * JWT 서명에 사용할 비밀 키
     * 환경변수 JWT_SECRET에서 주입됨
     */
    private String secret;
    
    /**
     * JWT 토큰 만료 시간 (밀리초)
     * 기본값: 1800000ms (30분)
     */
    private long expiration = 1800000L;
}
