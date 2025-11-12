package com.softwarecampus.backend.validation;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.dto.user.SignupRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link ValidAccountType} 애노테이션의 검증 로직 구현
 * 
 * <p>다음 규칙을 검증합니다:</p>
 * <ul>
 *   <li>accountType이 ACADEMY일 경우, academyId는 반드시 null이 아니어야 함</li>
 *   <li>accountType이 USER 또는 ADMIN일 경우, academyId는 무시됨 (null 허용)</li>
 * </ul>
 * 
 * @see ValidAccountType
 * @see SignupRequest
 */
public class AccountTypeValidator implements ConstraintValidator<ValidAccountType, SignupRequest> {
    
    @Override
    public void initialize(ValidAccountType constraintAnnotation) {
        // 초기화 로직 없음
    }
    
    @Override
    public boolean isValid(SignupRequest request, ConstraintValidatorContext context) {
        // null 요청은 다른 검증(@NotNull 등)에서 처리
        if (request == null) {
            return true;
        }
        
        // ACADEMY 타입일 때만 academyId 필수 검증
        if (request.accountType() == AccountType.ACADEMY) {
            return request.academyId() != null;
        }
        
        // USER, ADMIN 타입은 academyId 검증 안 함
        return true;
    }
}
