# Phase 15-1: Service Layer

**파일:** `ProfileService.java`, `ProfileServiceImpl.java`  
**목적:** 프로필 수정 및 계정 삭제 로직 구현

---

## 📋 새로 추가할 메서드

### 1. updateProfile()
```java
AccountResponse updateProfile(String email, UpdateProfileRequest request);
```
- 현재 로그인 사용자의 프로필 수정
- 전화번호 중복 검증 (변경 시에만)
- Account 엔티티 업데이트

### 2. deleteAccount()
```java
void deleteAccount(String email);
```
- 소프트 삭제 (`BaseSoftDeleteSupportEntity.markDeleted()`)
- `deletedAt` 타임스탬프 설정
- 실제 데이터는 삭제하지 않음

---

## 📄 ProfileService 인터페이스 수정

```java
package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;

public interface ProfileService {
    
    // 기존 메서드
    AccountResponse getProfile(String email);

    // ✅ 신규 메서드
    AccountResponse updateProfile(String email, UpdateProfileRequest request);
    
    void deleteAccount(String email);
}
```

---

## 📄 ProfileServiceImpl 구현

```java
package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.PhoneNumberAlreadyExistsException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getProfile(String email) {
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException(email));
        
        return AccountResponse.from(account);
    }

    @Override
    @Transactional
    public AccountResponse updateProfile(String email, UpdateProfileRequest request) {
        // 1. Account 조회
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException(email));

        // 2. 전화번호 중복 검증 (변경하는 경우에만)
        if (request.getPhoneNumber() != null && 
            !request.getPhoneNumber().equals(account.getPhoneNumber())) {
            
            if (accountRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new PhoneNumberAlreadyExistsException(request.getPhoneNumber());
            }
        }

        // 3. Account 업데이트
        updateAccountFields(account, request);

        // 4. 저장 및 응답 (JPA dirty checking으로 자동 저장)
        log.info("프로필 수정 완료 - email: {}", email);
        return AccountResponse.from(account);
    }

    @Override
    @Transactional
    public void deleteAccount(String email) {
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException(email));

        // 소프트 삭제
        account.markDeleted();
        
        log.info("계정 삭제 (소프트) - email: {}", email);
    }

    // Private helper method
    private void updateAccountFields(Account account, UpdateProfileRequest request) {
        if (request.getUserName() != null) {
            account.setUserName(request.getUserName());
        }
        if (request.getPhoneNumber() != null) {
            account.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAddress() != null) {
            account.setAddress(request.getAddress());
        }
        if (request.getAffiliation() != null) {
            account.setAffiliation(request.getAffiliation());
        }
        if (request.getPosition() != null) {
            account.setPosition(request.getPosition());
        }
    }
}
```

---

## 🔐 보안 고려사항

### 1. 본인 계정만 수정
- Controller에서 `@AuthenticationPrincipal`로 인증된 이메일 추출
- Service는 받은 이메일로만 조회/수정

### 2. 전화번호 중복 검증
```java
// 기존 전화번호와 다를 때만 중복 검사
if (request.getPhoneNumber() != null && 
    !request.getPhoneNumber().equals(account.getPhoneNumber())) {
    // 중복 검사
}
```

### 3. 소프트 삭제
```java
// deletedAt만 설정, 데이터는 보존
account.markDeleted();
// DELETE FROM account WHERE ... 실행 안 함!
```

---

## 🧪 테스트 시나리오

### updateProfile()
1. ✅ 정상 수정 (모든 필드)
2. ✅ 부분 수정 (일부 필드만)
3. ✅ 전화번호만 수정
4. ❌ 존재하지 않는 이메일 → AccountNotFoundException
5. ❌ 중복 전화번호 → PhoneNumberAlreadyExistsException

### deleteAccount()
1. ✅ 정상 삭제 (deletedAt 설정)
2. ❌ 존재하지 않는 이메일 → AccountNotFoundException

---

## 📌 체크리스트

- [ ] ProfileService 인터페이스 확장
- [ ] ProfileServiceImpl 메서드 구현
- [ ] 전화번호 중복 검증 로직
- [ ] 소프트 삭제 구현
- [ ] 로깅 추가
