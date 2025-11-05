package com.softwarecampus.backend.util;

import java.util.regex.Pattern;

/**
 * 이메일 관련 유틸리티
 * - 이메일 형식 검증
 * - 이메일 마스킹 (로깅용)
 */
public class EmailUtils {
    
    // RFC 5322 간소화 버전 + RFC 1035 TLD 표준 (최대 63자)
    // 국제화 도메인(punycode, xn--) 지원을 위해 TLD에 숫자와 하이픈 허용
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,63}$"
    );
    
    private EmailUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * 이메일 형식 검증
     */
    public static boolean isValidFormat(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 이메일 마스킹 (로깅용)
     * 예: "user@example.com" → "u***@e***.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "[empty]";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "[invalid]";
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        
        String maskedLocal = localPart.length() > 1 
            ? localPart.charAt(0) + "***" 
            : localPart;
        
        int dotIndex = domain.indexOf('.');
        String maskedDomain = dotIndex > 0
            ? domain.charAt(0) + "***" + domain.substring(dotIndex)
            : domain.charAt(0) + "***";
        
        return maskedLocal + "@" + maskedDomain;
    }
}
