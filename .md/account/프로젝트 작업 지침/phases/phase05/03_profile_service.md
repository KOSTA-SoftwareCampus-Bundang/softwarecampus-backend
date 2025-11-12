# 3. ProfileService 구현

## ProfileService.java (인터페이스)

**경로:** `service/user/profile/ProfileService.java`

**설명:** 계정 조회 기능 정의

```java
package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.dto.user.AccountResponse;

/**
 * 계정 조회 Service 인터페이스
 */
public interface ProfileService {
    
    /**
     * ID로 계정 조회
     * 
     * @param accountId 계정 ID
     * @return 계정 정보
     * @throws AccountNotFoundException 계정이 존재하지 않는 경우
     */
    AccountResponse getAccountById(Long accountId);
    
    /**
     * 이메일로 계정 조회
     * 
     * @param email 이메일
     * @return 계정 정보
     * @throws AccountNotFoundException 계정이 존재하지 않는 경우
     */
    AccountResponse getAccountByEmail(String email);
}
```

---

## ProfileServiceImpl.java (구현체)

**경로:** `service/user/profile/ProfileServiceImpl.java`

**설명:** 계정 조회 기능 구현 (Phase 5는 조회만, Phase 18에서 확장)

```java
package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 조회 Service
 * - Phase 5: 기본 조회 기능
 * - Phase 18: 수정/삭제 기능 추가 예정
 * - PII 로깅 보호 (이메일 마스킹)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {
    
    private final AccountRepository accountRepository;
    
    /**
     * ID로 계정 조회
     */
    @Override
    public AccountResponse getAccountById(Long accountId) {
        log.info("계정 조회 시도: accountId={}", accountId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다: " + accountId));
        
        log.info("계정 조회 완료: accountId={}, email={}, accountType={}", 
            account.getId(),
            EmailUtils.maskEmail(account.getEmail()),
            account.getAccountType());
        
        return toAccountResponse(account);
    }
    
    /**
     * 이메일로 계정 조회
     */
    @Override
    public AccountResponse getAccountByEmail(String email) {
        // 1. 입력 검증
        validateEmailInput(email);
        
        // 2. 계정 조회 (PII 마스킹 로깅)
        log.info("계정 조회 시도: email={}", EmailUtils.maskEmail(email));
        
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException("계정을 찾을 수 없습니다."));
        
        log.info("계정 조회 완료: accountId={}, accountType={}, userName={}", 
            account.getId(),
            account.getAccountType(),
            account.getUserName());
        
        return toAccountResponse(account);
    }
    
    /**
     * 이메일 형식 검증
     * RFC 5322 (이메일 기본 형식) + RFC 1035 (도메인 레이블 규칙)
     */
    private void validateEmailFormat(String email) {
        if (!EmailUtils.isValidEmail(email)) {
            log.warn("잘못된 이메일 형식: maskedEmail={}", EmailUtils.maskEmail(email));
            throw new InvalidInputException("잘못된 이메일 형식입니다.");
        }
    }
    
    // Phase 18에서 추가 예정:
    // - updateProfile(Long id, UpdateRequest request)
    // - deleteAccount(Long id)
}
```

---

## 설계 포인트

- Phase 5는 조회 기능만 구현 (Phase 6 테스트용)
- Phase 18에서 수정/삭제 기능 추가
- PII 보호를 위한 이메일 마스킹 적용
- **로깅 개선**: 조회 성공 시 식별 정보 추가 (accountId, accountType, userName 등)
- 파일 크기 약 95줄

---

## PR 리뷰 반영 (2024-11-12)

### Issue #2: 로깅 개선 - 식별 정보 추가
**리뷰 내용:**
> "이메일 마스킹으로 인해 로그만으로는 어떤 계정인지 파악 어려움. 추가 식별 정보 필요."

**수정 내용:**
- `getAccountById()`: 조회 성공 시 `email(마스킹), accountType` 로그 추가
- `getAccountByEmail()`: 조회 성공 시 `accountId, accountType, userName` 로그 추가

**변경 이유:**
- 이메일 마스킹은 PII 보호를 위해 유지
- 디버깅 시 accountId, accountType, userName으로 계정 식별 가능
- 보안성과 디버깅 용이성 균형 확보

---

## CodeRabbit 리뷰 반영

### 이메일 검증 로직 중복 허용
**CodeRabbit 리뷰:**
> "`validateEmailFormat`를 EmailUtils로 공통화 권장."

**논의 및 결정:**
- 중복 코드 약 5줄 (경미한 중복)
- 각 Service 맥락이 다름 (회원가입 vs 프로필 변경)
- 명확성 > DRY (Don't Repeat Yourself)
- 공통화 시 불필요한 추상화 발생 가능
- **결정:** 현재 유지 (명확성 우선)
