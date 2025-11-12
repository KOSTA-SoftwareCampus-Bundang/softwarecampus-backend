# 7. MessageResponse 수정 (RESTful)

**기존 파일 수정:** `dto/user/MessageResponse.java`

---

## CodeRabbit 리뷰 반영: Status 필드 제거

### 수정 후 코드

```java
package com.softwarecampus.backend.dto.user;

/**
 * 간단한 메시지 응답
 * RESTful 표준 준수: HTTP 상태 코드로 성공/실패 판단
 */
public record MessageResponse(String message) {
    
    /**
     * 메시지 응답 생성
     * 
     * @param message 응답 메시지
     * @return MessageResponse 인스턴스
     */
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
```

---

## 변경 내역

### Before (변경 전)
```java
record MessageResponse(Status status, String message) {
    enum Status { SUCCESS, ERROR }
    
    static MessageResponse success(String message) {
        return new MessageResponse(Status.SUCCESS, message);
    }
    
    static MessageResponse error(String message) {
        return new MessageResponse(Status.ERROR, message);
    }
}
```

### After (변경 후)
```java
record MessageResponse(String message) {
    static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
```

---

## 변경 이유

### 1. RESTful 표준 준수
- **HTTP 상태 코드**가 성공/실패를 표현
- Body의 Status 필드는 **불필요한 중복**
- 클라이언트는 `response.status`로 자동 확인

### 2. Spring ProblemDetail 패턴과 일관성
- 오류 응답: RFC 9457 ProblemDetail (`application/problem+json`)
- 성공 응답: 간단한 메시지 또는 데이터
- Status 필드 없이도 충분히 명확

### 3. 단순화
- 팩토리 메서드 2개 → 1개로 축소
- Enum 제거로 코드 간소화
- 사용법 명확: `MessageResponse.of("메시지")`

---

## 사용 예시

### Controller에서 사용
```java
@PostMapping("/signup")
public ResponseEntity<MessageResponse> signup(@RequestBody SignupRequest request) {
    // ...
    return ResponseEntity.ok(MessageResponse.of("회원가입이 완료되었습니다."));
}
```

### 클라이언트 응답
```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "회원가입이 완료되었습니다."
}
```

클라이언트는 `200 OK` 상태 코드로 성공 여부 판단.
