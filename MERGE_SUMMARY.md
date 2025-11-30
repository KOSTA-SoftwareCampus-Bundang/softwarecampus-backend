# backend-fix 브랜치 변경 내역

## 파일 Soft Delete 스케줄러 확장 및 최적화

### 1. 파일 엔티티 자동 정리 스케줄러 확장
- **목적**: 모든 파일 관련 엔티티의 Soft Delete 자동 정리
- **대상 엔티티**: CourseReviewFile, CourseReviewAttachment, BoardAttach, Attachment (Academy Q&A), CourseImage
- **유예기간**: 7일 → 1일로 변경
- **실행 주기**: 매일 새벽 5시
- **동작**: S3 파일 삭제 + DB 레코드 영구 삭제

### 2. 수동 실행 API 추가
- **엔드포인트**: `POST /admin/files/cleanup` (관리자 전용)
- **용도**: 테스트 및 긴급 정리

### 3. Repository 메서드 추가
- `CourseReviewAttachmentRepository.findByIsDeletedTrueAndDeletedAtBefore()`
- `BoardAttachRepository.findByIsDeletedTrueAndDeletedAtBefore()`
- `AttachmentRepository.findByIsDeletedTrueAndDeletedAtBefore()`
- `CourseImageRepository.findByIsDeletedTrueAndDeletedAtBefore()`

---

## CourseReviewFileService 개선

### 1. isDeleted 검증 추가
- **메서드**: `deleteReviewFile`, `restoreReviewFile`
- **내용**: 이미 삭제된 파일/삭제되지 않은 파일에 대한 명확한 검증

### 2. hardDeleteReviewFile 재정의
- **변경**: Soft Delete → 실제 Physical Delete (DB + S3)
- **용도**: 불법 콘텐츠, 개인정보 유출, 저작권 침해 등 긴급 제거
- **상세 JavaDoc 추가**: 경고 메시지, 사용 예시, 동작 설명

---

## ReviewLike 하드삭제 전환 (코드 리뷰 피드백 반영)

### 문제점
- 단순 상태값(LIKE/DISLIKE)에 Soft Delete 사용으로 로직 복잡도 증가
- 불필요한 히스토리 보관, DB 레코드 축적

### 해결
1. **엔티티 변경**: `BaseSoftDeleteSupportEntity` → `BaseTimeEntity`
2. **로직 단순화**:
   - 같은 타입 클릭 → DELETE (취소)
   - 다른 타입 클릭 → UPDATE (타입 변경)
   - 신규 클릭 → INSERT
3. **Repository 정리**:
   - `countByReviewIdAndTypeAndIsDeletedFalse` → `countByReviewIdAndType`
   - Soft Delete 메서드 제거
4. **코드 리뷰 피드백 반영**:
   - 삭제된 리뷰 조회 차단 (`findByIdAndCourseIdAndIsDeletedFalse` 사용)
   - courseId 검증 책임을 Service 내부로 이동
   - Interface 및 Controller 시그니처 업데이트

### 효과
- 로직 단순화 (복잡도 감소)
- 성능 개선 (불필요한 데이터 축적 방지, 인덱스 효율성 향상)
- 보안 강화 (삭제된 엔티티 검증)

---

## 커밋 이력

```
9dc1b71 수정: ReviewLike isDeleted 필터 제거 (하드삭제 전환으로 불필요)
204780e 리팩터링: ReviewLike를 하드삭제로 전환 및 코드 리뷰 피드백 반영
2dc7f95 수정: hardDeleteReviewFile을 실제 물리 삭제로 변경 (DB + S3)
6aa6202 수정: CourseReviewFileService에 isDeleted 검증 추가로 잘못된 삭제 상태 방지
f278c09 기능: 파일 정리 스케줄러 수동 실행 API 추가 (테스트용)
50d58a5 기능: FileCleanupScheduler 확장 - 모든 파일 엔티티 자동 정리 및 유예기간 1일로 변경
6e5a67c 기능: 파일 엔티티 Repository에 Soft Delete 조회 메서드 추가
```

---

## 주요 파일

### 변경
- `ReviewLike.java` - BaseTimeEntity로 변경
- `ReviewLikeRepository.java` - Soft Delete 메서드 제거
- `ReviewLikeServiceImpl.java` - 하드삭제 로직 단순화, courseId 검증 추가
- `ReviewLikeService.java` - toggleLike 시그니처 확장
- `ReviewLikeController.java` - Service에서 검증 처리
- `CourseReviewServiceImpl.java` - isDeleted 필터 제거
- `FileCleanupScheduler.java` - 5개 엔티티 처리 확장
- `CourseReviewFileServiceImpl.java` - hardDelete 재정의, 검증 추가
- `AdminController.java` - 수동 실행 API 추가

### 추가
- 4개 Repository에 `findByIsDeletedTrueAndDeletedAtBefore()` 메서드
