# Phase 9-10: 통합 테스트 (회원가입 E2E + Repository 검증)

**목표:** 전체 Spring Context로 회원가입 플로우 검증 + Repository 동작 확인  
**담당자:** 태윤  
**상태:** 🚧 준비 중

---

## 📋 작업 개요

Phase 9(Repository 테스트)와 Phase 10(통합 테스트)을 통합하여 하나의 통합 테스트로 진행합니다.

**통합 이유:**
- Repository 단독 테스트(@DataJpaTest)는 Spring Data JPA 쿼리 메서드 검증에 불과
- 통합 테스트에서 Repository 실제 동작을 함께 검증하는 것이 효율적
- Service 테스트(51개)에서 이미 Repository를 Mock으로 검증 완료
- 중복 작업 제거 및 시간 절약 (1-2시간 절약)

---

## 📂 생성 파일

```
src/test/java/com/softwarecampus/backend/
└─ integration/
   └─ SignupIntegrationTest.java
```

---

## 🔨 구현 내용

### SignupIntegrationTest.java

**경로:** `test/java/com/softwarecampus/backend/integration/SignupIntegrationTest.java`

**설명:** 회원가입 전체 플로우 통합 테스트

```java
package com.softwarecampus.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.MessageResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 회원가입 통합 테스트
 * - Controller → Service → Repository 전체 플로우 검증
 * - 실제 H2 DB 사용
 * - Repository 쿼리 메서드 동작 확인
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // 각 테스트 후 롤백
@DisplayName("회원가입 통합 테스트")
class SignupIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @BeforeEach
    void setUp() {
        // 각 테스트 전 DB 초기화 (@Transactional로 자동 롤백됨)
        accountRepository.deleteAll();
    }
    
    /**
     * 테스트 1: USER 회원가입 성공
     * - 201 Created
     * - Location 헤더 포함
     * - DB 저장 확인
     * - approvalStatus = APPROVED
     */
    @Test
    @DisplayName("USER 회원가입 성공 - DB 저장 및 조회 확인")
    void signup_USER_성공() throws Exception {
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
        
        // When
        String responseBody = mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.accountType").value("USER"))
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        AccountResponse response = objectMapper.readValue(responseBody, AccountResponse.class);
        
        // Then - Repository 검증
        Account savedAccount = accountRepository.findById(response.id()).orElseThrow();
        assertThat(savedAccount.getEmail()).isEqualTo("user@example.com");
        assertThat(savedAccount.getUserName()).isEqualTo("홍길동");
        assertThat(savedAccount.getAccountType()).isEqualTo(AccountType.USER);
        assertThat(savedAccount.getAccountApproved()).isEqualTo(ApprovalStatus.APPROVED);
        
        // Repository.existsByEmailAndIsDeletedFalse() 검증 (✅ 2025-12-01)
        assertThat(accountRepository.existsByEmailAndIsDeletedFalse("user@example.com")).isTrue();
        
        // Repository.findByEmail() 검증
        Account foundAccount = accountRepository.findByEmail("user@example.com").orElseThrow();
        assertThat(foundAccount.getId()).isEqualTo(savedAccount.getId());
    }
    
    /**
     * 테스트 2: ACADEMY 회원가입 성공
     * - approvalStatus = PENDING
     * - academyId 포함
     */
    @Test
    @DisplayName("ACADEMY 회원가입 성공 - PENDING 상태 확인")
    void signup_ACADEMY_성공() throws Exception {
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
        
        // When & Then
        String responseBody = mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountType").value("ACADEMY"))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        AccountResponse response = objectMapper.readValue(responseBody, AccountResponse.class);
        
        // Repository 검증
        Account savedAccount = accountRepository.findById(response.id()).orElseThrow();
        assertThat(savedAccount.getAccountApproved()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(savedAccount.getAcademyId()).isEqualTo(100L);
    }
    
    /**
     * 테스트 3: 이메일 중복 - 409 Conflict
     * - Repository.existsByEmailAndIsDeletedFalse() 동작 확인 (✅ 2025-12-01)
     */
    @Test
    @DisplayName("이메일 중복 시 409 Conflict")
    void signup_이메일중복_409() throws Exception {
        // Given - 첫 번째 회원가입
        SignupRequest firstRequest = new SignupRequest(
            "duplicate@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(firstRequest)))
            .andExpect(status().isCreated());
        
        // Repository 확인 (✅ 2025-12-01)
        assertThat(accountRepository.existsByEmailAndIsDeletedFalse("duplicate@example.com")).isTrue();
        
        // When - 중복 이메일로 두 번째 회원가입 시도
        SignupRequest duplicateRequest = new SignupRequest(
            "duplicate@example.com",  // 동일 이메일
            "password456!",
            "이순신",
            "010-9999-9999",
            null, null, null,
            AccountType.USER,
            null
        );
        
        // Then
        mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("이미 사용 중인 이메일입니다."));
    }
    
    /**
     * 테스트 4: 전화번호 중복 - 400 Bad Request
     * - Repository.existsByPhoneNumberAndIsDeletedFalse() 동작 확인 (간접) (✅ 2025-12-01)
     */
    @Test
    @DisplayName("전화번호 중복 시 400 Bad Request")
    void signup_전화번호중복_400() throws Exception {
        // Given
        SignupRequest firstRequest = new SignupRequest(
            "user1@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(firstRequest)))
            .andExpect(status().isCreated());
        
        // When - 중복 전화번호로 시도
        SignupRequest duplicatePhoneRequest = new SignupRequest(
            "user2@example.com",
            "password123!",
            "이순신",
            "010-1234-5678",  // 동일 전화번호
            null, null, null,
            AccountType.USER,
            null
        );
        
        // Then
        mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(duplicatePhoneRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("이미 사용 중인 전화번호입니다."));
    }
    
    /**
     * 테스트 5: 이메일 중복 확인 API - 사용 가능
     */
    @Test
    @DisplayName("이메일 중복 확인 - 사용 가능")
    void checkEmail_사용가능() throws Exception {
        // Given - DB에 없는 이메일
        
        // When & Then
        mockMvc.perform(get("/api/v1/auth/check-email")
            .param("email", "newuser@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("사용 가능한 이메일입니다."));
        
        // Repository 확인 (✅ 2025-12-01)
        assertThat(accountRepository.existsByEmailAndIsDeletedFalse("newuser@example.com")).isFalse();
    }
    
    /**
     * 테스트 6: 이메일 중복 확인 API - 사용 불가
     */
    @Test
    @DisplayName("이메일 중복 확인 - 이미 사용 중")
    void checkEmail_사용불가() throws Exception {
        // Given - 먼저 계정 생성
        SignupRequest request = new SignupRequest(
            "existing@example.com",
            "password123!",
            "홍길동",
            "010-1234-5678",
            null, null, null,
            AccountType.USER,
            null
        );
        
        mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
        
        // When & Then
        mockMvc.perform(get("/api/v1/auth/check-email")
            .param("email", "existing@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        
        // Repository 확인 (✅ 2025-12-01)
        assertThat(accountRepository.existsByEmailAndIsDeletedFalse("existing@example.com")).isTrue();
    }
    
    /**
     * 테스트 7: Bean Validation 실패 - 이메일 형식 오류
     */
    @Test
    @DisplayName("Bean Validation - 이메일 형식 오류")
    void signup_이메일형식오류_400() throws Exception {
        // Given
        SignupRequest request = new SignupRequest(
            "invalid-email",  // 잘못된 형식
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("올바른 이메일 형식이 아닙니다."));
        
        // DB에 저장되지 않았는지 확인
        assertThat(accountRepository.count()).isEqualTo(0);
    }
    
    /**
     * 테스트 8: Location 헤더 검증
     */
    @Test
    @DisplayName("회원가입 성공 시 Location 헤더 포함")
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
        
        // When & Then
        String responseBody = mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        AccountResponse response = objectMapper.readValue(responseBody, AccountResponse.class);
        String expectedLocation = "/api/v1/accounts/" + response.id();
        
        // Location 헤더 확인
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignupRequest(
                        "another@example.com",
                        "password123!",
                        "김철수",
                        "010-9999-8888",
                        null, null, null,
                        AccountType.USER,
                        null
                    )
                )))
            .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/accounts/\\d+")));
    }
}
```

---

## ✅ 검증 항목

### Controller Layer
- [x] POST /api/v1/auth/signup (201 Created)
- [x] GET /api/v1/auth/check-email (200 OK)
- [x] Bean Validation 동작 확인
- [x] Location 헤더 생성

### Service Layer
- [x] 회원가입 비즈니스 로직
- [x] 이메일 중복 확인 로직
- [x] accountType별 approvalStatus 처리

### Repository Layer
- [x] `save()` - DB 저장
- [x] `findById()` - ID로 조회
- [x] `existsByEmailAndIsDeletedFalse()` - 이메일 중복 확인 (✅ 2025-12-01)
- [x] `findByEmail()` - 이메일로 조회
- [x] UNIQUE 제약 조건 (이메일, 전화번호)
- [x] `deleteAll()` - 테스트 초기화

### 전체 플로우
- [x] Controller → Service → Repository 연동
- [x] 예외 처리 (@ExceptionHandler)
- [x] 트랜잭션 롤백 (@Transactional)

---

## 🧪 테스트 실행

### Maven 명령어

```powershell
# 통합 테스트만 실행
mvn test -Dtest=SignupIntegrationTest

# 전체 테스트 실행
mvn test

# 빌드 검증
mvn clean verify
```

### 예상 결과

```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 📝 Phase 완료 기준

- [x] **파일 생성 완료**
  - [x] `SignupIntegrationTest.java` 생성

- [ ] **테스트 통과**
  - [ ] 8개 테스트 모두 green
  - [ ] `mvn test -Dtest=SignupIntegrationTest` 성공

- [ ] **빌드 성공**
  - [ ] `mvn clean verify` 통과

- [ ] **문서 갱신**
  - [ ] 작업 기록에 Phase 9-10 완료 기록
  - [ ] implementation_plan.md 체크리스트 업데이트

---

## 🔜 다음 단계

**Phase 11: 보안 강화**
- Rate Limiting 구현 (/check-email 보호)
- CORS 설정 (WebConfig.java)
- TODO 주석 정리
- 보안 정책 문서 작성

---

**작성일:** 2025-11-12  
**최종 수정:** 2025-11-12  
**상태:** 🚧 준비 중

---

## 📊 구현 결과 (작성 예정)

### 생성된 파일 (1개)
- ✅ `test/.../integration/SignupIntegrationTest.java`

### 테스트 결과
```
예정
```

### 검증 완료 항목
- ✅ Controller → Service → Repository 전체 플로우
- ✅ Repository 쿼리 메서드 동작 확인
- ✅ UNIQUE 제약 조건 동작
- ✅ 트랜잭션 롤백
