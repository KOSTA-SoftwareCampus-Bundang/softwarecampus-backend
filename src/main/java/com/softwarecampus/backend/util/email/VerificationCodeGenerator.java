package com.softwarecampus.backend.util.email;

import com.softwarecampus.backend.common.constants.EmailConstants;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * 인증 코드 생성 유틸리티
 * - SecureRandom을 사용한 암호학적으로 안전한 난수 생성
 * - 6자리 숫자 코드 생성 (000000~999999)
 */
@Component
public class VerificationCodeGenerator {
    
    private final SecureRandom secureRandom;
    
    public VerificationCodeGenerator() {
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * 6자리 인증 코드 생성
     * 
     * @return 6자리 숫자 문자열 (예: "123456", "000042")
     */
    public String generateCode() {
        // SecureRandom으로 4바이트 난수 생성 후 int로 변환
        byte[] randomBytes = new byte[4];
        secureRandom.nextBytes(randomBytes);
        
        // ByteBuffer를 사용해 unsigned int로 변환
        int randomNumber = ByteBuffer.wrap(randomBytes).getInt() & Integer.MAX_VALUE;
        
        // 0~999999 범위로 변환 (1,000,000으로 나눈 나머지)
        int code = randomNumber % (EmailConstants.CODE_MAX + 1);
        
        // 6자리 문자열로 포맷 (앞자리 0 포함)
        return String.format("%0" + EmailConstants.CODE_LENGTH + "d", code);
    }
    
    /**
     * 인증 코드 형식 검증
     * 
     * @param code 검증할 코드
     * @return 6자리 숫자 형식이면 true
     */
    public boolean isValidFormat(String code) {
        if (code == null || code.length() != EmailConstants.CODE_LENGTH) {
            return false;
        }
        
        // 숫자만 포함되어 있는지 확인
        return code.matches("^[0-9]{" + EmailConstants.CODE_LENGTH + "}$");
    }
}
