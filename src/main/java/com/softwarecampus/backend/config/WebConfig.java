package com.softwarecampus.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 * 
 * 주요 설정:
 * - CORS (Cross-Origin Resource Sharing)
 * - Rate Limiting Interceptor
 * 
 * @author 태윤
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Value("${FRONTEND_PORT:3000}")
    private String frontendPort;

    /**
     * CORS 설정
     * 
     * 허용 정책:
     * - Origin: http://localhost:{FRONTEND_PORT}
     * - Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
     * - Headers: Authorization, Content-Type, X-Requested-With
     * - Credentials: true (쿠키/인증 허용)
     * - Max Age: 3600초 (1시간)
     * 
     * ⚠️ 프로덕션 환경:
     * - allowedOrigins에 실제 도메인 추가 필요
     * - 환경별 분리 또는 프로파일 사용 권장
     */
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 허용
                .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://localhost:8080",
                        "http://localhost:8081", "http://localhost:5174") // 명시적 허용
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Interceptor 등록
     * 
     * 설정:
     * - RateLimitInterceptor: 비밀번호 검증, 로그인 API Rate Limiting
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/auth/verify-password", "/api/auth/login");
    }

}
