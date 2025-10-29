# Account 도메인 구현 계획 (최종 설계안)

**담당자:** 태윤  
**목표:** Spring Boot + Spring Security를 사용한 인증/인가 시스템 구축  
**작업 기간:** 2025-10-29 ~ (예상)

---

## 📋 전체 작업 Phase 개요

```
Phase 1: Domain & Repository ✅ (완료)
Phase 2: GlobalExceptionHandler 기본 틀
Phase 3: 기본 보안 설정 (PasswordEncoder)
Phase 4: DTO Layer (Request/Response)
Phase 5: Service Layer + 도메인 예외 (동시 작성)
Phase 6: Service 단위 테스트 (Mockito)
Phase 7: Controller Layer (회원가입 API)
Phase 8: Controller 슬라이스 테스트 (@WebMvcTest)
Phase 9: Repository 테스트 (@DataJpaTest)
Phase 10: 통합 테스트 (회원가입 E2E)
Phase 11: JWT 구현 (JwtTokenProvider)
Phase 12: JWT 단위 테스트
Phase 13: UserDetailsService 구현
Phase 14: UserDetailsService 테스트
Phase 15: Security 고도화 (필터, 권한)
Phase 16: 로그인 API
Phase 17: 로그인 테스트 (Service + Controller)
Phase 18: 마이페이지 API
Phase 19: 마이페이지 테스트 (인증 포함)
Phase 20: 통합 테스트 (전체 플로우)
```

---

## Phase 1: Domain & Repository ✅ (완료)

### 완료된 작업
- ✅ `domain/common/AccountType.java` - Enum
- ✅ `domain/common/ApprovalStatus.java` - Enum
- ✅ `domain/user/Account.java` - 엔티티
- ✅ `repository/user/AccountRepository.java` - 7개 쿼리 메서드

### 검증 방법
- JPA DDL 자동 생성으로 테이블 생성 확인
- Repository 쿼리 메서드 동작 확인

---

## Phase 2: GlobalExceptionHandler 기본 틀

### 목표
- RFC 9457 Problem Details 구조 확립
- Spring 기본 예외 처리 (Validation)
- 도메인 예외는 나중에 추가 (주석으로 표시)

### 생성 파일
```
exception/
  ├─ GlobalExceptionHandler.java    # @RestControllerAdvice
  └─ (도메인 예외는 Phase 5에서 추가)
```

### 구현 내용

#### `GlobalExceptionHandler.java`
```java
package com.softwarecampus.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리기
 * RFC 9457 Problem Details 형식으로 응답
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 처리 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "요청 본문에 유효하지 않은 필드가 있습니다."
        );
        problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/validation-error"));
        problemDetail.setTitle("Validation Failed");
        
        // 필드별 오류 수집
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        problemDetail.setProperty("errors", errors);
        
        return problemDetail;
    }

    /**
     * 일반 예외 처리 (fallback)
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다."
        );
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle("Internal Server Error");
        
        return problemDetail;
    }

    // Phase 5에서 추가할 도메인 예외들 (주석으로 표시)
    
    // /**
    //  * 이메일 중복 예외 처리
    //  */
    // @ExceptionHandler(DuplicateEmailException.class)
    // public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
    //     ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
    //         HttpStatus.CONFLICT,
    //         ex.getMessage()
    //     );
    //     problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/duplicate-email"));
    //     problemDetail.setTitle("Duplicate Email");
    //     return problemDetail;
    // }

    // /**
    //  * 계정 없음 예외 처리
    //  */
    // @ExceptionHandler(AccountNotFoundException.class)
    // public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
    //     ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
    //         HttpStatus.NOT_FOUND,
    //         ex.getMessage()
    //     );
    //     problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/account-not-found"));
    //     problemDetail.setTitle("Account Not Found");
    //     return problemDetail;
    // }
}
```

### 검증 방법
- 임시 Controller 생성하여 Validation 오류 발생 테스트
- RFC 9457 형식으로 응답 확인

---

## Phase 3: 기본 보안 설정 (PasswordEncoder)

### 목표
- PasswordEncoder Bean만 먼저 등록
- JWT, 필터, 권한 설정은 나중에 (Phase 15)

### 생성 파일
```
security/
  └─ SecurityConfig.java    # 최소 구성
```

### 구현 내용

#### `SecurityConfig.java`
```java
package com.softwarecampus.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Security 기본 설정
 * Phase 3: PasswordEncoder만 먼저 구성
 * Phase 15: JWT, 필터, 권한 설정 추가 예정
 */
@Configuration
public class SecurityConfig {

    /**
     * 비밀번호 암호화를 위한 Encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Phase 15에서 추가할 설정들 (주석으로 표시)
    
    // @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //     http
    //         .csrf(csrf -> csrf.disable())
    //         .authorizeHttpRequests(auth -> auth
    //             .requestMatchers("/api/auth/**").permitAll()
    //             .anyRequest().authenticated()
    //         );
    //     return http.build();
    // }
}
```

### 검증 방법
```java
@Autowired
private PasswordEncoder passwordEncoder;

@Test
void 패스워드_인코더_동작_확인() {
    String raw = "password123";
    String encoded = passwordEncoder.encode(raw);
    
    assertNotEquals(raw, encoded);
    assertTrue(passwordEncoder.matches(raw, encoded));
}
```

---

## Phase 4: DTO Layer (Request/Response)

### 목표
- 회원가입에 필요한 DTO 작성
- Bean Validation 적용

### 생성 파일
```
dto/user/
  ├─ request/
  │   └─ SignupRequest.java
  └─ response/
      ├─ AccountResponse.java
      └─ MessageResponse.java
```

### 구현 내용

#### `SignupRequest.java`
```java
package com.softwarecampus.backend.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO
 */
public record SignupRequest(
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    String password,
    
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다")
    String nickname,
    
    String address,
    String affiliation,
    String position
) {
}
```

#### `AccountResponse.java`
```java
package com.softwarecampus.backend.dto.user.response;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;

/**
 * 계정 정보 응답 DTO
 */
public record AccountResponse(
    Long id,
    String email,
    String nickname,
    AccountType accountType,
    ApprovalStatus approvalStatus,
    String address,
    String affiliation,
    String position
) {
}
```

#### `MessageResponse.java`
```java
package com.softwarecampus.backend.dto.user.response;

/**
 * 간단한 메시지 응답 DTO
 */
public record MessageResponse(
    String message
) {
}
```

### 검증 방법
- Validation 테스트 작성 (Phase 8에서 Controller 테스트 시 검증)

---

## Phase 5: Service Layer + 도메인 예외 (동시 작성)

### 목표
- AccountService 인터페이스 정의
- AccountServiceImpl 구현
- 필요한 도메인 예외 즉시 생성
- GlobalExceptionHandler에 핸들러 추가

### 생성 파일
```
service/user/
  ├─ AccountService.java
  └─ impl/
      └─ AccountServiceImpl.java

exception/
  ├─ DuplicateEmailException.java       # Service 작성 중 생성
  ├─ AccountNotFoundException.java      # Service 작성 중 생성
  └─ GlobalExceptionHandler.java        # 핸들러 추가
```

### 구현 내용

#### `AccountService.java`
```java
package com.softwarecampus.backend.service.user;

import com.softwarecampus.backend.dto.user.request.SignupRequest;
import com.softwarecampus.backend.dto.user.response.AccountResponse;

/**
 * Account 서비스 인터페이스
 */
public interface AccountService {
    
    /**
     * 회원가입
     */
    AccountResponse signup(SignupRequest request);
    
    /**
     * 이메일로 계정 조회
     */
    AccountResponse findByEmail(String email);
}
```

#### `AccountServiceImpl.java`
```java
package com.softwarecampus.backend.service.user.impl;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.request.SignupRequest;
import com.softwarecampus.backend.dto.user.response.AccountResponse;
import com.softwarecampus.backend.exception.AccountNotFoundException;
import com.softwarecampus.backend.exception.DuplicateEmailException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.service.user.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {
    
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public AccountResponse signup(SignupRequest request) {
        // 1. 이메일 중복 체크 → 예외 필요! (즉시 생성)
        if (accountRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        
        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        
        // 3. Account 엔티티 생성 (빌더 패턴 사용)
        Account account = Account.builder()
            .email(request.email())
            .password(encodedPassword)
            .nickname(request.nickname())
            .accountType(AccountType.USER) // 기본값
            .approvalStatus(ApprovalStatus.PENDING) // 기본값
            .address(request.address())
            .affiliation(request.affiliation())
            .position(request.position())
            .build();
        
        // 4. 저장
        Account savedAccount = accountRepository.save(account);
        
        // 5. DTO 변환
        return toAccountResponse(savedAccount);
    }
    
    @Override
    public AccountResponse findByEmail(String email) {
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException(email)); // 예외 즉시 생성
        
        return toAccountResponse(account);
    }
    
    // 엔티티 → DTO 변환
    private AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getEmail(),
            account.getNickname(),
            account.getAccountType(),
            account.getApprovalStatus(),
            account.getAddress(),
            account.getAffiliation(),
            account.getPosition()
        );
    }
}
```

#### `DuplicateEmailException.java` (Service 작성 중 생성)
```java
package com.softwarecampus.backend.exception;

/**
 * 이메일 중복 예외
 */
public class DuplicateEmailException extends RuntimeException {
    
    public DuplicateEmailException(String email) {
        super("이미 사용 중인 이메일입니다: " + email);
    }
}
```

#### `AccountNotFoundException.java` (Service 작성 중 생성)
```java
package com.softwarecampus.backend.exception;

/**
 * 계정 없음 예외
 */
public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException(String email) {
        super("계정을 찾을 수 없습니다: " + email);
    }
}
```

#### `GlobalExceptionHandler.java` (핸들러 추가)
```java
// Phase 2에서 주석 처리했던 부분 활성화

/**
 * 이메일 중복 예외 처리
 */
@ExceptionHandler(DuplicateEmailException.class)
public ProblemDetail handleDuplicateEmail(DuplicateEmailException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/duplicate-email"));
    problemDetail.setTitle("Duplicate Email");
    return problemDetail;
}

/**
 * 계정 없음 예외 처리
 */
@ExceptionHandler(AccountNotFoundException.class)
public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problemDetail.setType(URI.create("https://api.softwarecampus.com/problems/account-not-found"));
    problemDetail.setTitle("Account Not Found");
    return problemDetail;
}
```

### 검증 방법
- Phase 6 단위 테스트에서 검증

---

## Phase 6: Service 단위 테스트 (Mockito)

### 목표
- Mockito로 의존성 모킹
- 정상 케이스 + 예외 케이스 검증

### 생성 파일
```
test/.../service/user/
  └─ AccountServiceImplTest.java
```

### 구현 내용

#### `AccountServiceImplTest.java`
```java
package com.softwarecampus.backend.service.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.request.SignupRequest;
import com.softwarecampus.backend.dto.user.response.AccountResponse;
import com.softwarecampus.backend.exception.AccountNotFoundException;
import com.softwarecampus.backend.exception.DuplicateEmailException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.service.user.impl.AccountServiceImpl;
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
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 단위 테스트")
class AccountServiceImplTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private AccountServiceImpl accountService;
    
    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        SignupRequest request = new SignupRequest(
            "test@example.com",
            "password123!",
            "테스트유저",
            null, null, null
        );
        
        given(accountRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded_password");
        
        Account savedAccount = Account.builder()
            .id(1L)
            .email(request.email())
            .password("encoded_password")
            .nickname(request.nickname())
            .accountType(AccountType.USER)
            .approvalStatus(ApprovalStatus.PENDING)
            .build();
        
        given(accountRepository.save(any(Account.class))).willReturn(savedAccount);
        
        // when
        AccountResponse response = accountService.signup(request);
        
        // then
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("테스트유저");
        
        // verify
        then(accountRepository).should(times(1)).existsByEmail(request.email());
        then(passwordEncoder).should(times(1)).encode(request.password());
        then(accountRepository).should(times(1)).save(any(Account.class));
    }
    
    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_fail_duplicateEmail() {
        // given
        SignupRequest request = new SignupRequest(
            "duplicate@example.com",
            "password123!",
            "테스트유저",
            null, null, null
        );
        
        given(accountRepository.existsByEmail(request.email())).willReturn(true);
        
        // when & then
        assertThatThrownBy(() -> accountService.signup(request))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("duplicate@example.com");
        
        // verify - save는 호출되지 않아야 함
        then(accountRepository).should(never()).save(any(Account.class));
    }
    
    @Test
    @DisplayName("이메일로 계정 조회 성공")
    void findByEmail_success() {
        // given
        String email = "test@example.com";
        Account account = Account.builder()
            .id(1L)
            .email(email)
            .nickname("테스트유저")
            .accountType(AccountType.USER)
            .approvalStatus(ApprovalStatus.APPROVED)
            .build();
        
        given(accountRepository.findByEmail(email)).willReturn(Optional.of(account));
        
        // when
        AccountResponse response = accountService.findByEmail(email);
        
        // then
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.nickname()).isEqualTo("테스트유저");
    }
    
    @Test
    @DisplayName("이메일로 계정 조회 실패 - 계정 없음")
    void findByEmail_fail_notFound() {
        // given
        String email = "notfound@example.com";
        given(accountRepository.findByEmail(email)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> accountService.findByEmail(email))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining(email);
    }
}
```

### 검증 방법
```bash
./mvnw test -Dtest=AccountServiceImplTest
```

---

## Phase 7: Controller Layer (회원가입 API)

### 목표
- 회원가입 REST API 구현
- API 명세 준수 (POST /api/auth/signup)

### 생성 파일
```
controller/user/
  └─ AuthController.java
```

### 구현 내용

#### `AuthController.java`
```java
package com.softwarecampus.backend.controller.user;

import com.softwarecampus.backend.dto.user.request.SignupRequest;
import com.softwarecampus.backend.dto.user.response.AccountResponse;
import com.softwarecampus.backend.service.user.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러
 * Phase 7: 회원가입만 구현
 * Phase 16: 로그인 추가 예정
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AccountService accountService;
    
    /**
     * 회원가입
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request) {
        AccountResponse response = accountService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // Phase 16에서 추가할 로그인 API (주석으로 표시)
    
    // /**
    //  * 로그인
    //  * POST /api/auth/login
    //  */
    // @PostMapping("/login")
    // public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    //     LoginResponse response = authService.login(request);
    //     return ResponseEntity.ok(response);
    // }
}
```

### API 명세

#### POST /api/auth/signup
**Request:**
```json
{
  "email": "test@example.com",
  "password": "password123!",
  "nickname": "테스트유저",
  "address": "서울시 강남구",
  "affiliation": "소프트웨어캠퍼스",
  "position": "수강생"
}
```

**Success Response (201 Created):**
```json
{
  "id": 1,
  "email": "test@example.com",
  "nickname": "테스트유저",
  "accountType": "USER",
  "approvalStatus": "PENDING",
  "address": "서울시 강남구",
  "affiliation": "소프트웨어캠퍼스",
  "position": "수강생"
}
```

**Error Response (400 Bad Request - Validation):**
```json
{
  "type": "https://api.softwarecampus.com/problems/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "요청 본문에 유효하지 않은 필드가 있습니다.",
  "errors": {
    "email": "유효한 이메일 형식이 아닙니다",
    "password": "비밀번호는 8~20자여야 합니다"
  }
}
```

**Error Response (409 Conflict - Duplicate Email):**
```json
{
  "type": "https://api.softwarecampus.com/problems/duplicate-email",
  "title": "Duplicate Email",
  "status": 409,
  "detail": "이미 사용 중인 이메일입니다: test@example.com"
}
```

### 검증 방법
- Phase 8 Controller 테스트에서 검증

---

## Phase 8: Controller 슬라이스 테스트 (@WebMvcTest)

### 목표
- MockMvc로 HTTP 요청/응답 테스트
- Service 레이어는 @MockBean으로 모킹

### 생성 파일
```
test/.../controller/user/
  └─ AuthControllerTest.java
```

### 구현 내용

#### `AuthControllerTest.java`
```java
package com.softwarecampus.backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.dto.user.request.SignupRequest;
import com.softwarecampus.backend.dto.user.response.AccountResponse;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.exception.DuplicateEmailException;
import com.softwarecampus.backend.service.user.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController 슬라이스 테스트")
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AccountService accountService;
    
    @Test
    @DisplayName("회원가입 API 성공 - 201 Created")
    void signup_success() throws Exception {
        // given
        SignupRequest request = new SignupRequest(
            "test@example.com",
            "password123!",
            "테스트유저",
            null, null, null
        );
        
        AccountResponse response = new AccountResponse(
            1L,
            "test@example.com",
            "테스트유저",
            AccountType.USER,
            ApprovalStatus.PENDING,
            null, null, null
        );
        
        given(accountService.signup(any(SignupRequest.class))).willReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.nickname").value("테스트유저"))
            .andExpect(jsonPath("$.accountType").value("USER"));
    }
    
    @Test
    @DisplayName("회원가입 API 실패 - 400 Bad Request (Validation)")
    void signup_fail_validation() throws Exception {
        // given - 잘못된 이메일 형식
        SignupRequest request = new SignupRequest(
            "invalid-email",
            "password123!",
            "테스트유저",
            null, null, null
        );
        
        // when & then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.status").value(400));
    }
    
    @Test
    @DisplayName("회원가입 API 실패 - 409 Conflict (이메일 중복)")
    void signup_fail_duplicateEmail() throws Exception {
        // given
        SignupRequest request = new SignupRequest(
            "duplicate@example.com",
            "password123!",
            "테스트유저",
            null, null, null
        );
        
        given(accountService.signup(any(SignupRequest.class)))
            .willThrow(new DuplicateEmailException("duplicate@example.com"));
        
        // when & then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").exists())
            .andExpect(jsonPath("$.title").value("Duplicate Email"))
            .andExpect(jsonPath("$.status").value(409));
    }
}
```

### 검증 방법
```bash
./mvnw test -Dtest=AuthControllerTest
```

---

## Phase 9: Repository 테스트 (@DataJpaTest)

### 목표
- Repository 쿼리 메서드 동작 검증
- 실제 DB(H2) 사용

### 생성 파일
```
test/.../repository/user/
  └─ AccountRepositoryTest.java
```

### 구현 내용

#### `AccountRepositoryTest.java`
```java
package com.softwarecampus.backend.repository.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("AccountRepository 테스트")
class AccountRepositoryTest {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Test
    @DisplayName("이메일로 계정 조회 성공")
    void findByEmail_success() {
        // given
        Account account = createAccount("test@example.com", "테스트유저");
        accountRepository.save(account);
        
        // when
        Optional<Account> found = accountRepository.findByEmail("test@example.com");
        
        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getNickname()).isEqualTo("테스트유저");
    }
    
    @Test
    @DisplayName("이메일 존재 여부 확인")
    void existsByEmail() {
        // given
        Account account = createAccount("exists@example.com", "존재유저");
        accountRepository.save(account);
        
        // when & then
        assertThat(accountRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(accountRepository.existsByEmail("notexist@example.com")).isFalse();
    }
    
    @Test
    @DisplayName("닉네임으로 계정 조회")
    void findByNickname() {
        // given
        Account account = createAccount("test@example.com", "유니크닉네임");
        accountRepository.save(account);
        
        // when
        Optional<Account> found = accountRepository.findByNickname("유니크닉네임");
        
        // then
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("유니크닉네임");
    }
    
    @Test
    @DisplayName("계정 타입으로 계정 목록 조회")
    void findByAccountType() {
        // given
        accountRepository.save(createAccount("user1@example.com", "유저1", AccountType.USER));
        accountRepository.save(createAccount("user2@example.com", "유저2", AccountType.USER));
        accountRepository.save(createAccount("academy@example.com", "학원", AccountType.ACADEMY));
        
        // when
        List<Account> users = accountRepository.findByAccountType(AccountType.USER);
        List<Account> academies = accountRepository.findByAccountType(AccountType.ACADEMY);
        
        // then
        assertThat(users).hasSize(2);
        assertThat(academies).hasSize(1);
    }
    
    @Test
    @DisplayName("승인 상태로 계정 목록 조회")
    void findByApprovalStatus() {
        // given
        accountRepository.save(createAccountWithStatus("pending@example.com", ApprovalStatus.PENDING));
        accountRepository.save(createAccountWithStatus("approved@example.com", ApprovalStatus.APPROVED));
        
        // when
        List<Account> pending = accountRepository.findByApprovalStatus(ApprovalStatus.PENDING);
        List<Account> approved = accountRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        
        // then
        assertThat(pending).hasSize(1);
        assertThat(approved).hasSize(1);
    }
    
    // 테스트 헬퍼 메서드
    private Account createAccount(String email, String nickname) {
        return createAccount(email, nickname, AccountType.USER);
    }
    
    private Account createAccount(String email, String nickname, AccountType accountType) {
        return Account.builder()
            .email(email)
            .password("encoded_password")
            .nickname(nickname)
            .accountType(accountType)
            .approvalStatus(ApprovalStatus.PENDING)
            .build();
    }
    
    private Account createAccountWithStatus(String email, ApprovalStatus status) {
        return Account.builder()
            .email(email)
            .password("encoded_password")
            .nickname("테스트유저")
            .accountType(AccountType.USER)
            .approvalStatus(status)
            .build();
    }
}
```

### 검증 방법
```bash
./mvnw test -Dtest=AccountRepositoryTest
```

---

## Phase 10: 통합 테스트 (회원가입 E2E)

### 목표
- 실제 Spring Context로 전체 플로우 검증
- DB까지 포함한 E2E 테스트

### 생성 파일
```
test/.../integration/
  └─ SignupIntegrationTest.java
```

### 구현 내용

#### `SignupIntegrationTest.java`
```java
package com.softwarecampus.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.dto.user.request.SignupRequest;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("회원가입 통합 테스트")
class SignupIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @AfterEach
    void tearDown() {
        accountRepository.deleteAll();
    }
    
    @Test
    @DisplayName("회원가입 전체 플로우 성공")
    void signup_e2e_success() throws Exception {
        // given
        SignupRequest request = new SignupRequest(
            "integration@example.com",
            "password123!",
            "통합테스트유저",
            "서울시 강남구",
            "소프트웨어캠퍼스",
            "수강생"
        );
        
        // when
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("integration@example.com"))
            .andExpect(jsonPath("$.nickname").value("통합테스트유저"))
            .andExpect(jsonPath("$.accountType").value("USER"))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING"));
        
        // then - DB에 실제로 저장되었는지 확인
        assertThat(accountRepository.existsByEmail("integration@example.com")).isTrue();
        
        var savedAccount = accountRepository.findByEmail("integration@example.com").get();
        assertThat(savedAccount.getPassword()).isNotEqualTo("password123!"); // 암호화 확인
        assertThat(savedAccount.getAddress()).isEqualTo("서울시 강남구");
    }
    
    @Test
    @DisplayName("회원가입 실패 - 이메일 중복 (409)")
    void signup_e2e_fail_duplicate() throws Exception {
        // given - 먼저 회원가입
        SignupRequest firstRequest = new SignupRequest(
            "duplicate@example.com",
            "password123!",
            "첫번째유저",
            null, null, null
        );
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
            .andExpect(status().isCreated());
        
        // when - 같은 이메일로 다시 회원가입 시도
        SignupRequest secondRequest = new SignupRequest(
            "duplicate@example.com",
            "password456!",
            "두번째유저",
            null, null, null
        );
        
        // then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Duplicate Email"));
        
        // DB에는 1개만 존재해야 함
        assertThat(accountRepository.count()).isEqualTo(1);
    }
}
```

### 검증 방법
```bash
./mvnw test -Dtest=SignupIntegrationTest
```

---

## Phase 11-20: JWT 및 로그인/마이페이지 (상세 계획은 Phase 10 완료 후 작성)

### Phase 11: JWT 구현
- JwtTokenProvider.java
- JwtProperties.java
- application.properties 설정

### Phase 12: JWT 단위 테스트
- JwtTokenProviderTest.java

### Phase 13: UserDetailsService 구현
- CustomUserDetailsService.java

### Phase 14: UserDetailsService 테스트
- CustomUserDetailsServiceTest.java

### Phase 15: Security 고도화
- JwtAuthenticationFilter.java
- SecurityFilterChain 완성

### Phase 16: 로그인 API
- LoginRequest/Response
- AuthService 로그인 메서드
- AuthController 로그인 엔드포인트

### Phase 17: 로그인 테스트
- Service + Controller 테스트

### Phase 18: 마이페이지 API
- MyPageController.java

### Phase 19: 마이페이지 테스트
- @WithMockUser 사용

### Phase 20: 통합 테스트 (전체 플로우)
- 회원가입 → 로그인 → JWT → 마이페이지

---

## 📝 작업 진행 체크리스트

- [x] Phase 1: Domain & Repository
- [ ] Phase 2: GlobalExceptionHandler 기본 틀
- [ ] Phase 3: 기본 보안 설정
- [ ] Phase 4: DTO Layer
- [ ] Phase 5: Service Layer + 도메인 예외
- [ ] Phase 6: Service 단위 테스트
- [ ] Phase 7: Controller Layer
- [ ] Phase 8: Controller 슬라이스 테스트
- [ ] Phase 9: Repository 테스트
- [ ] Phase 10: 통합 테스트 (회원가입)
- [ ] Phase 11-20: JWT 및 로그인/마이페이지

---

## 🎯 각 Phase별 검증 기준

### Phase 완료 기준
1. **코드 작성 완료** - 해당 Phase의 모든 파일 생성
2. **테스트 통과** - 관련 테스트가 모두 green
3. **빌드 성공** - `./mvnw clean verify` 통과
4. **문서 갱신** - work-history.md에 작업 내역 기록

### 다음 Phase로 넘어가는 조건
- 현재 Phase의 모든 검증 기준 충족
- 코드 리뷰 완료 (필요 시)
- Git 커밋 완료

---

**작성일:** 2025-10-29  
**최종 수정:** 2025-10-29  
**상태:** Phase 1 완료, Phase 2 진행 예정
