# 2. Service Layer 확장 (마이페이지)

**목표:** ProfileService에 updateProfile() 메서드 추가

---

## 📂 수정 파일

```
src/main/java/com/softwarecampus/backend/
└─ service/user/profile/
   ├─ ProfileService.java       (인터페이스 확장)
   └─ ProfileServiceImpl.java   (구현체 확장)
```

---

## 2.1 ProfileService.java (인터페이스 확장)

**경로:** `service/user/profile/ProfileService.java`

**설명:** updateProfile() 메서드 추가

```java
package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;

/**
 * 프로필 Service 인터페이스
 * 
 * @author 태윤
 */
public interface ProfileService {
    
    /**
     * 프로필 조회
     * 
     * @param email 사용자 이메일
     * @return 프로필 정보
     * @throws UsernameNotFoundException 사용자 없음
     */
    AccountResponse getProfile(String email);
    
    /**
     * 프로필 수정
     * 
     * @param email 사용자 이메일
     * @param request 수정 요청 (userName, phoneNumber, address, affiliation, position)
     * @return 수정된 프로필 정보
     * @throws UsernameNotFoundException 사용자 없음
     * @throws InvalidInputException 빈 요청 (모든 필드 null)
     * @throws InvalidInputException 전화번호 중복
     */
    AccountResponse updateProfile(String email, UpdateProfileRequest request);
}
```

---

## 2.2 ProfileServiceImpl.java (구현체 확장)

**경로:** `service/user/profile/ProfileServiceImpl.java`

**설명:** updateProfile() 메서드 구현

```java
package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.exception.user.InvalidInputException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 Service 구현체
 * 
 * @author 태윤
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {
    
    private final AccountRepository accountRepository;
    
    /**
     * 프로필 조회
     */
    @Override
    public AccountResponse getProfile(String email) {
        log.info("프로필 조회 요청: email={}", EmailUtils.maskEmail(email));
        
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        return AccountResponse.from(account);
    }
    
    /**
     * 프로필 수정
     * 
     * 처리 순서:
     * 1. 빈 요청 검증 (모든 필드 null 체크)
     * 2. Account 조회
     * 3. 전화번호 중복 검증 (변경 시)
     * 4. 엔티티 업데이트 (null이 아닌 필드만)
     * 5. 저장 및 반환
     */
    @Override
    @Transactional
    public AccountResponse updateProfile(String email, UpdateProfileRequest request) {
        log.info("프로필 수정 요청: email={}", EmailUtils.maskEmail(email));
        
        // 1. 빈 요청 검증
        if (request.isAllFieldsNull()) {
            log.warn("프로필 수정 실패 - 변경할 항목 없음: email={}", EmailUtils.maskEmail(email));
            throw new InvalidInputException("변경할 항목이 없습니다");
        }
        
        // 2. Account 조회
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("프로필 수정 실패 - 사용자 없음: email={}", EmailUtils.maskEmail(email));
                return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email);
            });
        
        // 3. 전화번호 중복 검증 (변경 시)
        if (request.phoneNumber() != null && 
            !request.phoneNumber().equals(account.getPhoneNumber())) {
            validatePhoneNumberUnique(request.phoneNumber(), email);
        }
        
        // 4. 엔티티 업데이트 (null이 아닌 필드만)
        updateAccountFields(account, request);
        
        // 5. 저장 및 반환
        Account updated = accountRepository.save(account);
        
        log.info("프로필 수정 완료: email={}, accountId={}", 
            EmailUtils.maskEmail(email), 
            updated.getId());
        
        return AccountResponse.from(updated);
    }
    
    /**
     * 전화번호 중복 검증
     * 
     * @param phoneNumber 변경할 전화번호
     * @param currentEmail 현재 사용자 이메일 (본인 제외)
     */
    private void validatePhoneNumberUnique(String phoneNumber, String currentEmail) {
        boolean exists = accountRepository.existsByPhoneNumber(phoneNumber);
        
        if (exists) {
            // 본인 전화번호인지 확인
            Account existingAccount = accountRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow();
            
            if (!existingAccount.getEmail().equals(currentEmail)) {
                log.warn("프로필 수정 실패 - 전화번호 중복: phoneNumber=***");
                throw new InvalidInputException("이미 사용 중인 전화번호입니다");
            }
        }
    }
    
    /**
     * Account 엔티티 필드 업데이트
     * null이 아닌 필드만 업데이트
     * 
     * @param account 업데이트할 Account 엔티티
     * @param request 수정 요청 DTO
     */
    private void updateAccountFields(Account account, UpdateProfileRequest request) {
        if (request.userName() != null) {
            account.updateUserName(request.userName());
            log.debug("userName 변경: {}", request.userName());
        }
        
        if (request.phoneNumber() != null) {
            account.updatePhoneNumber(request.phoneNumber());
            log.debug("phoneNumber 변경: ***");
        }
        
        if (request.address() != null) {
            account.updateAddress(request.address());
            log.debug("address 변경: {}", request.address());
        }
        
        if (request.affiliation() != null) {
            account.updateAffiliation(request.affiliation());
            log.debug("affiliation 변경: {}", request.affiliation());
        }
        
        if (request.position() != null) {
            account.updatePosition(request.position());
            log.debug("position 변경: {}", request.position());
        }
    }
}
```

**핵심 로직:**

### 1. 빈 요청 검증
```java
if (request.isAllFieldsNull()) {
    throw new InvalidInputException("변경할 항목이 없습니다");
}
```
- **목적**: 아무것도 변경하지 않는 요청 차단
- **예시**: 모든 필드가 null인 경우

### 2. 전화번호 중복 검증
```java
if (request.phoneNumber() != null && 
    !request.phoneNumber().equals(account.getPhoneNumber())) {
    validatePhoneNumberUnique(request.phoneNumber(), email);
}
```
- **조건**:
  1. 전화번호 변경 요청이 있고 (`!= null`)
  2. 기존 전화번호와 다른 경우
- **예외**: 본인 전화번호는 중복 허용

### 3. 부분 업데이트
```java
if (request.userName() != null) {
    account.updateUserName(request.userName());
}
```
- **null 체크**: null이 아닌 필드만 업데이트
- **불변 필드 보호**: email, accountType 등은 수정 불가

### 4. 엔티티 업데이트 메서드 사용
```java
account.updateUserName(request.userName());
account.updatePhoneNumber(request.phoneNumber());
```
- **JPA Dirty Checking**: setter 대신 엔티티 메서드 사용
- **도메인 로직 캡슐화**: Account 엔티티 내부에서 검증 가능

---

## 📊 의존성

```text
ProfileServiceImpl
    ↓
├─ AccountRepository.findByEmail(String)
├─ AccountRepository.existsByPhoneNumber(String)
├─ AccountRepository.findByPhoneNumber(String)
├─ AccountRepository.save(Account)
└─ Account.updateXxx() (엔티티 메서드)
```

---

## 🔍 Account 엔티티 업데이트 메서드 (필요시 추가)

**경로:** `domain/user/Account.java`

```java
/**
 * 사용자명 변경
 */
public void updateUserName(String userName) {
    this.userName = userName;
}

/**
 * 전화번호 변경
 */
public void updatePhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
}

/**
 * 주소 변경
 */
public void updateAddress(String address) {
    this.address = address;
}

/**
 * 소속 변경
 */
public void updateAffiliation(String affiliation) {
    this.affiliation = affiliation;
}

/**
 * 직책 변경
 */
public void updatePosition(String position) {
    this.position = position;
}
```

---

## 🔐 보안 고려사항

1. **전화번호 중복 검증**
   ```java
   // 본인 전화번호는 중복 허용
   if (!existingAccount.getEmail().equals(currentEmail)) {
       throw new InvalidInputException("이미 사용 중인 전화번호입니다");
   }
   ```

2. **PII 로깅**
   ```java
   log.info("프로필 수정 요청: email={}", EmailUtils.maskEmail(email));
   log.debug("phoneNumber 변경: ***");  // 전화번호는 마스킹
   ```

3. **불변 필드 보호**
   - email, accountType, accountApproved는 UpdateProfileRequest에 포함되지 않음
   - Service Layer에서 수정 불가

---

## 🔗 다음 단계

Service 확장 후:
1. **MyPageController 구현** - GET, PATCH 엔드포인트 추가 ([03_controller_layer.md](03_controller_layer.md))
2. **MyPageControllerTest** 작성 ([04_controller_test.md](04_controller_test.md))
