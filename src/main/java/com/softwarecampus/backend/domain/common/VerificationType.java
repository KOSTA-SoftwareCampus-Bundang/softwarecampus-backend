package com.softwarecampus.backend.domain.common;

/**
 * 이메일 인증 타입
 */
public enum VerificationType {
    SIGNUP("회원가입"),
    PASSWORD_RESET("비밀번호 재설정"),
    PASSWORD_CHANGE("비밀번호 변경");
    
    private final String description;
    
    VerificationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
