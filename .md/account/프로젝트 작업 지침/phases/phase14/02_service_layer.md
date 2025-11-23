# 2. Service Layer (로그인)

**목표:** 로그인 비즈니스 로직 구현

---

## 📂 생성 파일

```
src/main/java/com/softwarecampus/backend/
└─ service/user/login/
   ├─ LoginService.java       (인터페이스)
   └─ LoginServiceImpl.java   (구현체)
```

---

## 2.1 LoginService.java (인터페이스)

**경로:** `service/user/login/LoginService.java`

**설명:** 로그인 기능 정의

```java
package com.softwarecampus.backend.service.user.login;

import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.LoginResponse;

/**
 * 로그인 Service 인터페이스
 * 
 * @author 태윤
 */
public interface LoginService {
    
    /**
     * 로그인 처리
     * 
     * @param request 로그인 요청 (email, password)
     * @return 로그인 응답 (accessToken, refreshToken, account)
     * @throws InvalidCredentialsException 이메일 없음 또는 비밀번호 불일치
     */
    LoginResponse login(LoginRequest request);
}
```

---

## 2.2 LoginServiceImpl.java (구현체)

**경로:** `service/user/login/LoginServiceImpl.java`

**설명:** 로그인 비즈니스 로직 전담

```java
package com.softwarecampus.backend.service.user.login;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.LoginResponse;
import com.softwarecampus.backend.exception.user.InvalidCredentialsException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import com.softwarecampus.backend.service.token.TokenService;
import com.softwarecampus.backend.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 Service 구현체
 * 
 * 처리 순서:
 * 1. 이메일로 Account 조회
 * 2. 비밀번호 검증 (PasswordEncoder.matches)
 * 3. 계정 상태 검증 (활성화, 승인 여부)
 * 4. JWT Access Token 생성
 * 5. Refresh Token 생성 및 Redis 저장
 * 6. LoginResponse 반환
 * 
 * @author 태윤
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginServiceImpl implements LoginService {
    
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    
    /**
     * 로그인 처리
     * 
     * 보안 원칙:
     * - 이메일 존재 여부와 비밀번호 오류를 구분하지 않음 (정보 유출 방지)
     * - 모든 인증 실패는 동일한 예외 메시지 사용
     * - PII 로깅 시 마스킹 적용
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("로그인 시도: email={}", EmailUtils.maskEmail(request.email()));
        
        // 1. Account 조회
        Account account = accountRepository.findByEmail(request.email())
            .orElseThrow(() -> {
                log.warn("로그인 실패 - 존재하지 않는 이메일: {}", EmailUtils.maskEmail(request.email()));
                return new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다");
            });
        
        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            log.warn("로그인 실패 - 비밀번호 불일치: {}", EmailUtils.maskEmail(request.email()));
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        
        // 3. 계정 상태 검증 (비활성화 또는 미승인 계정 차단)
        if (!account.isActive()) {
            log.warn("로그인 실패 - 비활성화된 계정: {}", EmailUtils.maskEmail(request.email()));
            throw new InvalidCredentialsException("비활성화된 계정입니다");
        }
        
        // ACADEMY 계정은 관리자 승인 필요
        if (account.getAccountType().name().equals("ACADEMY") && 
            !account.getAccountApproved().name().equals("APPROVED")) {
            log.warn("로그인 실패 - 미승인 ACADEMY 계정: {}, status={}", 
                EmailUtils.maskEmail(request.email()), 
                account.getAccountApproved());
            throw new InvalidCredentialsException("승인 대기 중인 계정입니다");
        }
        
        // 4. JWT Access Token 생성
        String accessToken = jwtTokenProvider.generateToken(
            account.getEmail(), 
            account.getAccountType().name()
        );
        
        // 5. Refresh Token 생성 및 Redis 저장
        String refreshToken = jwtTokenProvider.generateRefreshToken(account.getEmail());
        tokenService.saveRefreshToken(account.getEmail(), refreshToken);
        
        // 6. LoginResponse 생성
        AccountResponse accountResponse = AccountResponse.from(account);
        Long expiresIn = jwtTokenProvider.getExpiration() / 1000;  // 밀리초 → 초 변환
        
        log.info("로그인 성공: email={}, accountType={}", 
            EmailUtils.maskEmail(request.email()), 
            account.getAccountType());
        
        return LoginResponse.of(accessToken, refreshToken, expiresIn, accountResponse);
    }
}
```

**핵심 로직:**

### 1. 이메일 조회
```java
Account account = accountRepository.findByEmail(request.email())
    .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다"));
```
- `Optional.orElseThrow()`: 이메일 없으면 예외 발생
- **보안**: "존재하지 않는 이메일"이라고 명시하지 않음 (계정 존재 여부 유출 방지)

### 2. 비밀번호 검증
```java
if (!passwordEncoder.matches(request.password(), account.getPassword())) {
    throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다");
}
```
- `PasswordEncoder.matches()`: BCrypt 해시 비교
- 평문 비밀번호 + 암호화된 비밀번호 비교
- **동일한 예외 메시지**: 이메일 오류와 구분 불가

### 3. 계정 상태 검증
```java
if (!account.isActive()) {
    throw new InvalidCredentialsException("비활성화된 계정입니다");
}

if (account.getAccountType() == ACADEMY && account.getAccountApproved() != APPROVED) {
    throw new InvalidCredentialsException("승인 대기 중인 계정입니다");
}
```
- **isActive()**: 소프트 삭제된 계정 차단
- **ACADEMY 승인**: PENDING/REJECTED 상태 로그인 방지

### 4. JWT 토큰 생성
```java
String accessToken = jwtTokenProvider.generateToken(email, role);
String refreshToken = jwtTokenProvider.generateRefreshToken(email);
```
- **Access Token**: 15분 (900초) 유효
- **Refresh Token**: 7일 (604,800초) 유효

### 5. Refresh Token 저장
```java
tokenService.saveRefreshToken(email, refreshToken);
```
- Redis 저장: `refresh:{email}` 키
- TTL: 7일 자동 만료

---

## 📊 의존성

```text
LoginServiceImpl
    ↓
├─ AccountRepository.findByEmail(String)
├─ PasswordEncoder.matches(rawPassword, encodedPassword)
├─ JwtTokenProvider.generateToken(email, role)
├─ JwtTokenProvider.generateRefreshToken(email)
├─ JwtTokenProvider.getExpiration()
└─ TokenService.saveRefreshToken(email, refreshToken)
```

---

## 🔐 보안 고려사항

1. **정보 유출 방지**
   - 이메일 존재 여부를 알려주지 않음
   - 이메일 오류/비밀번호 오류 동일한 메시지

2. **계정 상태 검증**
   - 삭제된 계정 (isActive = false) 차단
   - 미승인 ACADEMY 계정 차단

3. **PII 로깅**
   - 이메일 마스킹: `EmailUtils.maskEmail()`
   - 비밀번호 절대 로깅 금지

4. **Timing Attack 방지**
   - `PasswordEncoder.matches()`는 내부적으로 일정 시간 소요 (BCrypt)
   - 이메일 오류/비밀번호 오류 응답 시간 유사

---

## 🔗 다음 단계

Service 구현 후:
1. **AuthController** 로그인 엔드포인트 추가 ([03_controller_layer.md](03_controller_layer.md))
2. **InvalidCredentialsException** 예외 클래스 생성 ([04_exception_handling.md](04_exception_handling.md))
