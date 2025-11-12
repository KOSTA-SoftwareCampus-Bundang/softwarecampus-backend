package com.softwarecampus.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ACADEMY 계정 타입에 대한 academyId 필수 검증 애노테이션
 * 
 * <p>계정 타입이 ACADEMY일 경우 academyId가 반드시 제공되어야 함을 검증합니다.</p>
 * 
 * <h3>사용 예시:</h3>
 * <pre>
 * {@code
 * @ValidAccountType
 * public record SignupRequest(
 *     AccountType accountType,
 *     Long academyId,
 *     ...
 * ) {}
 * }
 * </pre>
 * 
 * @see AccountTypeValidator
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AccountTypeValidator.class)
@Documented
public @interface ValidAccountType {
    
    /**
     * 검증 실패 시 반환할 메시지
     */
    String message() default "ACADEMY 타입일 경우 academyId는 필수입니다";
    
    /**
     * 검증 그룹
     */
    Class<?>[] groups() default {};
    
    /**
     * 추가 페이로드
     */
    Class<? extends Payload>[] payload() default {};
}
