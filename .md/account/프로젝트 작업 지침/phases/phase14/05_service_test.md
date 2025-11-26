# 5. Service 단위 테스트 (로그인)

**목표:** LoginServiceImpl 비즈니스 로직 검증

---

## 📂 생성 파일

```
src/test/java/com/softwarecampus/backend/
└─ service/user/login/
   └─ LoginServiceImplTest.java
```

---

## 5.1 LoginServiceImplTest.java

**경로:** `test/java/com/softwarecampus/backend/service/user/login/LoginServiceImplTest.java`

**설명:** LoginServiceImpl 단위 테스트 (8-10개 테스트)

```java
package com.softwarecampus.backend.service.user.login;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.LoginResponse;
import com.softwarecampus.backend.exception.user.InvalidCredentialsException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import com.softwarecampus.backend.service.token.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

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
 * - TokenService: Refresh Token 저장 모킹
 * 
 * @author 태윤
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginService 단위 테스트")
class LoginServiceImplTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private TokenService tokenService;
    
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
        when(jwtTokenProvider.generateRefreshToken(activeUserAccount.getEmail()))
            .thenReturn("refresh-token-456");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(900000L);  // 15분 = 900,000 밀리초
        
        // when
        LoginResponse response = loginService.login(validRequest);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token-123");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-456");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);  // 초 단위
        assertThat(response.account().email()).isEqualTo("user@example.com");
        assertThat(response.account().accountType()).isEqualTo("USER");
        
        // verify
        verify(accountRepository).findByEmail(validRequest.email());
        verify(passwordEncoder).matches(validRequest.password(), activeUserAccount.getPassword());
        verify(jwtTokenProvider).generateToken(activeUserAccount.getEmail(), "USER");
        verify(jwtTokenProvider).generateRefreshToken(activeUserAccount.getEmail());
        verify(tokenService).saveRefreshToken(activeUserAccount.getEmail(), "refresh-token-456");
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
        when(jwtTokenProvider.generateRefreshToken(activeAcademyAccount.getEmail()))
            .thenReturn("refresh-token-academy");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(900000L);
        
        // when
        LoginResponse response = loginService.login(academyRequest);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.account().accountType()).isEqualTo("ACADEMY");
        assertThat(response.account().accountApproved()).isEqualTo("APPROVED");
        
        verify(tokenService).saveRefreshToken(activeAcademyAccount.getEmail(), "refresh-token-academy");
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
        inactiveAccount.delete();  // 소프트 삭제
        
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
            .hasMessage("승인 대기 중인 계정입니다");
        
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
        when(jwtTokenProvider.generateRefreshToken(activeUserAccount.getEmail()))
            .thenReturn("refresh-token");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(900000L);
        
        // when
        loginService.login(validRequest);
        
        // then
        verify(jwtTokenProvider).generateToken(
            eq(activeUserAccount.getEmail()), 
            eq("USER")
        );
        verify(jwtTokenProvider).generateRefreshToken(eq(activeUserAccount.getEmail()));
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
        when(jwtTokenProvider.generateRefreshToken(anyString()))
            .thenReturn("refresh-token-saved");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(900000L);
        
        // when
        loginService.login(validRequest);
        
        // then
        verify(tokenService).saveRefreshToken(
            eq(activeUserAccount.getEmail()), 
            eq("refresh-token-saved")
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
        when(jwtTokenProvider.generateRefreshToken(anyString()))
            .thenReturn("refresh-token");
        when(jwtTokenProvider.getExpiration())
            .thenReturn(900000L);  // 밀리초
        
        // when
        LoginResponse response = loginService.login(validRequest);
        
        // then
        assertThat(response.expiresIn()).isEqualTo(900L);  // 초 단위로 변환
    }
}
```

---

## 📊 테스트 커버리지

| 테스트 케이스 | 검증 내용 |
|------------|---------|
| login_Success_User | USER 계정 로그인 성공 |
| login_Success_Academy | ACADEMY 계정 로그인 성공 (승인됨) |
| login_Fail_EmailNotFound | 존재하지 않는 이메일 → 401 |
| login_Fail_WrongPassword | 비밀번호 불일치 → 401 |
| login_Fail_InactiveAccount | 비활성화된 계정 → 401 |
| login_Fail_PendingAcademy | PENDING 상태 ACADEMY → 401 |
| login_Fail_RejectedAcademy | REJECTED 상태 ACADEMY → 401 |
| login_VerifyJwtTokenGeneration | JWT 토큰 생성 검증 |
| login_VerifyRefreshTokenSave | Refresh Token 저장 검증 |
| login_VerifyExpiresInConversion | 밀리초→초 변환 검증 |

**총 10개 테스트**

---

## 🔗 다음 단계

Service 단위 테스트 완료 후:
1. **AuthControllerTest** 로그인 테스트 추가 ([06_controller_test.md](06_controller_test.md))
2. **LoginIntegrationTest** 통합 테스트 작성 ([07_integration_test.md](07_integration_test.md))
