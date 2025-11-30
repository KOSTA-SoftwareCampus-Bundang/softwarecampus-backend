package com.softwarecampus.backend.validation;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.dto.user.SignupRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ValidAccountType} 및 {@link AccountTypeValidator} 검증 테스트
 */
@DisplayName("ValidAccountType 검증 테스트")
class ValidAccountTypeTest {

        private static Validator validator;

        @BeforeAll
        static void setUp() {
                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                validator = factory.getValidator();
        }

        @Test
        @DisplayName("ACADEMY 타입 + academyId 있음 → 검증 통과")
        void testAcademyWithAcademyId_shouldPass() {
                // given
                SignupRequest request = new SignupRequest(
                                "academy@test.com",
                                "Password1!",
                                "Test Academy",
                                "010-1234-5678",
                                null,
                                null,
                                null,
                                AccountType.ACADEMY,
                                123L, // academyId 제공
                                true, // termsAgreed
                                true, // privacyAgreed
                                true // marketingAgreed
                );

                // when
                Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

                // then
                assertThat(violations)
                                .filteredOn(v -> v.getMessage().contains("ACADEMY"))
                                .isEmpty();
        }

        @Test
        @DisplayName("ACADEMY 타입 + academyId 없음 → 검증 실패")
        void testAcademyWithoutAcademyId_shouldFail() {
                // given
                SignupRequest request = new SignupRequest(
                                "academy@test.com",
                                "Password1!",
                                "Test Academy",
                                "010-1234-5678",
                                null,
                                null,
                                null,
                                AccountType.ACADEMY,
                                null, // academyId 미제공
                                true, // termsAgreed
                                true, // privacyAgreed
                                true // marketingAgreed
                );

                // when
                Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

                // then
                assertThat(violations)
                                .filteredOn(v -> v.getMessage().contains("ACADEMY 타입일 경우 academyId는 필수입니다"))
                                .hasSize(1);
        }

        @Test
        @DisplayName("USER 타입 + academyId 없음 → 검증 통과")
        void testUserWithoutAcademyId_shouldPass() {
                // given
                SignupRequest request = new SignupRequest(
                                "user@test.com",
                                "Password1!",
                                "Test User",
                                "010-1234-5678",
                                null,
                                null,
                                null,
                                AccountType.USER,
                                null, // USER는 academyId 불필요
                                true, // termsAgreed
                                true, // privacyAgreed
                                true // marketingAgreed
                );

                // when
                Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

                // then
                assertThat(violations)
                                .filteredOn(v -> v.getMessage().contains("ACADEMY"))
                                .isEmpty();
        }

        @Test
        @DisplayName("USER 타입 + academyId 있음 → 검증 통과 (무시)")
        void testUserWithAcademyId_shouldPass() {
                // given
                SignupRequest request = new SignupRequest(
                                "user@test.com",
                                "Password1!",
                                "Test User",
                                "010-1234-5678",
                                null,
                                null,
                                null,
                                AccountType.USER,
                                123L, // USER는 academyId가 있어도 무시
                                true, // termsAgreed
                                true, // privacyAgreed
                                true // marketingAgreed
                );

                // when
                Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

                // then
                assertThat(violations)
                                .filteredOn(v -> v.getMessage().contains("ACADEMY"))
                                .isEmpty();
        }

        @Test
        @DisplayName("ADMIN 타입 + academyId 없음 → 검증 통과")
        void testAdminWithoutAcademyId_shouldPass() {
                // given
                SignupRequest request = new SignupRequest(
                                "admin@test.com",
                                "Password1!",
                                "Test Admin",
                                "010-1234-5678",
                                null,
                                null,
                                null,
                                AccountType.ADMIN,
                                null, // ADMIN은 academyId 불필요
                                true, // termsAgreed
                                true, // privacyAgreed
                                true // marketingAgreed
                );

                // when
                Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

                // then
                assertThat(violations)
                                .filteredOn(v -> v.getMessage().contains("ACADEMY"))
                                .isEmpty();
        }
}
