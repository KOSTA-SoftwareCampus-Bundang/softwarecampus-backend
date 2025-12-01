# 커뮤니티 API 연동 중간 보고서 - 백엔드

> **브랜치**: `Community-Integration`  
> **작성일**: 2025년 12월 2일  
> **상태**: 진행 중

---

## 📋 개요

커뮤니티(게시판) 기능의 프론트엔드-백엔드 API 연동 작업을 진행하며 발견한 문제점과 수정 사항을 정리합니다.

---

## 🐛 발견된 문제점 및 해결

### 1. 게시글/댓글 작성 시 작성자가 "시스템 관리자"로 표시되는 문제

**증상**: 로그인한 사용자가 글이나 댓글을 작성해도 작성자가 항상 "시스템 관리자"(id=1)로 표시됨

**원인**: `BoardServiceImpl.java`에서 실제 로그인한 사용자 ID 대신 하드코딩된 `1L` 사용

**수정 파일**: `src/main/java/com/softwarecampus/backend/service/board/BoardServiceImpl.java`

#### 수정 내용

##### createBoard 메서드
```java
// 수정 전
Account account = accountRepository.findById(1L)  // 하드코딩!
        .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));

// 수정 후
Account account = accountRepository.findById(userId)  // 실제 로그인 사용자 ID 사용
        .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
```

##### createComment 메서드
```java
// 수정 전
Account account = accountRepository.findById(1L)  // 하드코딩!
        .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));

// 수정 후
Account account = accountRepository.findById(userId)  // 실제 로그인 사용자 ID 사용
        .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
```

---

### 2. 게시글 목록에서 조회수/추천수가 항상 0으로 표시되는 문제

**증상**: 게시글 목록 조회 시 조회수(hits)와 추천수(likeCount)가 항상 0으로 표시됨

**원인**: 
1. `BoardListResponseDTO`에 `hits`, `likeCount` 필드가 없음
2. `BoardRepository`의 JPQL 쿼리에서 해당 값을 SELECT하지 않음

#### 수정 파일 1: `BoardListResponseDTO.java`

```java
// 추가된 필드
private Long hits;       // 조회수
private Long likeCount;  // 추천수
```

**전체 필드 구조**:
```java
public class BoardListResponseDTO {
    private Long id;
    private BoardCategory category;
    private String title;
    private Boolean secret;
    private String userNickName;
    private Long accountId;
    private Long commentsCount;
    private Long hits;        // 신규 추가
    private Long likeCount;   // 신규 추가
    private LocalDateTime createdAt;
}
```

#### 수정 파일 2: `BoardRepository.java`

**모든 검색 쿼리 수정** (4개 메서드):

```java
// 수정 전 (예: findBoardsByCategory)
@Query(value = "SELECT new ...BoardListResponseDTO(
    b.id, MAX(b.category), MAX(b.title), MAX(b.secret), MAX(a.userName), MAX(a.id),
    sum(case when c.isDeleted=false then 1 else 0 end),
    max(b.createdAt)) ...")

// 수정 후
@Query(value = "SELECT new ...BoardListResponseDTO(
    b.id, MAX(b.category), MAX(b.title), MAX(b.secret), MAX(a.userName), MAX(a.id),
    sum(case when c.isDeleted=false then 1 else 0 end),
    MAX(b.hits),               // 조회수 추가
    count(distinct r.id),      // 추천수 추가 (BoardRecommend 테이블)
    max(b.createdAt)) ...")
```

**수정된 메서드 목록**:
- `findBoardsByCategory()` - 카테고리별 목록 조회
- `findBoardsByTitle()` - 제목 검색
- `findBoardsByText()` - 내용 검색  
- `findBoardsByTitleAndText()` - 제목+내용 검색

---

## 📂 수정된 파일 요약

| 파일 | 수정 내용 |
|------|----------|
| `service/board/BoardServiceImpl.java` | `createBoard`, `createComment`에서 하드코딩된 사용자 ID 제거 |
| `dto/board/BoardListResponseDTO.java` | `hits`, `likeCount` 필드 추가 |
| `repository/board/BoardRepository.java` | 4개 쿼리에 조회수/추천수 SELECT 추가 |

---

## ⚠️ 주의사항

### 하드코딩된 값 발생 원인 추정
- 개발 초기 테스트용으로 `1L`을 임시로 사용
- 주석에 "실제론 1L 대신 userId가 인자로 전달"이라 적혀 있었으나 실제 코드는 수정되지 않음
- **향후 코드 리뷰 시 하드코딩된 값 점검 필요**

### DTO-Repository 쿼리 동기화
- `BoardListResponseDTO`의 생성자 파라미터 순서와 Repository 쿼리의 SELECT 순서가 **정확히 일치**해야 함
- 필드 추가 시 반드시 양쪽 모두 수정 필요

---

## ✅ 테스트 결과

- [x] 게시글 작성 시 실제 로그인 사용자 이름 표시
- [x] 댓글 작성 시 실제 로그인 사용자 이름 표시
- [x] 게시글 목록에서 조회수 정상 표시
- [x] 게시글 목록에서 추천수 정상 표시

---

## 🔜 후속 작업

1. ~~`BoardController.java`의 디버그 로깅 코드 제거~~ (선택사항)
2. 다른 서비스에도 유사한 하드코딩 패턴 존재 여부 점검
3. API 문서화 업데이트

---

## 📌 참고: API 응답 구조

### 게시글 목록 응답 (GET /api/boards)
```json
{
  "content": [
    {
      "id": 1,
      "category": "NOTICE",
      "title": "공지사항 제목",
      "secret": false,
      "userNickName": "홍길동",
      "accountId": 12,
      "commentsCount": 5,
      "hits": 123,
      "likeCount": 10,
      "createdAt": "2025-12-02 10:30:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "number": 0,
  "size": 10
}
```
