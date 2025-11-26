# Phase 15-1: DTO Layer

**파일:** `UpdateProfileRequest.java`  
**목적:** 프로필 수정 요청 DTO (Bean Validation)

---

## 📋 UpdateProfileRequest

### 요구사항
- 이름, 전화번호, 주소, 소속, 직책 수정 가능
- Bean Validation으로 입력값 검증
- 모든 필드 선택사항 (null 허용)

---

## 📄 코드

```java
package com.softwarecampus.backend.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    private String userName;

    @Pattern(
        regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
        message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)"
    )
    private String phoneNumber;

    @Size(max = 200, message = "주소는 200자 이하여야 합니다")
    private String address;

    @Size(max = 100, message = "소속은 100자 이하여야 합니다")
    private String affiliation;

    @Size(max = 50, message = "직책은 50자 이하여야 합니다")
    private String position;
}
```

---

## ✅ 검증 규칙

| 필드 | 제약조건 | 에러 메시지 |
|------|----------|-------------|
| userName | 2-50자 | 이름은 2-50자 사이여야 합니다 |
| phoneNumber | 정규식 (010-XXXX-XXXX) | 전화번호 형식이 올바르지 않습니다 |
| address | 최대 200자 | 주소는 200자 이하여야 합니다 |
| affiliation | 최대 100자 | 소속은 100자 이하여야 합니다 |
| position | 최대 50자 | 직책은 50자 이하여야 합니다 |

**참고:** 모든 필드는 **선택사항** (null 가능)

---

## 🧪 테스트 케이스

### 유효한 입력
```json
{
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "address": "서울시 강남구",
  "affiliation": "소프트캠퍼스",
  "position": "개발자"
}
```

### 검증 실패 케이스

**1. 이름 길이 초과**
```json
{
  "userName": "가".repeat(51)
}
// → 400 Bad Request: 이름은 2-50자 사이여야 합니다
```

**2. 잘못된 전화번호**
```json
{
  "phoneNumber": "02-1234-5678"
}
// → 400 Bad Request: 전화번호 형식이 올바르지 않습니다
```

**3. 부분 수정 (일부 필드만)**
```json
{
  "userName": "김철수"
}
// → 200 OK (다른 필드는 유지)
```

---

## 📌 체크리스트

- [ ] `dto/user/UpdateProfileRequest.java` 생성
- [ ] Bean Validation 의존성 확인 (pom.xml)
- [ ] Lombok 어노테이션 적용
- [ ] 정규식 패턴 테스트
