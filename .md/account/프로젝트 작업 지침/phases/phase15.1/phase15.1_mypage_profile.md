# Phase 15-1: 마이페이지 프로필 관리

**목표:** 프로필 조회/수정/삭제 API 구현  
**담당자:** 태윤  
**상태:** 🚧 진행 중  
**예상 시간:** 2-3시간

---

## 📋 작업 개요

**구현 범위:**
- ✅ 프로필 조회 (GET /api/mypage/profile)
- ✅ 프로필 수정 (PATCH /api/mypage/profile)
- ✅ 계정 삭제 (DELETE /api/mypage/account) - 소프트 삭제

**미구현 (Phase 15-2):**
- ⏭️ 내가 쓴 글 목록
- ⏭️ 내가 쓴 댓글 목록
- ⏭️ 찜한 과정 목록
- ⏭️ 통계/최근 활동

---

## 📂 상세 문서

> **최적화 전략 적용:** 각 문서는 100-200줄 이내로 작성

1. **[DTO 설계](phase15.1/01_dto.md)** (100줄)
   - UpdateProfileRequest
   - Bean Validation

2. **[Service Layer](phase15.1/02_service.md)** (150줄)
   - ProfileService 확장
   - updateProfile(), deleteAccount() 구현

3. **[Controller Layer](phase15.1/03_controller.md)** (120줄)
   - MyPageController
   - GET, PATCH, DELETE 엔드포인트

4. **[테스트](phase15.1/04_test.md)** (180줄)
   - Controller 슬라이스 테스트 (10개)
   - Integration 테스트 (8개)

---

## 📊 생성 파일

```
src/main/java/
├── dto/user/
│   └── UpdateProfileRequest.java       ✅ 신규
├── service/user/profile/
│   ├── ProfileService.java             🔧 메서드 추가
│   └── ProfileServiceImpl.java         🔧 구현
└── controller/user/
    └── MyPageController.java           ✅ 신규

src/test/java/
├── controller/user/
│   └── MyPageControllerTest.java       ✅ 신규
└── integration/
    └── MyPageIntegrationTest.java      ✅ 신규
```

---

## 🎯 완료 기준

- [ ] UpdateProfileRequest DTO 생성
- [ ] ProfileService 메서드 추가
- [ ] MyPageController 구현
- [ ] Controller 테스트 10개 통과
- [ ] Integration 테스트 8개 통과
- [ ] LoginIntegrationTest 8/8 통과 (기존 6/8 → 8/8)

**예상 테스트 수:** 기존 82개 + 신규 18개 = **100개**

---

## 📝 API 명세

### 1. GET /api/mypage/profile
```http
Authorization: Bearer {JWT_TOKEN}
```
**Response 200:**
```json
{
  "email": "user@example.com",
  "userName": "홍길동",
  "phoneNumber": "010-1234-5678",
  "accountType": "USER",
  "approvalStatus": "APPROVED"
}
```

### 2. PATCH /api/mypage/profile
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "userName": "홍길동",
  "phoneNumber": "010-9999-8888",
  "address": "서울시 강남구",
  "affiliation": "소프트캠퍼스",
  "position": "개발자"
}
```

### 3. DELETE /api/mypage/account
```http
Authorization: Bearer {JWT_TOKEN}
```
**Response 204:** No Content

---

## ⏭️ 다음 단계

Phase 15-1 완료 후:
- **Phase 15-2**: 활동 내역 조회 (Board/Comment 구현 후)
- **문서**: [phase15.2_activity.md](phase15.2_activity.md) 참조
