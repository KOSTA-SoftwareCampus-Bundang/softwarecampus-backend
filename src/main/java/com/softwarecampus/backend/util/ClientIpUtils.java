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
        //    - trustedProxies가 null이거나 비어있으면 신뢰하지 않음 (IP 스푸핑 방지)
        if (trustedProxies == null || trustedProxies.trim().isEmpty()) {
            return remoteAddr;
        }
        
        String[] trustedProxyList = trustedProxies.split(",");
        boolean isTrustedProxy = false;
        boolean hasValidTrustedProxy = false;
        
        for (String trustedProxy : trustedProxyList) {
            String trimmedProxy = trustedProxy.trim();
            
            // 빈 항목은 무시
            if (trimmedProxy.isEmpty()) {
                continue;
            }
            
            // 기본 IP/CIDR 형식 검증 (잘못된 형식은 무시하여 실수로 신뢰하는 것을 방지)
            if (!isValidIpOrCidr(trimmedProxy)) {
                continue;
            }
            
            hasValidTrustedProxy = true;
            
            if (remoteAddr.equals(trimmedProxy)) {
                isTrustedProxy = true;
                break;
            }
        }
        
        // 유효한 신뢰 프록시가 하나도 없으면 신뢰하지 않음
        if (!hasValidTrustedProxy) {
            return remoteAddr;
        }
        
        // 신뢰할 수 없는 프록시에서 온 요청 → RemoteAddr 사용 (스푸핑 차단)
        if (!isTrustedProxy) {
            return remoteAddr;
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
    
    /**
     * IP 주소 또는 CIDR 표기법의 기본 형식을 검증
     * 
     * 잘못된 형식의 항목이 신뢰 프록시 목록에 포함되어 
     * 의도치 않게 신뢰되는 것을 방지
     * 
     * @param value 검증할 IP 또는 CIDR 문자열
     * @return 유효한 형식이면 true, 아니면 false
     */
    private boolean isValidIpOrCidr(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        // CIDR 표기법인 경우 (예: 10.0.0.0/8)
        String ipPart = value;
        if (value.contains("/")) {
            String[] parts = value.split("/", 2);
            ipPart = parts[0];
            String cidrPart = parts[1];
            
            // CIDR prefix 검증 (0-32 범위의 숫자여야 함)
            if (!cidrPart.matches("\\d{1,2}")) {
                return false;
            }
            try {
                int prefix = Integer.parseInt(cidrPart);
                if (prefix < 0 || prefix > 32) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        // IPv4 형식 검증 (예: 192.168.1.1)
        if (ipPart.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            String[] octets = ipPart.split("\\.");
            for (String octet : octets) {
                try {
                    int num = Integer.parseInt(octet);
                    if (num < 0 || num > 255) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        
        // IPv6 형식 기본 검증 (콜론을 포함하고 허용된 문자만 포함)
        if (ipPart.contains(":")) {
            return ipPart.matches("[0-9a-fA-F:]+");
        }
        
        // localhost 허용
        if ("localhost".equalsIgnoreCase(ipPart)) {
            return true;
        }
        
        return false;
    }
}
