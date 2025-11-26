# Phase 15-2: 마이페이지 활동 내역 (미구현 - TODO)

**상태:** 📝 계획 단계  
**예상 시간:** 3-4시간  
**선행 조건:** Board/Comment CRUD API 구현 필요

---

## ⚠️ 현재 미구현 이유

### 차단 요소
1. **Board 엔티티에 User 연관관계 없음**
   - `Board.author` 필드 필요
   - Board와 User 간 @ManyToOne 관계 필요

2. **Comment 엔티티에 User 연관관계 없음**
   - `Comment.author` 필드 필요
   - Comment와 User 간 @ManyToOne 관계 필요

3. **Board CRUD API 미구현**
   - BoardRepository 없음
   - BoardService 없음
   - BoardController 없음

4. **Comment CRUD API 미구현**
   - CommentRepository 없음
   - CommentService 없음
   - CommentController 없음

---

## 📋 구현 예정 기능

### 1. GET /api/mypage/posts
**목적:** 내가 쓴 글 목록 조회

**Request:**
```http
GET /api/mypage/posts?page=0&size=10&category=CODING_STORY
Authorization: Bearer {JWT_TOKEN}
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "React 19 새로운 기능 정리",
      "category": "CODING_STORY",
      "createdAt": "2024-01-15T10:30:00",
      "views": 245,
      "commentsCount": 12
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 3,
  "totalPages": 1
}
```

---

### 2. GET /api/mypage/comments
**목적:** 내가 쓴 댓글 목록 조회

**Request:**
```http
GET /api/mypage/comments?page=0&size=10
Authorization: Bearer {JWT_TOKEN}
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "postId": 5,
      "postTitle": "Next.js 14 App Router 사용기",
      "content": "저도 비슷한 경험이 있어서 공감되네요!",
      "createdAt": "2024-01-14T15:20:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 5,
  "totalPages": 1
}
```

---

### 3. GET /api/mypage/bookmarks
**목적:** 찜한 과정 목록 조회

**선행 조건:** ✅ CourseFavorite 이미 구현됨

**Request:**
```http
GET /api/mypage/bookmarks?page=0&size=10
Authorization: Bearer {JWT_TOKEN}
```

**Response:**
```json
{
  "content": [
    {
      "courseId": 1,
      "title": "React 완벽 마스터",
      "academy": "코딩마스터",
      "category": "프론트엔드",
      "rating": 4.8,
      "bookmarkedAt": "2024-01-13T09:00:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 3,
  "totalPages": 1
}
```

---

### 4. GET /api/mypage/stats
**목적:** 활동 통계 조회

**Response:**
```json
{
  "postsCount": 3,
  "commentsCount": 12,
  "bookmarksCount": 5,
  "totalViews": 746
}
```

---

### 5. GET /api/mypage/activity/recent
**목적:** 최근 활동 타임라인

**Response:**
```json
{
  "activities": [
    {
      "type": "POST",
      "title": "React 19 새로운 기능 정리",
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "type": "COMMENT",
      "postTitle": "Next.js 14 App Router 사용기",
      "content": "저도 비슷한 경험이...",
      "createdAt": "2024-01-14T15:20:00"
    },
    {
      "type": "BOOKMARK",
      "courseTitle": "TypeScript 실전 프로젝트",
      "createdAt": "2024-01-13T09:00:00"
    }
  ]
}
```

---

## 🛠️ 필요한 작업

### 1. 엔티티 수정
```java
// Board.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User author;

// Comment.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User author;
```

### 2. Repository 생성
```java
// BoardRepository.java
Page<Board> findByAuthorIdAndDeletedAtIsNull(Long userId, Pageable pageable);
Page<Board> findByAuthorIdAndCategoryAndDeletedAtIsNull(
    Long userId, BoardCategory category, Pageable pageable);

// CommentRepository.java
Page<Comment> findByAuthorIdAndDeletedAtIsNull(Long userId, Pageable pageable);
```

### 3. DTO 생성
```java
// PostSummaryResponse.java
public class PostSummaryResponse {
    private Long id;
    private String title;
    private BoardCategory category;
    private LocalDateTime createdAt;
    private long views;
    private int commentsCount;
}

// CommentResponse.java
public class CommentResponse {
    private Long id;
    private Long postId;
    private String postTitle;
    private String content;
    private LocalDateTime createdAt;
}

// MyPageStatsResponse.java
public class MyPageStatsResponse {
    private int postsCount;
    private int commentsCount;
    private int bookmarksCount;
    private long totalViews;
}
```

### 4. Service 확장
```java
// MyPageService.java (신규)
public interface MyPageService {
    Page<PostSummaryResponse> getMyPosts(Long userId, Pageable pageable);
    Page<CommentResponse> getMyComments(Long userId, Pageable pageable);
    Page<BookmarkResponse> getMyBookmarks(Long userId, Pageable pageable);
    MyPageStatsResponse getStats(Long userId);
    List<ActivityResponse> getRecentActivity(Long userId);
}
```

### 5. Controller 확장
```java
// MyPageController.java
@GetMapping("/posts")
Page<PostSummaryResponse> getMyPosts(@AuthenticationPrincipal UserDetails user);

@GetMapping("/comments")
Page<CommentResponse> getMyComments(@AuthenticationPrincipal UserDetails user);

@GetMapping("/bookmarks")
Page<BookmarkResponse> getMyBookmarks(@AuthenticationPrincipal UserDetails user);

@GetMapping("/stats")
MyPageStatsResponse getStats(@AuthenticationPrincipal UserDetails user);

@GetMapping("/activity/recent")
List<ActivityResponse> getRecentActivity(@AuthenticationPrincipal UserDetails user);
```

---

## 📊 예상 작업 시간

| 작업 | 시간 |
|------|------|
| Board/Comment 엔티티 수정 | 30분 |
| Repository 생성 및 쿼리 메서드 | 30분 |
| DTO 생성 (5개) | 30분 |
| MyPageService 구현 | 1시간 |
| Controller 확장 | 30분 |
| 테스트 작성 (15-20개) | 1-1.5시간 |

**총 예상 시간:** 3-4시간

---

## 📌 구현 우선순위

1. **Phase 16**: Board CRUD API 구현
2. **Phase 17**: Comment CRUD API 구현
3. **Phase 18**: Phase 15-2 구현 (활동 내역)

---

## ⏭️ Phase 15-1 완료 후 작업

Phase 15-1 완료 시:
- ✅ 프로필 조회/수정/삭제 완성
- ✅ LoginIntegrationTest 8/8 통과
- ✅ 총 테스트 100개 달성

다음 Phase:
- Phase 16에서 Board 시스템 구현
- Phase 17에서 Comment 시스템 구현
- Phase 18에서 Phase 15-2 완성
