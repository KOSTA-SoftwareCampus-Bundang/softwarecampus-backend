package com.softwarecampus.backend.common.constants;

/**
 * 이메일 관련 상수
 */
public final class EmailConstants {
    
    private EmailConstants() {
        throw new AssertionError("상수 클래스는 인스턴스화할 수 없습니다");
    }
    
    // 이메일 발신자 정보
    public static final String SENDER_EMAIL = "noreply@softwarecampus.com";
    public static final String SENDER_NAME = "소프트웨어캠퍼스";
    
    // 인증 코드 설정
    public static final int CODE_LENGTH = 6;
    public static final int CODE_MIN = 0;
    public static final int CODE_MAX = 999999;
    
    // 만료 시간
    public static final int EXPIRY_MINUTES = 3;
    public static final int EXPIRY_SECONDS = EXPIRY_MINUTES * 60; // 180초
    
    // 보안 설정
    public static final int MAX_ATTEMPTS = 5;
    public static final int BLOCK_DURATION_MINUTES = 30;
    public static final int RESEND_COOLDOWN_SECONDS = 60;
    
    // 이메일 제목
    public static final String SUBJECT_SIGNUP = "[소프트웨어캠퍼스] 회원가입 인증 코드";
    public static final String SUBJECT_PASSWORD_RESET = "[소프트웨어캠퍼스] 비밀번호 재설정 인증 코드";
}
