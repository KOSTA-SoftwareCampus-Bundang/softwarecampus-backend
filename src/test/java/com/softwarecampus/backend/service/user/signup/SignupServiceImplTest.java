package com.softwarecampus.backend.service.user.signup;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import com.softwarecampus.backend.service.user.email.EmailVerificationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SignupServiceImpl 단위 테스트
 * 
 * 테스트 대상:
 * - signup(SignupRequest): 회원가입 처리
 * 
 * Mock 대상:
 * - AccountRepository: DB 접근 모킹
 * - PasswordEncoder: 비밀번호 암호화 모킹
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SignupService 단위 테스트")
class SignupServiceImplTest {

        @Mock
        private AccountRepository accountRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private EmailVerificationService emailVerificationService;

        @InjectMocks
        private SignupServiceImpl signupService;

        private SignupRequest userRequest;
        private SignupRequest academyRequest;
        private Account savedUserAccount;
        private Account savedAcademyAccount;

        @BeforeEach
        void setUp() {
                // USER 요청
                userRequest = new SignupRequest(
                                "user@example.com",
                                "password123",
                                "홍길동",
                                "010-1234-5678",
                                "서울시 강남구",
                                null, // affiliation
                                null, // position
                                AccountType.USER,
                                null, // academyId
                                true,
                                true,
                                true);

                // ACADEMY 요청
                academyRequest = new SignupRequest(
                                "academy@example.com",
                                "password123",
                                "김선생",
                                "010-9876-5432",
                                "서울시 서초구",
                                "소프트웨어 캠퍼스",
                                "강사",
                                AccountType.ACADEMY,
                                100L, // academyId
                                true,
                                true,
                                true);

                // USER 저장 결과
                savedUserAccount = Account.builder()
                                .id(1L)
                                .email("user@example.com")
                                .password("encodedPassword")
                                .userName("홍길동")
                                .phoneNumber("010-1234-5678")
                                .accountType(AccountType.USER)
                                .accountApproved(ApprovalStatus.APPROVED)
                                .build();

                // ACADEMY 저장 결과
                savedAcademyAccount = Account.builder()
                                .id(2L)
                                .email("academy@example.com")
                                .password("encodedPassword")
                                .userName("김선생")
                                .phoneNumber("010-9876-5432")
                                .accountType(AccountType.ACADEMY)
                                .academyId(100L)
                                .accountApproved(ApprovalStatus.PENDING)
                                .build();

                // 모든 테스트에 대해 이메일 인증 통과 처리 (lenient로 불필요한 스텁 에러 방지)
                lenient().when(emailVerificationService.isEmailVerified(anyString(), any())).thenReturn(true);
        }

        @Test
        @DisplayName("정상 회원가입 - USER (즉시 승인)")
        void signup_성공_USER() {
                // Given
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(emailVerificationService.isEmailVerified(anyString(), any())).thenReturn(true);
                when(accountRepository.save(any(Account.class))).thenReturn(savedUserAccount);

                // When
                AccountResponse response = signupService.signup(userRequest);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(1L);
                assertThat(response.email()).isEqualTo("user@example.com");
                assertThat(response.userName()).isEqualTo("홍길동");
                assertThat(response.accountType()).isEqualTo(AccountType.USER);
                assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);

                // 메서드 호출 검증
                verify(passwordEncoder).encode("password123");
                verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("정상 회원가입 - ACADEMY (승인 대기)")
        void signup_성공_ACADEMY() {
                // Given
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(emailVerificationService.isEmailVerified(anyString(), any())).thenReturn(true);
                when(accountRepository.save(any(Account.class))).thenReturn(savedAcademyAccount);

                // When
                AccountResponse response = signupService.signup(academyRequest);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.accountType()).isEqualTo(AccountType.ACADEMY);
                assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.PENDING);

                // academyId 검증
                verify(accountRepository)
                                .save(argThat(account -> account.getAcademyId() != null
                                                && account.getAcademyId().equals(100L)));
        }

        @Test
        @DisplayName("이메일 형식 오류 - @ 없음 (RFC 5322 위반)")
        void signup_이메일형식오류_골뱅이없음() {
                // Given
                SignupRequest invalidRequest = new SignupRequest(
                                "invalid-email", // @ 없음
                                "password123",
                                "홍길동",
                                "010-1234-5678",
                                null, null, null,
                                AccountType.USER,
                                null,
                                true,
                                true,
                                true);

                // When & Then
                assertThatThrownBy(() -> signupService.signup(invalidRequest))
                                .isInstanceOf(InvalidInputException.class)
                                .hasMessage("올바른 이메일 형식이 아닙니다.");

                // Repository 호출되지 않아야 함
                verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("이메일 형식 오류 - 하이픈 시작 (RFC 1035 위반)")
        void signup_이메일형식오류_하이픈시작() {
                // Given
                SignupRequest invalidRequest = new SignupRequest(
                                "user@-invalid.com", // 도메인 레이블 하이픈 시작
                                "password123",
                                "홍길동",
                                "010-1234-5678",
                                null, null, null,
                                AccountType.USER,
                                null,
                                true,
                                true,
                                true);

                // When & Then
                assertThatThrownBy(() -> signupService.signup(invalidRequest))
                                .isInstanceOf(InvalidInputException.class)
                                .hasMessage("올바른 이메일 형식이 아닙니다.");

                // Repository 호출되지 않아야 함
                verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("이메일 중복 - DataIntegrityViolationException (Race Condition)")
        void signup_이메일중복_RaceCondition() {
                // Given
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(emailVerificationService.isEmailVerified(anyString(), any())).thenReturn(true);
                when(accountRepository.save(any(Account.class)))
                                .thenThrow(new DataIntegrityViolationException(
                                                "Duplicate entry 'user@example.com' for key 'UK_ACCOUNT_EMAIL'"));

                // When & Then
                assertThatThrownBy(() -> signupService.signup(userRequest))
                                .isInstanceOf(DuplicateEmailException.class)
                                .hasMessage("이미 사용 중인 이메일입니다.");

                // save() 호출은 되어야 함
                verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("전화번호 중복 - DataIntegrityViolationException")
        void signup_전화번호중복() {
                // Given
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(emailVerificationService.isEmailVerified(anyString(), any())).thenReturn(true);
                when(accountRepository.save(any(Account.class)))
                                .thenThrow(new DataIntegrityViolationException(
                                                "Duplicate entry '010-1234-5678' for key 'UK_ACCOUNT_PHONE'"));

                // When & Then
                assertThatThrownBy(() -> signupService.signup(userRequest))
                                .isInstanceOf(InvalidInputException.class)
                                .hasMessage("이미 사용 중인 전화번호입니다.");
        }

        @Test
        @DisplayName("ACADEMY 타입 - academyId 필수 검증")
        void signup_ACADEMY_academyId없음() {
                // Given
                SignupRequest invalidRequest = new SignupRequest(
                                "academy@example.com",
                                "password123",
                                "김선생",
                                "010-9876-5432",
                                null,
                                "소프트웨어 캠퍼스",
                                "강사",
                                AccountType.ACADEMY,
                                null, // academyId 없음!
                                true,
                                true,
                                true);

                // When & Then
                assertThatThrownBy(() -> signupService.signup(invalidRequest))
                                .isInstanceOf(InvalidInputException.class)
                                .hasMessage("기관 회원은 기관 ID가 필수입니다.");
        }

        @Test
        @DisplayName("ADMIN 타입 - 회원가입 차단")
        void signup_ADMIN_차단() {
                // Given
                SignupRequest adminRequest = new SignupRequest(
                                "admin@example.com",
                                "password123",
                                "관리자",
                                "010-0000-0000",
                                null, null, null,
                                AccountType.ADMIN, // ADMIN 타입!
                                null,
                                true,
                                true,
                                true);

                // When & Then
                assertThatThrownBy(() -> signupService.signup(adminRequest))
                                .isInstanceOf(InvalidInputException.class)
                                .hasMessage("관리자 계정은 회원가입으로 생성할 수 없습니다.");
        }

        @Test
        @DisplayName("null 요청 - NullPointerException")
        void signup_null요청() {
                // When & Then
                assertThatThrownBy(() -> signupService.signup(null))
                                .isInstanceOf(NullPointerException.class)
                                .hasMessage("SignupRequest must not be null");
        }

        @Test
        @DisplayName("비밀번호 암호화 확인")
        void signup_비밀번호암호화() {
                // Given
                when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
                when(emailVerificationService.isEmailVerified(anyString(), any())).thenReturn(true);
                when(accountRepository.save(any(Account.class))).thenReturn(savedUserAccount);

                // When
                signupService.signup(userRequest);

                // Then
                verify(passwordEncoder).encode("password123");

                // 저장되는 Account의 password가 암호화되었는지 확인
                verify(accountRepository).save(argThat(account -> account.getPassword().equals("encodedPassword")));
        }
}
