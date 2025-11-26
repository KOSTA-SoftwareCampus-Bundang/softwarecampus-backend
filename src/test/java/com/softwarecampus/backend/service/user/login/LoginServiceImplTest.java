package com.softwarecampus.backend.service.user.login;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.LoginResponse;
import com.softwarecampus.backend.exception.user.InvalidCredentialsException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LoginServiceImpl 단위 테스트
 * 
 * 테스트 대상:
 * - login(LoginRequest): 로그인 처리
 * 
 * Mock 대상:
 * - AccountRepository: DB 접근 모킹
 * - PasswordEncoder: 비밀번호 검증 모킹
 * - JwtTokenProvider: JWT 토큰 생성 모킹
 * - RedisTemplate: Refresh Token 저장 모킹
 * 
 * @author 태윤
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoginService 단위 테스트")
class LoginServiceImplTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @InjectMocks
    private LoginServiceImpl loginService;
    
    private LoginRequest validRequest;
    private Account activeUserAccount;
    private Account activeAcademyAccount;
    
    @BeforeEach
    void setUp() {
        validRequest = new LoginRequest(
            "user@example.com",
            "Password123!"
        );
        
        // 활성화된 USER 계정
        activeUserAccount = Account.builder()
            .id(1L)
            .email("user@example.com")
            .password("$2a$10$encodedPassword")  // BCrypt 해시
            .userName("홍길동")
            .phoneNumber("010-1234-5678")
            .address("서울시 강남구")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
        
        // 승인된 ACADEMY 계정
        activeAcademyAccount = Account.builder()
            .id(2L)
            .email("academy@example.com")
            .password("$2a$10$encodedPassword")
            .userName("김선생")
            .phoneNumber("010-9876-5432")
            .address("서울시 서초구")
            .affiliation("소프트웨어 캠퍼스")
            .position("강사")
            .accountType(AccountType.ACADEMY)
            .accountApproved(ApprovalStatus.APPROVED)
            .academyId(100L)
            .build();
        
        // RedisTemplate Mock 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    @DisplayName("로그인 성공 - USER 계정")
    void login_Success_User() {
        // given
        when(accountRepository.findByEmail(validRequest.email()))
            .thenReturn(Optional.of(activeUserAccount));
        when(passwordEncoder.matches(validRequest.password(), activeUserAccount.getPassword()))
            .thenReturn(true);
        when(jwtTokenProvider.generateToken(activeUserAccount.getEmail(), "USER"))
            .thenReturn("access-token-123");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(180000L);  // 3분 = 180,000 밀리초
        
        // when
        LoginResponse response = loginService.login(validRequest);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token-123");
        assertThat(response.refreshToken()).isNotNull();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(180L);  // 초 단위
        assertThat(response.account().email()).isEqualTo("user@example.com");
        assertThat(response.account().accountType()).isEqualTo(AccountType.USER);
        
        // verify
        verify(accountRepository).findByEmail(validRequest.email());
        verify(passwordEncoder).matches(validRequest.password(), activeUserAccount.getPassword());
        verify(jwtTokenProvider).generateToken(activeUserAccount.getEmail(), "USER");
        verify(valueOperations).set(
            eq("refresh:user@example.com"),
            anyString(),
            eq(7 * 24 * 60 * 60 * 1000L),
            eq(TimeUnit.MILLISECONDS)
        );
    }
    
    @Test
    @DisplayName("로그인 성공 - ACADEMY 계정 (승인됨)")
    void login_Success_Academy() {
        // given
        LoginRequest academyRequest = new LoginRequest(
            "academy@example.com",
            "Password123!"
        );
        
        when(accountRepository.findByEmail(academyRequest.email()))
            .thenReturn(Optional.of(activeAcademyAccount));
        when(passwordEncoder.matches(academyRequest.password(), activeAcademyAccount.getPassword()))
            .thenReturn(true);
        when(jwtTokenProvider.generateToken(activeAcademyAccount.getEmail(), "ACADEMY"))
            .thenReturn("access-token-academy");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(180000L);
        
        // when
        LoginResponse response = loginService.login(academyRequest);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.account().accountType()).isEqualTo(AccountType.ACADEMY);
        assertThat(response.account().approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        
        verify(valueOperations).set(
            eq("refresh:academy@example.com"),
            anyString(),
            anyLong(),
            eq(TimeUnit.MILLISECONDS)
        );
    }
    
    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_EmailNotFound() {
        // given
        when(accountRepository.findByEmail(validRequest.email()))
            .thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> loginService.login(validRequest))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
        
        verify(accountRepository).findByEmail(validRequest.email());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(anyString(), anyString());
    }
    
    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_WrongPassword() {
        // given
        when(accountRepository.findByEmail(validRequest.email()))
            .thenReturn(Optional.of(activeUserAccount));
        when(passwordEncoder.matches(validRequest.password(), activeUserAccount.getPassword()))
            .thenReturn(false);  // 비밀번호 불일치
        
        // when & then
        assertThatThrownBy(() -> loginService.login(validRequest))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
        
        verify(passwordEncoder).matches(validRequest.password(), activeUserAccount.getPassword());
        verify(jwtTokenProvider, never()).generateToken(anyString(), anyString());
    }
    
    @Test
    @DisplayName("로그인 실패 - 비활성화된 계정")
    void login_Fail_InactiveAccount() {
        // given
        Account inactiveAccount = Account.builder()
            .id(3L)
            .email("inactive@example.com")
            .password("$2a$10$encodedPassword")
            .userName("비활성 사용자")
            .phoneNumber("010-1111-2222")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
        inactiveAccount.markDeleted();  // 소프트 삭제
        
        LoginRequest inactiveRequest = new LoginRequest(
            "inactive@example.com",
            "Password123!"
        );
        
        when(accountRepository.findByEmail(inactiveRequest.email()))
            .thenReturn(Optional.of(inactiveAccount));
        when(passwordEncoder.matches(inactiveRequest.password(), inactiveAccount.getPassword()))
            .thenReturn(true);
        
        // when & then
        assertThatThrownBy(() -> loginService.login(inactiveRequest))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("비활성화된 계정입니다");
        
        verify(jwtTokenProvider, never()).generateToken(anyString(), anyString());
    }
    
    @Test
    @DisplayName("로그인 실패 - 미승인 ACADEMY 계정 (PENDING)")
    void login_Fail_PendingAcademy() {
        // given
        Account pendingAcademy = Account.builder()
            .id(4L)
            .email("pending@example.com")
            .password("$2a$10$encodedPassword")
            .userName("승인대기")
            .phoneNumber("010-3333-4444")
            .accountType(AccountType.ACADEMY)
            .accountApproved(ApprovalStatus.PENDING)  // 승인 대기
            .academyId(200L)
            .build();
        
        LoginRequest pendingRequest = new LoginRequest(
            "pending@example.com",
            "Password123!"
        );
        
        when(accountRepository.findByEmail(pendingRequest.email()))
            .thenReturn(Optional.of(pendingAcademy));
        when(passwordEncoder.matches(pendingRequest.password(), pendingAcademy.getPassword()))
            .thenReturn(true);
        
        // when & then
        assertThatThrownBy(() -> loginService.login(pendingRequest))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("승인 대기 중인 계정입니다");
        
        verify(jwtTokenProvider, never()).generateToken(anyString(), anyString());
    }
    
    @Test
    @DisplayName("로그인 실패 - 거부된 ACADEMY 계정 (REJECTED)")
    void login_Fail_RejectedAcademy() {
        // given
        Account rejectedAcademy = Account.builder()
            .id(5L)
            .email("rejected@example.com")
            .password("$2a$10$encodedPassword")
            .userName("승인거부")
            .phoneNumber("010-5555-6666")
            .accountType(AccountType.ACADEMY)
            .accountApproved(ApprovalStatus.REJECTED)  // 승인 거부
            .academyId(300L)
            .build();
        
        LoginRequest rejectedRequest = new LoginRequest(
            "rejected@example.com",
            "Password123!"
        );
        
        when(accountRepository.findByEmail(rejectedRequest.email()))
            .thenReturn(Optional.of(rejectedAcademy));
        when(passwordEncoder.matches(rejectedRequest.password(), rejectedAcademy.getPassword()))
            .thenReturn(true);
        
        // when & then
        assertThatThrownBy(() -> loginService.login(rejectedRequest))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("승인이 거부된 계정입니다");
        
        verify(jwtTokenProvider, never()).generateToken(anyString(), anyString());
    }
    
    @Test
    @DisplayName("JWT 토큰 발급 검증")
    void login_VerifyJwtTokenGeneration() {
        // given
        when(accountRepository.findByEmail(validRequest.email()))
            .thenReturn(Optional.of(activeUserAccount));
        when(passwordEncoder.matches(validRequest.password(), activeUserAccount.getPassword()))
            .thenReturn(true);
        when(jwtTokenProvider.generateToken(activeUserAccount.getEmail(), "USER"))
            .thenReturn("access-token");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(180000L);
        
        // when
        loginService.login(validRequest);
        
        // then
        verify(jwtTokenProvider).generateToken(
            eq(activeUserAccount.getEmail()), 
            eq("USER")
        );
    }
    
    @Test
    @DisplayName("Refresh Token 저장 검증")
    void login_VerifyRefreshTokenSave() {
        // given
        when(accountRepository.findByEmail(validRequest.email()))
            .thenReturn(Optional.of(activeUserAccount));
        when(passwordEncoder.matches(validRequest.password(), activeUserAccount.getPassword()))
            .thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString(), anyString()))
            .thenReturn("access-token");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(180000L);
        
        // when
        loginService.login(validRequest);
        
        // then
        verify(valueOperations).set(
            eq("refresh:user@example.com"),
            anyString(),
            eq(7 * 24 * 60 * 60 * 1000L),
            eq(TimeUnit.MILLISECONDS)
        );
    }
    
    @Test
    @DisplayName("expiresIn 변환 검증 (밀리초 → 초)")
    void login_VerifyExpiresInConversion() {
        // given
        when(accountRepository.findByEmail(validRequest.email()))
            .thenReturn(Optional.of(activeUserAccount));
        when(passwordEncoder.matches(validRequest.password(), activeUserAccount.getPassword()))
            .thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString(), anyString()))
            .thenReturn("access-token");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(180000L);  // 밀리초
        
        // when
        LoginResponse response = loginService.login(validRequest);
        
        // then
        assertThat(response.expiresIn()).isEqualTo(180L);  // 초 단위로 변환 (3분)
    }
}
