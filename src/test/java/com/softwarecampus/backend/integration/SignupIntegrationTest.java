package com.softwarecampus.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 9-10: 회원가입 통합 테스트 (E2E + Repository 검증)
 * 
 * 목표:
 * - 전체 Spring Context로 회원가입 플로우 E2E 검증
 * - Repository 쿼리 메서드 동작 확인
 * - 실제 DB 저장 및 조회 검증
 * - UNIQUE 제약 조건 동작 확인
 * 
 * 테스트 시나리오:
 * 1. 회원가입 성공 (USER)
 * 2. 회원가입 성공 (ACADEMY)
 * 3. 이메일 중복 확인 (Repository.existsByEmail 검증)
 * 4. DB 저장 확인 (Repository.save + findByEmail 검증)
 * 5. 전화번호 중복 확인 (Repository.existsByPhoneNumber 검증)
 * 6. 이메일 중복 확인 API (/check-email)
 * 
 * @author 태윤
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
@ActiveProfiles("test")
@Transactional // 각 테스트 후 롤백
@DisplayName("회원가입 통합 테스트 (E2E + Repository)")
class SignupIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // 각 테스트 전 DB 초기화 (Transactional로 자동 롤백)
        accountRepository.deleteAll();
    }
    
    @Test
    @DisplayName("회원가입 성공 (USER) - E2E 플로우")
    void signup_성공_USER타입() throws Exception {
        // given
        SignupRequest request = new SignupRequest(
            "user@test.com",
            "Password123!",
            "홍길동",
            "010-1234-5678",
            "서울시 강남구",
            "소속없음",
            "개발자",
            AccountType.USER,
            null
        );
        
        // when & then - HTTP 요청/응답 검증
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.email").value("user@test.com"))
            .andExpect(jsonPath("$.userName").value("홍길동"))
            .andExpect(jsonPath("$.accountType").value("USER"))
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
        
        // Repository 검증: DB 저장 확인
        Account savedAccount = accountRepository.findByEmail("user@test.com")
            .orElseThrow(() -> new AssertionError("저장된 계정을 찾을 수 없습니다."));
        
        assertThat(savedAccount.getEmail()).isEqualTo("user@test.com");
        assertThat(savedAccount.getUserName()).isEqualTo("홍길동");
        assertThat(savedAccount.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(savedAccount.getAccountType()).isEqualTo(AccountType.USER);
        assertThat(savedAccount.getAccountApproved()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(savedAccount.getPassword()).isNotEqualTo("Password123!"); // 암호화 확인
        assertThat(savedAccount.getPassword()).startsWith("$2a$"); // BCrypt 확인
    }
    
    @Test
    @DisplayName("회원가입 성공 (ACADEMY) - E2E 플로우")
    void signup_성공_ACADEMY타입() throws Exception {
        // given
        SignupRequest request = new SignupRequest(
            "academy@test.com",
            "Password123!",
            "학원관리자",
            "010-9876-5432",
            "서울시 서초구",
            "ABC 학원",
            "원장",
            AccountType.ACADEMY,
            1L // academyId
        );
        
        // when & then - HTTP 요청/응답 검증
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.email").value("academy@test.com"))
            .andExpect(jsonPath("$.userName").value("학원관리자"))
            .andExpect(jsonPath("$.accountType").value("ACADEMY"))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING")); // ACADEMY는 승인 대기
        
        // Repository 검증: DB 저장 확인
        Account savedAccount = accountRepository.findByEmail("academy@test.com")
            .orElseThrow(() -> new AssertionError("저장된 계정을 찾을 수 없습니다."));
        
        assertThat(savedAccount.getEmail()).isEqualTo("academy@test.com");
        assertThat(savedAccount.getAccountType()).isEqualTo(AccountType.ACADEMY);
        assertThat(savedAccount.getAccountApproved()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(savedAccount.getAcademyId()).isEqualTo(1L);
    }
    
    @Test
    @DisplayName("이메일 중복 확인 - existsByEmail() 검증")
    void 이메일중복확인_Repository검증() throws Exception {
        // given - 기존 계정 생성
        Account existingAccount = Account.builder()
            .email("existing@test.com")
            .password("$2a$10$encodedPassword")
            .userName("기존사용자")
            .phoneNumber("010-1111-2222")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
        accountRepository.save(existingAccount);
        
        // Repository 직접 검증
        assertThat(accountRepository.existsByEmail("existing@test.com")).isTrue();
        assertThat(accountRepository.existsByEmail("new@test.com")).isFalse();
        
        // when & then - 중복 이메일로 회원가입 시도
        SignupRequest duplicateRequest = new SignupRequest(
            "existing@test.com",
            "Password123!",
            "신규사용자",
            "010-3333-4444",
            "서울시 강남구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isConflict()) // 409 Conflict
            .andExpect(jsonPath("$.title").value("Duplicate Email"))
            .andExpect(jsonPath("$.status").value(409));
    }
    
    @Test
    @DisplayName("전화번호 중복 확인 - existsByPhoneNumber() 검증")
    void 전화번호중복확인_Repository검증() throws Exception {
        // given - 기존 계정 생성
        Account existingAccount = Account.builder()
            .email("existing@test.com")
            .password("$2a$10$encodedPassword")
            .userName("기존사용자")
            .phoneNumber("010-1234-5678")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
        accountRepository.save(existingAccount);
        
        // Repository 직접 검증
        assertThat(accountRepository.existsByPhoneNumber("010-1234-5678")).isTrue();
        assertThat(accountRepository.existsByPhoneNumber("010-9999-8888")).isFalse();
        
        // when & then - 중복 전화번호로 회원가입 시도
        SignupRequest duplicateRequest = new SignupRequest(
            "newuser@test.com",
            "Password123!",
            "신규사용자",
            "010-1234-5678", // 중복 전화번호
            "서울시 강남구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isBadRequest()) // 400 Bad Request
            .andExpect(jsonPath("$.title").value("Invalid Input"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("이미 사용 중인 전화번호입니다."));
    }
    
    @Test
    @DisplayName("이메일 중복 확인 API - /check-email")
    void checkEmail_API_검증() throws Exception {
        // given - 기존 계정 생성
        Account existingAccount = Account.builder()
            .email("existing@test.com")
            .password("$2a$10$encodedPassword")
            .userName("기존사용자")
            .phoneNumber("010-1234-5678")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
        accountRepository.save(existingAccount);
        
        // when & then - 중복 이메일 확인
        mockMvc.perform(get("/api/auth/check-email")
                .param("email", "existing@test.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        
        // when & then - 사용 가능한 이메일 확인
        mockMvc.perform(get("/api/auth/check-email")
                .param("email", "available@test.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("사용 가능한 이메일입니다."));
    }
    
    @Test
    @DisplayName("DB 저장 및 조회 - save() + findByEmail() 검증")
    void DB저장조회_Repository검증() {
        // given
        Account newAccount = Account.builder()
            .email("repository@test.com")
            .password("$2a$10$encodedPassword")
            .userName("레포지토리테스트")
            .phoneNumber("010-5555-6666")
            .accountType(AccountType.USER)
            .accountApproved(ApprovalStatus.APPROVED)
            .build();
        
        // when - save() 검증
        Account savedAccount = accountRepository.save(newAccount);
        
        // then - save() 결과 확인
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getEmail()).isEqualTo("repository@test.com");
        assertThat(savedAccount.getCreatedAt()).isNotNull();
        
        // when - findByEmail() 검증
        Account foundAccount = accountRepository.findByEmail("repository@test.com")
            .orElseThrow(() -> new AssertionError("계정을 찾을 수 없습니다."));
        
        // then - findByEmail() 결과 확인
        assertThat(foundAccount.getId()).isEqualTo(savedAccount.getId());
        assertThat(foundAccount.getEmail()).isEqualTo("repository@test.com");
        assertThat(foundAccount.getUserName()).isEqualTo("레포지토리테스트");
    }
    
    @Test
    @DisplayName("이메일 형식 검증 - Bean Validation")
    void 이메일형식검증_BeanValidation() throws Exception {
        // given - 잘못된 이메일 형식
        SignupRequest invalidRequest = new SignupRequest(
            "invalid-email", // 잘못된 형식
            "Password123!",
            "홍길동",
            "010-1234-5678",
            "서울시 강남구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        // when & then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Validation Failed"));
    }
    
    @Test
    @DisplayName("ADMIN 계정 회원가입 차단")
    void ADMIN계정_회원가입차단() throws Exception {
        // given
        SignupRequest adminRequest = new SignupRequest(
            "admin@test.com",
            "Password123!",
            "관리자",
            "010-1234-5678",
            "서울시 강남구",
            null,
            null,
            AccountType.ADMIN, // ADMIN 타입 시도
            null
        );
        
        // when & then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid Input"))
            .andExpect(jsonPath("$.detail").value("관리자 계정은 회원가입으로 생성할 수 없습니다."));
    }
    
    @Test
    @DisplayName("ACADEMY 타입 academyId 누락 시 오류")
    void ACADEMY타입_academyId누락_오류() throws Exception {
        // given
        SignupRequest academyRequest = new SignupRequest(
            "academy@test.com",
            "Password123!",
            "학원관리자",
            "010-1234-5678",
            "서울시 강남구",
            "ABC 학원",
            "원장",
            AccountType.ACADEMY,
            null // academyId 누락
        );
        
        // when & then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(academyRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.detail").value("요청 본문에 유효하지 않은 필드가 있습니다."));
    }
    
    @Test
    @DisplayName("전체 플로우 검증 - 회원가입 → DB 저장 → 이메일 중복 확인")
    void 전체플로우_E2E검증() throws Exception {
        // Step 1: 회원가입
        SignupRequest request = new SignupRequest(
            "fullflow@test.com",
            "Password123!",
            "플로우테스트",
            "010-7777-8888",
            "서울시 강남구",
            null,
            null,
            AccountType.USER,
            null
        );
        
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
        
        // Step 2: DB 저장 확인
        assertThat(accountRepository.existsByEmail("fullflow@test.com")).isTrue();
        Account savedAccount = accountRepository.findByEmail("fullflow@test.com")
            .orElseThrow(() -> new AssertionError("계정이 저장되지 않았습니다."));
        assertThat(savedAccount.getUserName()).isEqualTo("플로우테스트");
        
        // Step 3: 이메일 중복 확인 API
        mockMvc.perform(get("/api/auth/check-email")
                .param("email", "fullflow@test.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        
        // Step 4: 동일 이메일로 재시도
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }
}
