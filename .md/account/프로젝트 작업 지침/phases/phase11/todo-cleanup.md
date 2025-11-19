# TODO 주석 정리

## 📋 스캔 결과

**실행일:** 2025-11-19
**스캔 범위:** `src/main/java/**/*.java`
**발견 개수:** 2개

---

## ✅ 정리 대상

### 1. `AuthController.java` (Line 84)
**현재:**
```java
/*
 * TODO Phase 8: Rate Limiter 구현
 */
```

**변경 후:**
```java
/*
 * 선택사항: Rate Limiter 추후 구현
 * - 이메일 중복 체크 API에 Rate Limiting 적용 권장
 * - 구현 시 Bucket4j 또는 Spring Cloud Gateway 사용 고려
 */
```

**이유:**
- "TODO Phase 8"은 불명확한 표현
- Phase 11 가이드에서 Rate Limiting은 선택사항으로 제외
- 미래 작업자를 위한 명확한 가이드 제공

**작업 상태:** Phase 11에서 처리

---

## ⏭️ 보류 대상

### 2. `CourseFavoriteController.java` (Line 27)
**내용:**
```java
// TODO: Security 이후 아래 코드로 교체
```

**보류 이유:**
- Course 도메인 관련 (Account 범위 아님)
- Security 설정 완료 후 별도 작업 예정

**작업 상태:** Phase 11-13 범위 밖, 추후 처리

---

## 📝 정리 원칙

### 1. TODO 주석 작성 규칙
```java
// ❌ 나쁜 예
// TODO: 나중에 수정

// ✅ 좋은 예
// TODO: [Phase X] 기능명 구현
// - 구체적인 작업 내용
// - 참고 이슈/문서 링크
```

### 2. 완료된 TODO 처리
- 작업 완료 시 TODO 주석 제거
- 필요시 일반 주석으로 변경 (구현 의도 설명)

### 3. 장기 보류 TODO
- "선택사항", "추후 고려" 등 명확한 표현 사용
- 우선순위와 이유 명시

---

## ✅ 작업 완료 조건

- [x] TODO 스캔 완료
- [ ] `AuthController.java` 주석 수정
- [ ] 변경 사항 커밋
- [ ] `CourseFavoriteController.java`는 현재 브랜치에서 제외

---

## 🔗 관련 문서

- [Phase 11 Overview](./overview.md)
- [AGENTS.md - 코드 주석 규칙](../../AGENTS.md)
