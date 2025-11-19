package com.softwarecampus.backend.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * JWT Secret 보안 검증기
 * 애플리케이션 시작 시 JWT Secret의 보안 요구사항을 검증합니다.
 * 
 * 검증 항목:
 * - Secret 존재 여부 (JwtProperties의 @NotBlank가 1차 검증)
 * - Secret 최소 길이 (HMAC-SHA256 요구사항: 256bit = 32바이트)
 * - 기본/테스트용 Secret 사용 경고
 * 
 * @since 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtSecretValidator implements InitializingBean {
    
    private final JwtProperties jwtProperties;
    
    /**
     * Bean 초기화 시 JWT Secret 검증 실행
     * 검증 실패 시 IllegalStateException으로 애플리케이션 시작 중단
     */
    @Override
    public void afterPropertiesSet() {
        validateSecretLength();
        warnIfInsecureSecret();
    }
    
    /**
     * JWT Secret 최소 길이 검증
     * HMAC-SHA256 알고리즘은 최소 256bit (32바이트) 이상의 키를 요구합니다
     * 
     * @throws IllegalStateException Secret의 최소 길이 미달 시
     */
    private void validateSecretLength() {
        String secret = jwtProperties.getSecret();
        int secretLength = secret.getBytes(StandardCharsets.UTF_8).length;
        
        // HMAC-SHA256 requires at least 256 bits (32 bytes)
        if (secretLength < 32) {
            throw new IllegalStateException(String.format(
                "JWT_SECRET must be at least 256 bits (32 characters). " +
                "Current length: %d bytes. " +
                "Please set a stronger JWT_SECRET environment variable.",
                secretLength
            ));
        }
        
        log.info("JWT Secret validation passed. Length: {} bytes", secretLength);
    }
    
    /**
     * 불안전한 Secret 사용 경고
     * 프로덕션 환경에서 기본값이나 테스트용 Secret을 사용하면 보안 위협이 있습니다.
     */
    private void warnIfInsecureSecret() {
        String secret = jwtProperties.getSecret();
        
        // 일반적인 불안전한 패턴 검사
        if (secret.contains("YourSecureSecretKey") || 
            secret.contains("ChangeThisInProduction") ||
            secret.equalsIgnoreCase("default") || 
            secret.equalsIgnoreCase("test") ||
            secret.equalsIgnoreCase("secret")) {
            
            log.warn("====================================================");
            log.warn("  WARNING: Using default or test JWT secret!");
            log.warn("This is INSECURE for production use.");
            log.warn("Please set a strong, random JWT_SECRET environment variable.");
            log.warn("====================================================");
        }
    }
}