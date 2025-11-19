# Phase 11: 보안 기본 설정

## 📋 개요
**목표:** CORS 설정 및 TODO 주석 정리
**예상 시간:** 1-2시간
**생성 파일:** 1개

---

## ✅ 체크리스트

### 1. CORS 설정
- [ ] `config/WebConfig.java` 생성
- [ ] CORS 매핑 설정
- [ ] 빌드 및 검증

### 2. TODO 주석 정리
- [ ] `AuthController.java` TODO 주석 명확화
- [ ] ~~Rate Limiting 제외 (선택사항)~~

---

## 📁 생성할 파일

1. **`config/WebConfig.java`**
   - CORS 설정 클래스
   - 상세: [cors-config.md](./cors-config.md)

---

## 🔗 관련 문서

- [CORS 설정 상세](./cors-config.md)
- [TODO 정리 목록](./todo-cleanup.md)

---

## 📝 완료 조건

- [x] TODO 스캔 완료 (2개 발견)
- [ ] `WebConfig.java` 생성 및 테스트
- [ ] `AuthController.java` 주석 수정
- [ ] 빌드 성공 (`./mvnw clean verify`)
- [ ] 기존 테스트 통과 (82/82)
