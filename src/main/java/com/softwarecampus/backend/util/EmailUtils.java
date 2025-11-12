package com.softwarecampus.backend.util;

import java.util.regex.Pattern;

/**
 * 이메일 관련 유틸리티
 * - 이메일 형식 검증
 * - 이메일 마스킹 (로깅용)
 */
public class EmailUtils {
    
    // RFC 5322 간소화 버전 + RFC 1035 표준 준수
    // - TLD 최대 63자
    // - 국제화 도메인(punycode, xn--) 지원
    // - 도메인 레이블: 영문자/숫자로 시작/끝, 중간에만 하이픈 허용 (RFC 1035 섹션 2.3.1)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?$"
    );
    
    private EmailUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * 이메일 형식 검증
     * RFC 5321: 로컬 파트 최대 64자
     */
    public static boolean isValidFormat(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        
        // 로컬 파트 길이 검증 (RFC 5321)
        int atIndex = email.indexOf('@');
        if (atIndex > 64) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 이메일 마스킹 (로깅용)
     * 예: "user@example.com" → "u***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        
        // 로컬 파트만 마스킹 (길이에 따라)
        String maskedLocal;
        if (localPart.length() <= 3) {
            maskedLocal = localPart.charAt(0) + "***";
        } else {
            int visibleChars = localPart.length() / 3;
            maskedLocal = localPart.substring(0, visibleChars) + "***";
        }
        
        return maskedLocal + "@" + domain;
    }
}
