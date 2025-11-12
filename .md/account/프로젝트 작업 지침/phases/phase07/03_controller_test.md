# 3. Controller 통합 테스트

**경로:** `test/java/com/softwarecampus/backend/controller/user/AuthControllerTest.java`

**설명:** AuthController의 HTTP 요청/응답 통합 테스트

---

## 📋 테스트 개요

AuthController의 API 엔드포인트를 MockMvc로 테스트합니다:
- POST /api/v1/auth/signup (회원가입)
- GET /api/v1/auth/check-email (이메일 중복 확인)
- HTTP 상태 코드, Location 헤더, 응답 Body 검증
- Service Layer 모킹 (`@MockBean`)

---

## 🔧 전체 코드

```java
package com.softwarecampus.backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.exception.user.DuplicateEmailException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.service.user.signup.SignupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트
 * 
 * 테스트 대상:
 * - POST /api/v1/auth/signup: 회원가입
 * - GET /api/v1/auth/check-email: 이메일 중복 확인
 * 
 * 테스트 도구:
 * - @WebMvcTest: Controller Layer만 로드
 * - MockMvc: HTTP 요청/응답 모킹
 * - @MockBean: Service Layer 모킹
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private SignupService signupService;
    
    @Test
    @DisplayName("POST /signup - 회원가입 성공 (USER)")
    void signup_성공_USER() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            "서울시 강남구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        AccountResponse response = new AccountResponse(
            1L,
            "user@example.com",
            "홍길동",
            "010-1234-5678",
            "서울시 강남구",
            null,
            null,
            AccountType.USER,
            ApprovalStatus.APPROVED,
            null,
            LocalDateTime.now()
        );
        
        when(signupService.signup(any(SignupRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/accounts/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.userName").value("홍길동"))
            .andExpect(jsonPath("$.accountType").value("USER"))
            .andExpect(jsonPath("$.accountApproved").value("APPROVED"));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - 회원가입 성공 (ACADEMY)")
    void signup_성공_ACADEMY() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "teacher@example.com",
            "password123!",
            "김선생",
            "010-9876-5432",
            "서울시 서초구",
            "ABC학원",
            "수학 강사",
            AccountType.ACADEMY,
            100L
        );
        
        AccountResponse response = new AccountResponse(
            2L,
            "teacher@example.com",
            "김선생",
            "010-9876-5432",
            "서울시 서초구",
            "ABC학원",
            "수학 강사",
            AccountType.ACADEMY,
            ApprovalStatus.PENDING,
            100L,
            LocalDateTime.now()
        );
        
        when(signupService.signup(any(SignupRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/accounts/2"))
            .andExpect(jsonPath("$.accountType").value("ACADEMY"))
            .andExpect(jsonPath("$.accountApproved").value("PENDING"))
            .andExpect(jsonPath("$.academyId").value(100));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - Bean Validation 실패 (이메일 누락)")
    void signup_BeanValidation실패_이메일누락() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            null,  // email 누락
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
        
        // Service 호출되지 않음
        verify(signupService, never()).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - 이메일 형식 오류 (RFC 5322 위반)")
    void signup_이메일형식오류() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "invalid-email",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        when(signupService.signup(any(SignupRequest.class)))
            .thenThrow(new InvalidInputException("올바른 이메일 형식이 아닙니다."));
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("올바른 이메일 형식이 아닙니다."));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - 이메일 중복 (409 Conflict)")
    void signup_이메일중복() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        when(signupService.signup(any(SignupRequest.class)))
            .thenThrow(new DuplicateEmailException("이미 사용 중인 이메일입니다."));
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("이미 사용 중인 이메일입니다."));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - 전화번호 중복 (400 Bad Request)")
    void signup_전화번호중복() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        when(signupService.signup(any(SignupRequest.class)))
            .thenThrow(new InvalidInputException("이미 사용 중인 전화번호입니다."));
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("이미 사용 중인 전화번호입니다."));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - ADMIN 계정 차단")
    void signup_ADMIN_차단() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "admin@example.com",
            "password123!",
            "관리자",
            "010-0000-0000",
            null, null, null,
            AccountType.ADMIN,
            null
        );
        
        when(signupService.signup(any(SignupRequest.class)))
            .thenThrow(new InvalidInputException("관리자 계정은 회원가입으로 생성할 수 없습니다."));
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("관리자 계정은 회원가입으로 생성할 수 없습니다."));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("POST /signup - ACADEMY academyId 누락")
    void signup_ACADEMY_academyId없음() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "teacher@example.com",
            "password123!",
            "김선생",
            "010-9876-5432",
            null,
            "ABC학원",
            "강사",
            AccountType.ACADEMY,
            null  // academyId 누락
        );
        
        when(signupService.signup(any(SignupRequest.class)))
            .thenThrow(new InvalidInputException("기관 회원은 기관 ID가 필수입니다."));
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("기관 회원은 기관 ID가 필수입니다."));
        
        verify(signupService).signup(any(SignupRequest.class));
    }
    
    @Test
    @DisplayName("GET /check-email - 사용 가능 (200)")
    void checkEmail_사용가능() throws Exception {
        // Given
        when(signupService.isEmailAvailable("newuser@example.com")).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/v1/auth/check-email")
                .param("email", "newuser@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("사용 가능한 이메일입니다."));
        
        verify(signupService).isEmailAvailable("newuser@example.com");
    }
    
    @Test
    @DisplayName("GET /check-email - 사용 불가 (200)")
    void checkEmail_사용불가() throws Exception {
        // Given
        when(signupService.isEmailAvailable("user@example.com")).thenReturn(false);
        
        // When & Then
        mockMvc.perform(get("/api/v1/auth/check-email")
                .param("email", "user@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        
        verify(signupService).isEmailAvailable("user@example.com");
    }
    
    @Test
    @DisplayName("GET /check-email - 이메일 형식 오류 (400)")
    void checkEmail_이메일형식오류() throws Exception {
        // Given
        when(signupService.isEmailAvailable("invalid-email"))
            .thenThrow(new InvalidInputException("올바른 이메일 형식이 아닙니다."));
        
        // When & Then
        mockMvc.perform(get("/api/v1/auth/check-email")
                .param("email", "invalid-email"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("올바른 이메일 형식이 아닙니다."));
        
        verify(signupService).isEmailAvailable("invalid-email");
    }
    
    @Test
    @DisplayName("POST /signup - Location 헤더 검증")
    void signup_Location헤더검증() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "user@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        AccountResponse response = new AccountResponse(
            123L,  // accountId
            "user@example.com",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            ApprovalStatus.APPROVED,
            null,
            LocalDateTime.now()
        );
        
        when(signupService.signup(any(SignupRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/accounts/123"));
    }
}
```

---

## 📊 테스트 시나리오

| 번호 | 테스트명 | 검증 내용 | 예상 결과 |
|------|----------|----------|----------|
| 1 | signup_성공_USER | USER 회원가입 성공 | 201 Created, APPROVED |
| 2 | signup_성공_ACADEMY | ACADEMY 회원가입 성공 | 201 Created, PENDING |
| 3 | signup_BeanValidation실패 | 이메일 누락 | 400 Bad Request |
| 4 | signup_이메일형식오류 | RFC 5322 위반 | 400 Bad Request |
| 5 | signup_이메일중복 | 이메일 중복 | 409 Conflict |
| 6 | signup_전화번호중복 | 전화번호 중복 | 400 Bad Request |
| 7 | signup_ADMIN_차단 | ADMIN 회원가입 시도 | 400 Bad Request |
| 8 | signup_ACADEMY_academyId없음 | academyId 누락 | 400 Bad Request |
| 9 | checkEmail_사용가능 | 이메일 사용 가능 | 200 OK |
| 10 | checkEmail_사용불가 | 이메일 중복 | 200 OK |
| 11 | checkEmail_이메일형식오류 | 형식 오류 | 400 Bad Request |
| 12 | signup_Location헤더검증 | Location 헤더 | `/api/v1/accounts/{id}` |

---

## 🎯 검증 포인트

### 1. MockMvc 사용법

```java
mockMvc.perform(post("/api/v1/auth/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
    .andExpect(status().isCreated())
    .andExpect(header().string("Location", "/api/v1/accounts/1"))
    .andExpect(jsonPath("$.id").value(1));
```

### 2. Service Layer 모킹

```java
@MockBean
private SignupService signupService;

// 정상 응답
when(signupService.signup(any(SignupRequest.class))).thenReturn(response);

// 예외 발생
when(signupService.signup(any(SignupRequest.class)))
    .thenThrow(new DuplicateEmailException("이미 사용 중인 이메일입니다."));
```

### 3. HTTP 상태 코드 검증

```java
.andExpect(status().isCreated())        // 201
.andExpect(status().isOk())             // 200
.andExpect(status().isBadRequest())     // 400
.andExpect(status().isConflict())       // 409
```

### 4. JSON 응답 검증

```java
.andExpect(jsonPath("$.id").value(1))
.andExpect(jsonPath("$.email").value("user@example.com"))
.andExpect(jsonPath("$.accountType").value("USER"))
```

---

## 📝 주요 패턴

### @WebMvcTest

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    // Controller Layer만 로드
    // Service, Repository는 @MockBean으로 모킹
}
```

### ObjectMapper (JSON 변환)

```java
@Autowired
private ObjectMapper objectMapper;

// SignupRequest → JSON 문자열
String json = objectMapper.writeValueAsString(request);
```

### verify() 행위 검증

```java
// Service 메서드 호출 확인
verify(signupService).signup(any(SignupRequest.class));

// Service 호출되지 않음 확인
verify(signupService, never()).signup(any());
```

---

## ✅ 완료 체크리스트

- [ ] `@WebMvcTest(AuthController.class)` 적용
- [ ] MockMvc 주입 (`@Autowired`)
- [ ] ObjectMapper 주입 (`@Autowired`)
- [ ] SignupService 모킹 (`@MockBean`)
- [ ] POST /signup 테스트 (8개)
- [ ] GET /check-email 테스트 (3개)
- [ ] Location 헤더 검증 (1개)
- [ ] HTTP 상태 코드 검증
- [ ] JSON 응답 검증 (`jsonPath`)
- [ ] `verify()` 행위 검증

---

## 🔗 관련 문서

- [AuthController 구현](01_auth_controller.md) - Controller 코드
- [API 명세서](02_api_specification.md) - 요청/응답 예시
- [보안 및 RESTful 원칙](04_security_restful.md) - 테스트 실행 명령어
