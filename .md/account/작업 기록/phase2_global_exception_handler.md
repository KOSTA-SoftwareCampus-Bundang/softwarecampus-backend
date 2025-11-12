# Phase 2: GlobalExceptionHandler 기본 틀 작성

**작업 기간:** 2025-10-29  
**담당자:** 태윤  
**상태:** ✅ 완료

---

## 📌 작업 목표
- RFC 9457 Problem Details 형식 예외 응답 구조 확립
- Bean Validation 예외 처리 (@Valid)
- 도메인 예외는 주석으로 표시 (Phase 5에서 구현 예정)

## 📂 생성/수정 파일
- ✅ `exception/GlobalExceptionHandler.java`

---

## 🔨 작업 내용

### 1. GlobalExceptionHandler 생성

**최종 상태:** ✅ 정상 작동

#### 작성한 코드
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // Bean Validation 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex)
    
    // 일반 예외 처리 (fallback)
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex)
    
    // Phase 5에서 추가할 도메인 예외들 (주석 처리)
    // @ExceptionHandler(DuplicateEmailException.class)
    // @ExceptionHandler(AccountNotFoundException.class)
}
```

#### 컴파일 확인
```bash
mvn clean compile
```

**결과:** ✅ BUILD SUCCESS

---

## ✅ 최종 체크리스트
- [x] GlobalExceptionHandler.java 생성
- [x] RFC 9457 Problem Details 구조 적용
- [x] Bean Validation 예외 처리 구현
- [x] 도메인 예외 핸들러 주석으로 준비
- [x] 컴파일 성공

## 📝 주요 결정 사항
- **RFC 9457 형식 채택**: `ProblemDetail` 사용
- **도메인 예외 지연 구현**: Phase 5에서 실제 예외 발생 시점에 추가
- **주석 처리**: 미리 구조만 준비하고 나중에 활성화

## 🔜 다음 단계
- Phase 3: 기본 보안 설정 (PasswordEncoder) - 기존 파일 확인
