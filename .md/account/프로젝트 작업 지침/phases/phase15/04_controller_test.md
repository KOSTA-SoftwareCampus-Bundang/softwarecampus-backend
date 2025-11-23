# 4. Controller 슬라이스 테스트 (마이페이지)

**목표:** MyPageController 엔드포인트 테스트

---

## 📂 생성 파일

```
src/test/java/com/softwarecampus/backend/
└─ controller/user/
   └─ MyPageControllerTest.java
```

---

## 4.1 MyPageControllerTest.java

**경로:** `test/java/com/softwarecampus/backend/controller/user/MyPageControllerTest.java`

**설명:** MyPageController 슬라이스 테스트 (10-12개 테스트)

```java
package com.softwarecampus.backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.service.user.profile.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MyPageController 슬라이스 테스트
 * 
 * 테스트 대상:
 * - GET /api/mypage/profile: 프로필 조회
 * - PATCH /api/mypage/profile: 프로필 수정
 * 
 * Mock 대상:
 * - ProfileService: 프로필 서비스 모킹
 * 
 * 인증:
 * - @WithMockUser: Spring Security 인증 모킹
 * 
 * @author 태윤
 */
@WebMvcTest(MyPageController.class)
@Import(TestSecurityConfig.class)  // JWT 필터 비활성화
@DisplayName("MyPageController 슬라이스 테스트")
class MyPageControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ProfileService profileService;
    
    @Nested
    @DisplayName("GET /api/mypage/profile - 프로필 조회")
    class GetProfile {
        
        private AccountResponse userProfileResponse;
        
        @BeforeEach
        void setUp() {
            userProfileResponse = new AccountResponse(
                1L,
                "user@example.com",
                "홍길동",
                "010-1234-5678",
                "서울시 강남구",
                null,
                null,
                "USER",
                "APPROVED",
                LocalDateTime.now()
            );
        }
        
        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("프로필 조회 성공")
        void getProfile_Success() throws Exception {
            // given
            when(profileService.getProfile("user@example.com"))
                .thenReturn(userProfileResponse);
            
            // when & then
            mockMvc.perform(get("/api/mypage/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.userName").value("홍길동"))
                .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
                .andExpect(jsonPath("$.accountType").value("USER"))
                .andExpect(jsonPath("$.accountApproved").value("APPROVED"));
            
            verify(profileService).getProfile("user@example.com");
        }
        
        @Test
        @DisplayName("인증 없이 프로필 조회 실패")
        void getProfile_Fail_Unauthenticated() throws Exception {
            // when & then
            mockMvc.perform(get("/api/mypage/profile"))
                .andExpect(status().isUnauthorized());
            
            verify(profileService, never()).getProfile(anyString());
        }
        
        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("프로필 조회 실패 - 사용자 없음")
        void getProfile_Fail_UserNotFound() throws Exception {
            // given
            when(profileService.getProfile("user@example.com"))
                .thenThrow(new UsernameNotFoundException("사용자를 찾을 수 없습니다"));
            
            // when & then
            mockMvc.perform(get("/api/mypage/profile"))
                .andExpect(status().isUnauthorized());
        }
    }
    
    @Nested
    @DisplayName("PATCH /api/mypage/profile - 프로필 수정")
    class UpdateProfile {
        
        private UpdateProfileRequest validRequest;
        private AccountResponse updatedProfileResponse;
        
        @BeforeEach
        void setUp() {
            validRequest = new UpdateProfileRequest(
                "홍길동 (수정)",
                "010-9999-8888",
                "서울시 종로구",
                "소프트웨어 캠퍼스",
                "수강생"
            );
            
            updatedProfileResponse = new AccountResponse(
                1L,
                "user@example.com",
                "홍길동 (수정)",
                "010-9999-8888",
                "서울시 종로구",
                "소프트웨어 캠퍼스",
                "수강생",
                "USER",
                "APPROVED",
                LocalDateTime.now()
            );
        }
        
        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("프로필 수정 성공")
        void updateProfile_Success() throws Exception {
            // given
            when(profileService.updateProfile(eq("user@example.com"), any(UpdateProfileRequest.class)))
                .thenReturn(updatedProfileResponse);
            
            // when & then
            mockMvc.perform(patch("/api/mypage/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("홍길동 (수정)"))
                .andExpect(jsonPath("$.phoneNumber").value("010-9999-8888"))
                .andExpect(jsonPath("$.address").value("서울시 종로구"))
                .andExpect(jsonPath("$.affiliation").value("소프트웨어 캠퍼스"))
                .andExpect(jsonPath("$.position").value("수강생"));
            
            verify(profileService).updateProfile(eq("user@example.com"), any(UpdateProfileRequest.class));
        }
        
        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("프로필 수정 성공 - 부분 업데이트 (userName만)")
        void updateProfile_Success_PartialUpdate() throws Exception {
            // given
            UpdateProfileRequest partialRequest = new UpdateProfileRequest(
                "새이름",
                null,
                null,
                null,
                null
            );
            
            AccountResponse partialUpdateResponse = new AccountResponse(
                1L,
                "user@example.com",
                "새이름",
                "010-1234-5678",
                "서울시 강남구",
                null,
                null,
                "USER",
                "APPROVED",
                LocalDateTime.now()
            );
            
            when(profileService.updateProfile(eq("user@example.com"), any(UpdateProfileRequest.class)))
                .thenReturn(partialUpdateResponse);
            
            // when & then
            mockMvc.perform(patch("/api/mypage/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("새이름"))
                .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"));
        }
        
        @Test
        @DisplayName("인증 없이 프로필 수정 실패")
        void updateProfile_Fail_Unauthenticated() throws Exception {
            // when & then
            mockMvc.perform(patch("/api/mypage/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
            
            verify(profileService, never()).updateProfile(anyString(), any(UpdateProfileRequest.class));
        }
        
        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("Bean Validation 실패 - userName 길이 초과")
        void updateProfile_Fail_UserNameTooLong() throws Exception {
            // given
            UpdateProfileRequest invalidRequest = new UpdateProfileRequest(
                "가나다라마바사아자차카타파하가나다라마바사아자차카타파하가나다라마바사아자차카타파하",
                null,
                null,
                null,
                null
            );
            
            // when & then
            mockMvc.perform(patch("/api/mypage/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.userName").value("사용자명은 2~50자여야 합니다"));
            
            verify(profileService, never()).updateProfile(anyString(), any(UpdateProfileRequest.class));
        }
        
        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("Bean Validation 실패 - phoneNumber 형식 오류")
        void updateProfile_Fail_PhoneNumberInvalid() throws Exception {
            // given
            UpdateProfileRequest invalidRequest = new UpdateProfileRequest(
                null,
                "12345678",  // 잘못된 전화번호 형식
                null,
                null,
                null
            );
            
            // when & then
            mockMvc.perform(patch("/api/mypage/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.phoneNumber").exists());
            
            verify(profileService, never()).updateProfile(anyString(), any(UpdateProfileRequest.class));
        }
        
        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("프로필 수정 실패 - 빈 요청 (모든 필드 null)")
        void updateProfile_Fail_AllFieldsNull() throws Exception {
            // given
            UpdateProfileRequest emptyRequest = new UpdateProfileRequest(
                null,
                null,
                null,
                null,
                null
            );
            
            when(profileService.updateProfile(eq("user@example.com"), any(UpdateProfileRequest.class)))
                .thenThrow(new InvalidInputException("변경할 항목이 없습니다"));
            
            // when & then
            mockMvc.perform(patch("/api/mypage/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("변경할 항목이 없습니다"));
        }
        
        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("프로필 수정 실패 - 전화번호 중복")
        void updateProfile_Fail_PhoneNumberDuplicate() throws Exception {
            // given
            when(profileService.updateProfile(eq("user@example.com"), any(UpdateProfileRequest.class)))
                .thenThrow(new InvalidInputException("이미 사용 중인 전화번호입니다"));
            
            // when & then
            mockMvc.perform(patch("/api/mypage/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("이미 사용 중인 전화번호입니다"));
        }
        
        @Test
        @WithMockUser(username = "academy@example.com", roles = "ACADEMY")
        @DisplayName("프로필 수정 성공 - ACADEMY 계정")
        void updateProfile_Success_AcademyAccount() throws Exception {
            // given
            AccountResponse academyResponse = new AccountResponse(
                2L,
                "academy@example.com",
                "김선생 (수정)",
                "010-9999-8888",
                "서울시 서초구",
                "소프트웨어 캠퍼스",
                "강사",
                "ACADEMY",
                "APPROVED",
                LocalDateTime.now()
            );
            
            when(profileService.updateProfile(eq("academy@example.com"), any(UpdateProfileRequest.class)))
                .thenReturn(academyResponse);
            
            // when & then
            mockMvc.perform(patch("/api/mypage/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("academy@example.com"))
                .andExpect(jsonPath("$.accountType").value("ACADEMY"))
                .andExpect(jsonPath("$.accountApproved").value("APPROVED"));
        }
    }
}
```

---

## 📊 테스트 커버리지

| 테스트 케이스 | HTTP 메서드 | 상태 코드 | 검증 내용 |
|------------|-----------|---------|---------|
| getProfile_Success | GET | 200 | 프로필 조회 성공 |
| getProfile_Fail_Unauthenticated | GET | 401 | 인증 없이 조회 실패 |
| getProfile_Fail_UserNotFound | GET | 401 | 사용자 없음 |
| updateProfile_Success | PATCH | 200 | 프로필 수정 성공 |
| updateProfile_Success_PartialUpdate | PATCH | 200 | 부분 업데이트 성공 |
| updateProfile_Fail_Unauthenticated | PATCH | 401 | 인증 없이 수정 실패 |
| updateProfile_Fail_UserNameTooLong | PATCH | 400 | userName 길이 초과 |
| updateProfile_Fail_PhoneNumberInvalid | PATCH | 400 | phoneNumber 형식 오류 |
| updateProfile_Fail_AllFieldsNull | PATCH | 400 | 빈 요청 (모든 필드 null) |
| updateProfile_Fail_PhoneNumberDuplicate | PATCH | 400 | 전화번호 중복 |
| updateProfile_Success_AcademyAccount | PATCH | 200 | ACADEMY 계정 수정 성공 |

**총 11개 테스트**

---

## 🔍 핵심 검증 포인트

### 1. @WithMockUser 인증 모킹
```java
@WithMockUser(username = "user@example.com", roles = "USER")
```
- **Spring Security**: SecurityContext에 인증 정보 주입
- **username**: UserDetails.getUsername() 반환값 (이메일)
- **roles**: 권한 (USER, ACADEMY)

### 2. 인증 필수 검증
```java
@Test
@DisplayName("인증 없이 프로필 조회 실패")
void getProfile_Fail_Unauthenticated() throws Exception {
    mockMvc.perform(get("/api/mypage/profile"))
        .andExpect(status().isUnauthorized());
}
```
- **@WithMockUser 없음**: 인증 정보 없음
- **401 Unauthorized**: Spring Security가 자동 차단

### 3. 부분 업데이트 검증
```java
UpdateProfileRequest partialRequest = new UpdateProfileRequest(
    "새이름",  // userName만 변경
    null,      // phoneNumber 변경 안 함
    null,      // address 변경 안 함
    null,      // affiliation 변경 안 함
    null       // position 변경 안 함
);
```
- **null 필드**: Service Layer에서 무시
- **PATCH 의미**: 일부 필드만 수정

### 4. Bean Validation 검증
```java
.andExpect(status().isBadRequest())
.andExpect(jsonPath("$.errors.userName").value("사용자명은 2~50자여야 합니다"))
```
- **400 Bad Request**: Bean Validation 실패
- **errors 필드**: GlobalExceptionHandler가 추가

---

## 🧪 TestSecurityConfig (필요시 추가)

**경로:** `test/java/com/softwarecampus/backend/config/TestSecurityConfig.java`

```java
package com.softwarecampus.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 테스트용 Security 설정
 * JWT 필터 비활성화
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {
    
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/mypage/**").authenticated()
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
}
```

---

## 🔗 다음 단계

Controller 슬라이스 테스트 완료 후:
1. **FullE2ETest** 전체 통합 테스트 작성 ([05_full_e2e_test.md](05_full_e2e_test.md))
