package com.softwarecampus.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 요청 유틸리티
 * 
 * 기능:
 * - 클라이언트 실제 IP 추출 (프록시, 로드밸런서 고려)
 * - Rate Limiting 키 생성
 * 
 * @author GitHub Copilot
 */
@Slf4j
@UtilityClass
public class HttpRequestUtils {

    /**
     * 클라이언트 실제 IP 주소 추출
     * 
     * 우선순위:
     * 1. X-Forwarded-For (프록시/로드밸런서)
     * 2. X-Real-IP (Nginx)
     * 3. Proxy-Client-IP
     * 4. WL-Proxy-Client-IP (WebLogic)
     * 5. HTTP_CLIENT_IP
     * 6. HTTP_X_FORWARDED_FOR
     * 7. RemoteAddr (직접 연결)
     * 
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (isInvalidIp(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isInvalidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For는 "client, proxy1, proxy2" 형태일 수 있음
        // 첫 번째 IP만 추출
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        log.debug("Extracted client IP: {}", ip);
        return ip;
    }

    /**
     * Rate Limiting 키 생성 (IP + Username)
     * 
     * @param ip       클라이언트 IP
     * @param username 사용자명 (이메일)
     * @return Rate limiting 키
     */
    public static String createRateLimitKey(String ip, String username) {
        return ip + ":" + username;
    }

    /**
     * Rate Limiting 키 생성 (IP만)
     * 
     * @param ip 클라이언트 IP
     * @return Rate limiting 키
     */
    public static String createRateLimitKey(String ip) {
        return ip;
    }

    /**
     * IP 주소 유효성 검증
     * 
     * @param ip IP 주소
     * @return 유효하지 않으면 true
     */
    private static boolean isInvalidIp(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}
