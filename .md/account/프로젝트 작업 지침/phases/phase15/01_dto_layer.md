# 1. DTO Layer (마이페이지)

**목표:** 프로필 수정 요청 DTO 작성 및 Bean Validation 적용

---

## 📂 생성 파일

```
src/main/java/com/softwarecampus/backend/
└─ dto/user/
   └─ UpdateProfileRequest.java
```

---

## 1.1 UpdateProfileRequest.java

**경로:** `dto/user/UpdateProfileRequest.java`

**설명:** 프로필 수정 요청 데이터를 담는 DTO

```java
package com.softwarecampus.backend.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 DTO
 * 
 * 수정 가능 필드:
 * - userName (사용자명)
 * - phoneNumber (전화번호)
 * - address (주소)
 * - affiliation (소속)
 * - position (직책)
 * 
 * 수정 불가 필드:
 * - email (계정 식별자, 불변)
 * - password (별도 비밀번호 변경 API 사용)
 * - accountType (계정 타입, 불변)
 * - accountApproved (승인 상태, 관리자만 변경)
 * 
 * @author 태윤
 */
public record UpdateProfileRequest(
    
    @Size(min = 2, max = 50, message = "사용자명은 2~50자여야 합니다")
    String userName,
    
    @Pattern(
        regexp = "^01[0-9]-[0-9]{3,4}-[0-9]{4}$|^01[0-9][0-9]{7,8}$",
        message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)"
    )
    String phoneNumber,
    
    String address,
    
    String affiliation,
    
    String position
) {
    /**
     * 모든 필드가 null인지 확인
     * 
     * @return 모든 필드가 null이면 true
     */
    public boolean isAllFieldsNull() {
        return userName == null 
            && phoneNumber == null 
            && address == null 
            && affiliation == null 
            && position == null;
    }
}
```

**핵심 포인트:**

### 1. 선택적 필드 (모든 필드 Optional)
- **null 허용**: 변경하지 않을 필드는 null로 전송
- **부분 업데이트**: userName만 변경하고 싶으면 나머지는 null
- **예시**:
  ```json
  {
    "userName": "새이름",
    "phoneNumber": null,
    "address": null,
    "affiliation": null,
    "position": null
  }
  ```

### 2. Bean Validation 제약
- **userName**:
  - `@Size(min = 2, max = 50)`: 2~50자
  - null 허용 (변경하지 않을 때)
  
- **phoneNumber**:
  - `@Pattern`: 휴대폰 형식 (010-1234-5678 또는 01012345678)
  - null 허용 (변경하지 않을 때)
  
- **address, affiliation, position**:
  - 제약 없음 (자유 입력)
  - null 허용

### 3. 수정 불가 필드
```java
// ❌ UpdateProfileRequest에 포함되지 않음
- email: 계정 식별자, 변경 불가
- password: 별도 비밀번호 변경 API 필요
- accountType: USER/ACADEMY 변경 불가
- accountApproved: 관리자만 변경 가능
- academyId: ACADEMY 타입 불변 데이터
```

### 4. 유틸리티 메서드
```java
public boolean isAllFieldsNull() {
    return userName == null 
        && phoneNumber == null 
        && address == null 
        && affiliation == null 
        && position == null;
}
```
- **목적**: "아무것도 변경하지 않음" 검증
- **사용**: Service Layer에서 빈 요청 차단

---

## 📊 요청 예시

### 1. 사용자명만 변경

```json
{
  "userName": "홍길동 (수정)",
  "phoneNumber": null,
  "address": null,
  "affiliation": null,
  "position": null
}
```

### 2. 전화번호 + 주소 변경

```json
{
  "userName": null,
  "phoneNumber": "010-9999-8888",
  "address": "서울시 종로구 새주소",
  "affiliation": null,
  "position": null
}
```

### 3. 전체 필드 변경

```json
{
  "userName": "김철수",
  "phoneNumber": "010-5555-6666",
  "address": "부산시 해운대구",
  "affiliation": "소프트웨어 캠퍼스",
  "position": "수강생"
}
```

### 4. 빈 요청 (아무것도 변경 안 함)

```json
{
  "userName": null,
  "phoneNumber": null,
  "address": null,
  "affiliation": null,
  "position": null
}
```
→ Service Layer에서 `isAllFieldsNull()` 검증 후 예외 발생

---

## 🔍 Bean Validation 검증

### 성공 케이스
```json
{
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678"
}
```
→ 200 OK

### 실패 케이스 1: userName 길이 초과
```json
{
  "userName": "가나다라마바사아자차카타파하가나다라마바사아자차카타파하가나다라마바사아"
}
```
→ 400 Bad Request
```json
{
  "status": 400,
  "title": "Bad Request",
  "errors": {
    "userName": "사용자명은 2~50자여야 합니다"
  }
}
```

### 실패 케이스 2: phoneNumber 형식 오류
```json
{
  "phoneNumber": "12345678"
}
```
→ 400 Bad Request
```json
{
  "status": 400,
  "errors": {
    "phoneNumber": "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)"
  }
}
```

---

## 🔄 Service Layer 처리 로직

```java
@Override
@Transactional
public AccountResponse updateProfile(String email, UpdateProfileRequest request) {
    // 1. 빈 요청 검증
    if (request.isAllFieldsNull()) {
        throw new InvalidInputException("변경할 항목이 없습니다");
    }
    
    // 2. Account 조회
    Account account = accountRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다"));
    
    // 3. 전화번호 중복 검증 (변경 시)
    if (request.phoneNumber() != null && 
        !request.phoneNumber().equals(account.getPhoneNumber())) {
        validatePhoneNumberUnique(request.phoneNumber(), email);
    }
    
    // 4. 엔티티 업데이트 (null이 아닌 필드만)
    if (request.userName() != null) {
        account.updateUserName(request.userName());
    }
    if (request.phoneNumber() != null) {
        account.updatePhoneNumber(request.phoneNumber());
    }
    if (request.address() != null) {
        account.updateAddress(request.address());
    }
    if (request.affiliation() != null) {
        account.updateAffiliation(request.affiliation());
    }
    if (request.position() != null) {
        account.updatePosition(request.position());
    }
    
    // 5. 저장 및 반환
    Account updated = accountRepository.save(account);
    return AccountResponse.from(updated);
}
```

---

## 🔗 다음 단계

DTO 생성 후:
1. **ProfileService 확장** - updateProfile() 메서드 추가 ([02_service_layer.md](02_service_layer.md))
2. **MyPageController 구현** - PATCH 엔드포인트 추가 ([03_controller_layer.md](03_controller_layer.md))
