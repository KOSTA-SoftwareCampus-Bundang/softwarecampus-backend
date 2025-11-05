# Phase 4: DTO Layer (Request/Response)

**목표:** 회원가입에 필요한 DTO 작성 및 Bean Validation 적용  
**담당자:** 태윤  
**상태:** ✅ 완료 (2025-11-05)

---

## 📋 작업 개요

회원가입 API에서 사용할 Request/Response DTO를 작성합니다. Java Record를 사용하여 불변 객체로 구현하고, Bean Validation 어노테이션을 적용하여 입력값 검증을 자동화합니다.

---

## 📂 생성 파일

```
src/main/java/com/softwarecampus/backend/
└─ dto/
   └─ user/
      ├─ request/
      │  └─ SignupRequest.java
      └─ response/
         ├─ AccountResponse.java
         └─ MessageResponse.java
```

---

## 🔨 구현 내용

### 1. SignupRequest.java

**경로:** `dto/user/request/SignupRequest.java`

**설명:** 회원가입 요청 데이터를 담는 DTO

```java
package com.softwarecampus.backend.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO
 * 
 * @param email 이메일 (필수, 이메일 형식)
 * @param password 비밀번호 (필수, 8~20자, 영문+숫자+특수문자)
 * @param userName 사용자명 (필수, 2~50자)
 * @param phoneNumber 전화번호 (필수, 휴대폰 형식)
 * @param address 주소 (선택)
 * @param affiliation 소속 (선택)
 * @param position 직책 (선택)
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
    
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 2, max = 50, message = "사용자명은 2~50자여야 합니다")
    String userName,
    
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(
        regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
        message = "올바른 휴대폰 번호 형식이 아닙니다 (예: 010-1234-5678)"
    )
    String phoneNumber,
    
    String address,
    String affiliation,
    String position
) {
}
```

**검증 규칙:**
- `email`: 필수, 이메일 형식
- `password`: 필수, 8~20자, 영문+숫자+특수문자 포함
- `userName`: 필수, 2~50자 (한글/영문 이름 모두 수용)
- `phoneNumber`: 필수, 휴대폰 형식 (010-1234-5678 또는 01012345678)
- `address`, `affiliation`, `position`: 선택 (null 허용)

---

### 2. AccountResponse.java

**경로:** `dto/user/response/AccountResponse.java`

**설명:** 계정 정보 응답 DTO

```java
package com.softwarecampus.backend.dto.user.response;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;

/**
 * 계정 정보 응답 DTO
 * 
 * @param id 계정 ID
 * @param email 이메일
 * @param userName 사용자명
 * @param phoneNumber 전화번호
 * @param accountType 계정 타입 (USER, INSTRUCTOR, ACADEMY, ADMIN)
 * @param approvalStatus 승인 상태 (PENDING, APPROVED, REJECTED)
 * @param address 주소
 * @param affiliation 소속
 * @param position 직책
 */
public record AccountResponse(
    Long id,
    String email,
    String userName,
    String phoneNumber,
    AccountType accountType,
    ApprovalStatus approvalStatus,
    String address,
    String affiliation,
    String position
) {
}
```

**특징:**
- 비밀번호는 응답에 포함되지 않음 (보안)
- Enum 타입 (AccountType, ApprovalStatus) 그대로 반환
- 모든 필드 public 접근 가능 (Record 특성)

---

### 3. MessageResponse.java

**경로:** `dto/user/response/MessageResponse.java`

**설명:** 간단한 메시지 응답 DTO

```java
package com.softwarecampus.backend.dto.user.response;

/**
 * 간단한 메시지 응답 DTO
 * 
 * @param message 응답 메시지
 */
public record MessageResponse(
    String message
) {
    /**
     * 정적 팩토리 메서드 - 성공 메시지
     */
    public static MessageResponse success(String message) {
        return new MessageResponse(message);
    }
    
    /**
     * 정적 팩토리 메서드 - 에러 메시지
     */
    public static MessageResponse error(String message) {
        return new MessageResponse(message);
    }
}
```

**사용 예시:**
```java
// 컨트롤러에서
return ResponseEntity.ok(MessageResponse.success("작업이 완료되었습니다"));
```

---

## ✅ 검증 방법

### 1. 컴파일 확인
```bash
./mvnw clean compile
```

### 2. Record 구조 확인
- Record는 자동으로 생성자, getter, equals, hashCode, toString 제공
- IDE에서 `SignupRequest.` 입력 시 `email()`, `password()` 등 메서드 자동완성 확인

### 3. Validation 동작 확인 (Phase 8에서 테스트)
- Controller에서 `@Valid` 사용 시 자동 검증
- 검증 실패 시 `MethodArgumentNotValidException` 발생 → GlobalExceptionHandler가 처리

---

## 📝 Phase 완료 기준

- [x] **파일 생성 완료**
  - [x] `SignupRequest.java` 생성
  - [x] `AccountResponse.java` 생성
  - [x] `MessageResponse.java` 생성

- [x] **코드 검증**
  - [x] 컴파일 성공 (`mvn clean compile` - BUILD SUCCESS)
  - [x] Record 문법 정상 동작 확인
  - [x] Bean Validation 어노테이션 올바르게 적용

- [x] **문서화**
  - [x] 작업 기록에 Phase 4 완료 기록
  - [x] implementation_plan.md 체크리스트 업데이트

- [x] **의존성 추가**
  - [x] `spring-boot-starter-validation` 추가 (pom.xml)

---

## 🔜 다음 단계

**Phase 5: Service Layer + 도메인 예외**
- AccountService 인터페이스 정의
- AccountServiceImpl 구현
- DuplicateEmailException, AccountNotFoundException 생성
- GlobalExceptionHandler에 도메인 예외 핸들러 추가

---

**작성일:** 2025-11-05  
**최종 수정:** 2025-11-05  
**상태:** ✅ 구현 완료

---

## 📊 구현 결과

### 생성된 파일 (3개)
- ✅ `src/main/java/com/softwarecampus/backend/dto/user/request/SignupRequest.java`
- ✅ `src/main/java/com/softwarecampus/backend/dto/user/response/AccountResponse.java`
- ✅ `src/main/java/com/softwarecampus/backend/dto/user/response/MessageResponse.java`

### 의존성 추가
- ✅ `pom.xml`: `spring-boot-starter-validation` 추가
  - Jakarta Bean Validation API 제공
  - Hibernate Validator 구현체 제공
  - Spring Boot 2.3 이후 별도 추가 필요

### 빌드 결과
```
[INFO] BUILD SUCCESS
[INFO] Total time:  3.365 s
[INFO] Compiling 35 source files
```

### 검증 완료 항목
- ✅ Java 17 Record 문법 정상 작동
- ✅ Bean Validation 어노테이션 컴파일 성공
- ✅ Account 엔티티 필드와 DTO 매핑 완료
- ✅ RFC 9457 Problem Details 연동 준비 완료
