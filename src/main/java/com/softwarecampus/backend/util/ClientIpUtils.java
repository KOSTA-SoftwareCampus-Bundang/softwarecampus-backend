package com.softwarecampus.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 IP 추출 유틸리티
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
 * @since 2025-12-03
 */
@Component
public class ClientIpUtils {
    
    /**
     * 신뢰할 수 있는 프록시 IP 목록
     * 
     * 운영 환경: 실제 프록시/로드밸런서 IP로 설정
     * 개발 환경: localhost, 127.0.0.1 등
     * 
     * application.properties 예시:
     * rate.limit.trusted-proxies=10.0.0.1,10.0.0.2,127.0.0.1
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
    
    /**
     * 클라이언트의 실제 IP 주소를 추출
     * 
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    public String getClientIp(HttpServletRequest request) {
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
