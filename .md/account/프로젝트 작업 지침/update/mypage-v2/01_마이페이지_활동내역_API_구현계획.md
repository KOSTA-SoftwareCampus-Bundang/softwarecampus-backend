# 마이페이지 활동 내역 API 구현 계획

## 📋 개요

| 항목 | 내용 |
|------|------|
| **작성일** | 2025-12-02 |
| **목적** | 마이페이지 Mock 데이터를 실제 API로 대체 |
| **우선순위** | Phase 3 (Phase 1-2 완료 후) |

---

## ✅ 완료된 기능 (Phase 1-2)

| 기능 | API | 상태 |
|------|-----|------|
| 프로필 조회 | `GET /api/mypage/profile` | ✅ 완료 |
| 프로필 수정 | `PATCH /api/mypage/profile` | ✅ 완료 |
| 비밀번호 변경 | `PUT /api/mypage/password` | ✅ 완료 |
| 회원 탈퇴 | `DELETE /api/mypage/account` | ✅ 완료 |
| 프로필 사진 | S3 업로드 | ✅ 완료 |

---

## 🎯 구현 대상 API (Phase 3)

### 1️⃣ 내가 쓴 글 목록
- **Endpoint**: `GET /api/mypage/posts`
- **설명**: 로그인한 사용자가 작성한 게시글 목록 조회
- **Query Params**: `page=0`, `size=10`, `sort=createdAt,desc`

**Response:**
```json
{
  "content": [
    {
      "id": 101,
      "title": "React 19 새로운 기능 정리",
      "category": "CODING_STORY",
      "hits": 245,
      "commentsCount": 12,
      "likeCount": 5,
      "createdAt": "2025-01-15T10:30:00"
    }
  ],
  "totalElements": 15,
  "totalPages": 2,
  "number": 0,
  "size": 10
}
```

---

### 2️⃣ 내가 쓴 댓글 목록
- **Endpoint**: `GET /api/mypage/comments`
- **설명**: 로그인한 사용자가 작성한 댓글 목록 조회
- **Query Params**: `page=0`, `size=10`, `sort=createdAt,desc`

**Response:**
```json
{
  "content": [
    {
      "id": 501,
      "text": "저도 같은 생각입니다!",
      "boardId": 101,
      "boardTitle": "React 19 새로운 기능 정리",
      "createdAt": "2025-01-15T11:00:00"
    }
  ],
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

---

### 3️⃣ 찜한 강좌 목록
- **Endpoint**: `GET /api/mypage/bookmarks`
- **설명**: 로그인한 사용자가 찜한 강좌 목록 조회
- **Query Params**: `page=0`, `size=10`

**Response:**
```json
{
  "content": [
    {
      "id": 201,
      "courseId": 50,
      "courseTitle": "React 완벽 마스터",
      "academyName": "코딩마스터",
      "categoryName": "프론트엔드",
      "rating": 4.8,
      "thumbnailUrl": "/images/course/react.jpg",
      "createdAt": "2025-01-10T09:00:00"
    }
  ],
  "totalElements": 8,
  "totalPages": 1,
  "number": 0,
  "size": 10
}
```

---

### 4️⃣ 활동 통계 (Dashboard KPI)
- **Endpoint**: `GET /api/mypage/stats`
- **설명**: 마이페이지 KPI 카드에 표시할 통계 데이터

**Response:**
```json
{
  "totalPosts": 15,
  "totalComments": 42,
  "totalBookmarks": 8,
  "totalViews": 1250,
  "postsThisMonth": 3,
  "commentsThisMonth": 10
}
```

---

## 📁 구현 파일 구조

```
src/main/java/com/softwarecampus/backend/
├── controller/
│   └── mypage/
│       └── MyPageController.java          # 기존 파일에 메서드 추가
├── service/
│   └── mypage/
│       ├── MyPageService.java             # 인터페이스 메서드 추가
│       └── MyPageServiceImpl.java         # 구현 추가
├── repository/
│   ├── board/
│   │   └── BoardRepository.java           # 기존 파일에 쿼리 추가
│   └── comment/
│       └── CommentRepository.java         # 기존 파일에 쿼리 추가
└── dto/
    └── mypage/
        ├── MyPostListResponseDTO.java     # 신규
        ├── MyCommentListResponseDTO.java  # 신규
        ├── MyBookmarkListResponseDTO.java # 신규
        └── MyStatsResponseDTO.java        # 신규
```

---

## 🔧 구현 체크리스트

### Task 1: 내가 쓴 글 목록 API
- [ ] `MyPostListResponseDTO` 생성
- [ ] `BoardRepository`에 `findByAccountId` 쿼리 추가
- [ ] `MyPageService`에 `getMyPosts` 메서드 추가
- [ ] `MyPageController`에 `GET /api/mypage/posts` 엔드포인트 추가
- [ ] 테스트

### Task 2: 내가 쓴 댓글 목록 API
- [ ] `MyCommentListResponseDTO` 생성
- [ ] `CommentRepository`에 `findByAccountId` 쿼리 추가
- [ ] `MyPageService`에 `getMyComments` 메서드 추가
- [ ] `MyPageController`에 `GET /api/mypage/comments` 엔드포인트 추가
- [ ] 테스트

### Task 3: 찜한 강좌 목록 API
- [ ] `MyBookmarkListResponseDTO` 생성
- [ ] 북마크 테이블/엔티티 확인 (없으면 생성)
- [ ] `BookmarkRepository` 쿼리 추가
- [ ] `MyPageService`에 `getMyBookmarks` 메서드 추가
- [ ] `MyPageController`에 `GET /api/mypage/bookmarks` 엔드포인트 추가
- [ ] 테스트

### Task 4: 활동 통계 API
- [ ] `MyStatsResponseDTO` 생성
- [ ] `MyPageService`에 `getMyStats` 메서드 추가
- [ ] 각 Repository에서 count 쿼리 추가
- [ ] `MyPageController`에 `GET /api/mypage/stats` 엔드포인트 추가
- [ ] 테스트

---

## 🗓️ 예상 일정

| Task | 예상 시간 | 우선순위 |
|------|----------|---------|
| Task 1: 내가 쓴 글 | 1시간 | 🔴 높음 |
| Task 2: 내가 쓴 댓글 | 1시간 | 🔴 높음 |
| Task 3: 찜한 강좌 | 1.5시간 | 🟡 중간 |
| Task 4: 활동 통계 | 30분 | 🟢 낮음 |

**총 예상 시간**: 4시간

---

## 📝 참고사항

### 기존 테이블 활용
- `board` 테이블: 내가 쓴 글 조회
- `comment` 테이블: 내가 쓴 댓글 조회
- `bookmark` 테이블: 찜한 강좌 (테이블 존재 여부 확인 필요)

### 보안
- 모든 API는 `@AuthenticationPrincipal`로 로그인 사용자 검증
- 다른 사용자의 데이터 조회 불가

### 페이지네이션
- Spring Data JPA `Pageable` 사용
- 프론트엔드와 일관성 유지 (page 0-indexed)

---

## 🔗 관련 문서

- 프론트엔드 Mock 데이터: `softwarecampus-frontend/docs/account/mypage/01_Mock_데이터_현황.md`
- 마이페이지 API 명세: `softwarecampus-frontend/docs/account/mypage/04_프론트_기반_백엔드_요구사항.md`
