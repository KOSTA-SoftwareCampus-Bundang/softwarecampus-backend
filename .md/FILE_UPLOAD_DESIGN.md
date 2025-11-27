# 파일 업로드 설계 의도

## 설계 철학

### 공용 컨트롤러 + 폴더 기반 도메인 구분

**핵심 아이디어**: 하나의 `FileController`로 모든 도메인의 파일 업로드를 처리하되, S3 버킷 내에서 **폴더(prefix)로 도메인을 구분**

```
S3 Bucket: softwarecampus-files
├── board/           # 게시판 첨부파일
│   ├── uuid-file1.pdf
│   ├── uuid-file2.jpg
│   └── ...
├── academy/         # 아카데미 관련 이미지
│   ├── uuid-logo.png
│   ├── uuid-banner.jpg
│   └── ...
├── course/          # 강좌 썸네일/자료
│   ├── uuid-thumbnail.jpg
│   └── ...
├── profile/         # 사용자 프로필 이미지
│   ├── uuid-avatar.jpg
│   └── ...
└── temp/            # 임시 파일
    └── ...
```

---

## 왜 이렇게 설계했나?

### 1. **단일 책임 원칙 (SRP)**
- 파일 업로드/삭제는 **기술적 관심사**이지 비즈니스 로직이 아님
- 도메인 컨트롤러(BoardController, AcademyController)는 비즈니스 로직에 집중
- 파일 관리는 공용 컨트롤러가 담당 → 책임 분리

### 2. **중복 코드 제거 (DRY)**
만약 도메인별로 분리하면:
```java
// ❌ 중복 코드 발생
// BoardController
@PostMapping("/api/board/upload")
public ResponseEntity<?> uploadBoardFile(MultipartFile file) {
    String url = s3Service.uploadFile(file, "board");
    return ResponseEntity.ok(url);
}

// AcademyController
@PostMapping("/api/academy/upload")
public ResponseEntity<?> uploadAcademyFile(MultipartFile file) {
    String url = s3Service.uploadFile(file, "academy");
    return ResponseEntity.ok(url);
}

// CourseController
@PostMapping("/api/course/upload")
public ResponseEntity<?> uploadCourseFile(MultipartFile file) {
    String url = s3Service.uploadFile(file, "course");
    return ResponseEntity.ok(url);
}
```

✅ 공용 컨트롤러 사용 시:
```java
// FileController 하나로 모두 처리
@PostMapping("/api/files/upload")
public ResponseEntity<?> uploadFile(
    @RequestParam MultipartFile file,
    @RequestParam String folder) {  // board, academy, course 등
    String url = s3Service.uploadFile(file, folder);
    return ResponseEntity.ok(url);
}
```

### 3. **S3 폴더 구조의 장점**

#### 관리 용이성
- AWS S3 콘솔에서 도메인별로 파일 확인 가능
- 특정 도메인 파일만 필터링/검색 가능
- 수명 주기 정책을 폴더별로 다르게 적용 가능
  ```
  예: temp/ 폴더는 7일 후 자동 삭제
      profile/ 폴더는 영구 보관
  ```

#### 비용 최적화
- S3 Intelligent-Tiering을 폴더별로 적용
- 접근 빈도가 낮은 폴더(예: archive/)는 Glacier로 자동 이동

#### 권한 관리
- IAM 정책으로 폴더별 접근 권한 제어 가능
  ```json
  {
    "Effect": "Allow",
    "Action": ["s3:GetObject"],
    "Resource": ["arn:aws:s3:::bucket-name/public/*"]
  }
  ```

---

## 사용 시나리오

### 시나리오 1: 프론트엔드에서 직접 업로드

```javascript
// 게시판 글 작성 시 파일 첨부
const uploadBoardFile = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('folder', 'board');  // 폴더 지정
  
  const response = await fetch('/api/files/upload', {
    method: 'POST',
    body: formData
  });
  
  const { fileUrl } = await response.json();
  return fileUrl;  // https://bucket.s3.region.amazonaws.com/board/uuid-file.jpg
};

// 프로필 이미지 업로드
const uploadProfileImage = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('folder', 'profile');  // 다른 폴더
  
  // ... 동일한 엔드포인트 사용
};
```

### 시나리오 2: 백엔드 서비스 레이어에서 처리

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final S3Service s3Service;
    private final BoardRepository boardRepository;
    
    @Transactional
    public BoardResponse createBoard(BoardRequest request, MultipartFile file) {
        // 1. 파일 업로드 (S3Service 직접 사용)
        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = s3Service.uploadFile(file, "board");
        }
        
        // 2. DB에 저장
        Board board = Board.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .attachmentUrl(fileUrl)  // S3 URL 저장
            .build();
        
        boardRepository.save(board);
        
        return BoardResponse.from(board);
    }
    
    @Transactional
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new BoardNotFoundException());
        
        // 1. S3에서 파일 삭제
        if (board.getAttachmentUrl() != null) {
            s3Service.deleteFile(board.getAttachmentUrl());
        }
        
        // 2. DB에서 삭제
        boardRepository.delete(board);
    }
}
```

---

## 대안 설계와 비교

### 대안 1: DB 기록 방식 (폴더 구분 없이 버킷 루트 사용)

**구조:**
```
S3 Bucket: softwarecampus-files (모든 파일이 루트에 flat하게 저장)
├── uuid1-file.pdf
├── uuid2-image.jpg
├── uuid3-logo.png
└── uuid4-thumbnail.jpg

DB 테이블별로 파일 정보 저장:
- board_files 테이블
- academy_files 테이블
- course_files 테이블
- profile_files 테이블
```

**구현 예시:**
```java
// Board 도메인
@Entity
public class Board {
    @Id
    private Long id;
    private String title;
    private String content;
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    private List<BoardFile> files;  // 첨부파일 테이블
}

@Entity
public class BoardFile {
    @Id
    private Long id;
    
    @ManyToOne
    private Board board;
    
    private String fileName;      // uuid-file.pdf
    private String originalName;  // 사용자가 업로드한 원본 파일명
    private String s3Url;         // https://bucket.s3.region.amazonaws.com/uuid-file.pdf
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}
```

#### ✅ 장점

**1. DB 중심의 명확한 추적**
```sql
-- 특정 게시글의 모든 파일 조회
SELECT * FROM board_files WHERE board_id = 123;

-- 특정 사용자가 업로드한 모든 파일
SELECT * FROM board_files bf 
JOIN boards b ON bf.board_id = b.id 
WHERE b.user_id = 456;

-- 도메인별 파일 용량 집계
SELECT SUM(file_size) FROM board_files;
SELECT SUM(file_size) FROM academy_files;
```

**2. 파일 메타데이터 관리 용이**
- 원본 파일명, 업로드 시간, 파일 크기, MIME 타입 등 상세 정보 저장
- 파일 다운로드 횟수, 마지막 접근 시간 등 추가 정보 기록 가능
- 트랜잭션 내에서 파일 정보와 비즈니스 데이터 함께 관리

**3. 복잡한 쿼리 지원**
```sql
-- 일주일 이내 업로드된 대용량 파일 검색
SELECT * FROM board_files 
WHERE uploaded_at > NOW() - INTERVAL 7 DAY 
AND file_size > 10485760  -- 10MB
ORDER BY file_size DESC;
```

**4. 도메인별 테이블 분리로 권한 제어 명확**
- 게시판 파일은 `board_files` 테이블로만 접근
- JPA Repository 레벨에서 도메인 격리 보장

**5. S3 구조 심플**
- 폴더 구조 신경 쓸 필요 없음
- 파일명(UUID)만 유니크하면 됨

#### ❌ 단점

**1. S3 콘솔에서 파일 관리 어려움**
```
문제 상황:
- S3 콘솔에 수만 개의 파일이 flat하게 나열됨
- 특정 도메인 파일만 찾기 어려움
- 시각적으로 파악 불가능
```

**2. S3 수명 주기 정책 적용 곤란**
```
불가능한 시나리오:
- "임시 파일은 7일 후 자동 삭제" → 폴더가 없어서 적용 불가
- "프로필 이미지는 Glacier로 이동" → 구분 불가
- "오래된 게시판 첨부파일만 아카이빙" → 폴더 구분 필요
```

**3. DB 의존성 증가**
```java
문제:
- S3에서 파일 삭제 시 DB 정보 필요
- DB 장애 시 파일 추적 불가능
- DB 백업/복구 시 S3 파일과 동기화 이슈
```

**4. 고아(Orphan) 파일 발생 위험**
```java
// 트랜잭션 실패 시나리오
@Transactional
public void createBoard(BoardRequest request, MultipartFile file) {
    // 1. S3에 파일 업로드 (성공)
    String fileUrl = s3Service.uploadFile(file, "");
    
    // 2. DB 저장 (실패 - 트랜잭션 롤백)
    Board board = new Board(...);
    boardRepository.save(board);  // 예외 발생
    
    // 결과: S3에는 파일이 남았지만 DB에는 기록 없음 → 고아 파일
}
```

**해결 방법: @TransactionalEventListener 사용**
```java
@Service
public class BoardService {
    @Transactional
    public void createBoard(BoardRequest request, MultipartFile file) {
        Board board = new Board(...);
        boardRepository.save(board);
        
        // 트랜잭션 커밋 후 파일 업로드
        applicationEventPublisher.publishEvent(
            new BoardCreatedEvent(board.getId(), file)
        );
    }
}

@Component
public class FileUploadEventListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBoardCreated(BoardCreatedEvent event) {
        String fileUrl = s3Service.uploadFile(event.getFile(), "");
        // 파일 URL 업데이트
    }
}
```

**5. 대량 파일 관리 시 성능 이슈**
```
S3 List Objects API:
- 한 번에 최대 1,000개 객체 반환
- 수만 개 파일이 루트에 있으면 페이지네이션 필요
- 폴더가 있으면 prefix로 필터링 가능 (훨씬 빠름)
```

**6. CloudFront 캐싱 전략 복잡**
```
폴더 구분 시:
- /profile/* → 캐시 1년
- /temp/* → 캐시 1시간
- /board/* → 캐시 1주일

루트에 flat하게 저장 시:
- 파일별 개별 캐시 정책 필요
- URL 패턴으로 구분 불가
```

#### 🔄 하이브리드 방식

**최선의 해결책: 폴더 구분 + DB 추적 병행**

```java
@Entity
public class BoardFile {
    @Id
    private Long id;
    
    @ManyToOne
    private Board board;
    
    private String folder = "board";  // S3 폴더 (고정값)
    private String fileName;          // uuid-file.pdf
    private String s3Url;             // https://.../board/uuid-file.pdf
    // ... 메타데이터
}

// 업로드 시
String fileUrl = s3Service.uploadFile(file, "board");
BoardFile boardFile = BoardFile.builder()
    .folder("board")
    .fileName(extractFileName(fileUrl))
    .s3Url(fileUrl)
    .build();
```

**장점:**
- ✅ S3에서 폴더로 시각적 구분 가능
- ✅ 수명 주기 정책 폴더별 적용 가능
- ✅ DB에서 상세 메타데이터 관리
- ✅ 복잡한 쿼리 지원
- ✅ CloudFront 캐싱 전략 간편

---

### 대안 2: 도메인별 컨트롤러에 업로드 메서드 추가

```java
@RestController
@RequestMapping("/api/board")
public class BoardController {
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(MultipartFile file) {
        // ... 업로드 로직
    }
}

@RestController
@RequestMapping("/api/academy")
public class AcademyController {
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(MultipartFile file) {
        // ... 동일한 업로드 로직 중복
    }
}
```

**단점:**
- ❌ 중복 코드
- ❌ 유지보수 어려움 (로직 변경 시 모든 컨트롤러 수정 필요)
- ❌ 도메인 컨트롤러가 파일 관리까지 책임짐 (SRP 위반)

### 대안 3: 도메인별 별도 S3 버킷 사용

```
Bucket: softwarecampus-board
Bucket: softwarecampus-academy
Bucket: softwarecampus-course
```

**단점:**
- ❌ 버킷 수 증가 → 관리 복잡도 증가
- ❌ 버킷당 비용 발생 (작은 규모에서는 비효율)
- ❌ CORS, 정책 등을 각 버킷마다 설정 필요

### ✅ 현재 설계: 공용 컨트롤러 + 폴더 구분

**장점:**
- ✅ 코드 중복 없음
- ✅ 단일 버킷으로 관리 간편
- ✅ 폴더로 논리적 구분 (물리적으로는 같은 버킷)
- ✅ 확장 용이 (새 도메인 추가 시 폴더명만 추가)

---

## 폴더 네이밍 컨벤션

### 권장 폴더 구조

```
/board          # 게시판 첨부파일
/academy        # 아카데미 이미지/로고
/course         # 강좌 썸네일/자료
/profile        # 사용자 프로필 이미지
/temp           # 임시 파일 (수명 주기 정책으로 자동 삭제)
/public         # 공개 자료 (인증 불필요)
/private        # 비공개 자료 (인증 필요)
```

### 하위 폴더 사용 (선택)

더 세분화된 구조가 필요한 경우:
```java
// 강좌 도메인 내에서 타입별 구분
s3Service.uploadFile(file, "course/thumbnails");
s3Service.uploadFile(file, "course/materials");
s3Service.uploadFile(file, "course/videos");
```

결과:
```
/course
  /thumbnails
    /uuid-thumb1.jpg
  /materials
    /uuid-material.pdf
  /videos
    /uuid-video.mp4
```

---

## 보안 고려사항

### 1. 폴더명 검증

악의적인 폴더명 방지:
```java
// S3Service에 검증 로직 추가 권장
private static final Set<String> ALLOWED_FOLDERS = Set.of(
    "board", "academy", "course", "profile", "temp"
);

public String uploadFile(MultipartFile file, String folder) {
    if (!ALLOWED_FOLDERS.contains(folder)) {
        throw new IllegalArgumentException("허용되지 않은 폴더입니다: " + folder);
    }
    // ... 업로드 로직
}
```

### 2. 파일 타입 검증

```java
private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
    "jpg", "jpeg", "png", "gif", "pdf", "docx"
);

public String uploadFile(MultipartFile file, String folder) {
    String extension = getFileExtension(file.getOriginalFilename());
    if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
        throw new IllegalArgumentException("허용되지 않은 파일 형식입니다");
    }
    // ... 업로드 로직
}
```

### 3. 파일 크기 제한

application.properties:
```properties
# 파일 업로드 크기 제한
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

---

## 확장 가능성

### 1. 도메인별 커스텀 로직 추가

```java
@Service
public class S3Service {
    
    public String uploadFile(MultipartFile file, String folder) {
        // 폴더별 특별 처리
        switch (folder) {
            case "profile":
                // 이미지 리사이징
                file = resizeImage(file, 200, 200);
                break;
            case "course":
                // 워터마크 추가
                file = addWatermark(file);
                break;
        }
        
        // ... 업로드 로직
    }
}
```

### 2. 폴더별 접근 권한 설정

```java
// SecurityConfig에서 폴더별 권한 제어
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers(GET, "/api/files/public/**").permitAll()
        .requestMatchers(GET, "/api/files/private/**").authenticated()
        .requestMatchers(POST, "/api/files/upload").authenticated()
    );
}
```

---

## 결론

### 방식별 비교표

| 항목 | DB 추적 방식 (루트 저장) | 폴더 구분 방식 (현재) | 하이브리드 (권장) |
|------|----------------------|-------------------|----------------|
| S3 시각적 관리 | ❌ 어려움 (flat) | ✅ 폴더로 구분 | ✅ 폴더로 구분 |
| DB 메타데이터 | ✅ 상세 관리 | ⚠️ 필요시 추가 | ✅ 상세 관리 |
| 복잡한 쿼리 | ✅ 지원 | ❌ 제한적 | ✅ 지원 |
| 수명 주기 정책 | ❌ 적용 곤란 | ✅ 폴더별 적용 | ✅ 폴더별 적용 |
| 고아 파일 위험 | ⚠️ 있음 | ⚠️ 있음 | ⚠️ 있음 (이벤트로 해결) |
| CloudFront 캐싱 | ❌ 복잡 | ✅ 간편 | ✅ 간편 |
| 구현 복잡도 | 🟡 중간 | 🟢 낮음 | 🟡 중간 |
| 코드 중복 | ✅ 없음 | ✅ 없음 | ✅ 없음 |

### 권장사항

#### 🥇 **하이브리드 방식 추천**

**폴더 구분 + DB 메타데이터 병행 관리**

```java
// 엔티티 예시
@Entity
public class BoardFile {
    @Id
    private Long id;
    
    @ManyToOne
    private Board board;
    
    private String s3Key;         // board/uuid-file.pdf (S3 키)
    private String originalName;  // user-uploaded-name.pdf
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}

// 서비스 로직
@Transactional
public BoardResponse createBoard(BoardRequest request, MultipartFile file) {
    // 1. DB 먼저 저장
    Board board = boardRepository.save(new Board(request));
    
    // 2. 트랜잭션 커밋 후 파일 업로드 (이벤트 사용)
    if (file != null) {
        applicationEventPublisher.publishEvent(
            new FileUploadEvent(board.getId(), file, "board")
        );
    }
    
    return BoardResponse.from(board);
}
```

**이유:**
- ✅ S3 콘솔에서 폴더로 관리 용이
- ✅ DB에서 파일 메타데이터 추적 가능
- ✅ 수명 주기 정책, CloudFront 캐싱 간편
- ✅ 복잡한 검색/집계 쿼리 지원
- ✅ 고아 파일은 배치 작업으로 정리

#### 🥈 **단순한 프로젝트는 폴더 구분만으로도 충분**

다음 경우 현재 방식(폴더 구분) 유지:
- 파일 메타데이터가 중요하지 않은 경우
- 복잡한 파일 검색이 필요 없는 경우
- 빠른 개발이 우선인 경우

#### 🥉 **DB 추적 방식 (루트 저장)은 비추천**

다음 이슈로 권장하지 않음:
- S3 콘솔 관리 어려움
- 수명 주기 정책 적용 불가
- 대량 파일 시 성능 저하
- CloudFront 캐싱 전략 복잡

### 현재 설계의 핵심 (폴더 구분 방식)

✅ **단일 공용 컨트롤러** (`FileController`)  
✅ **폴더 파라미터로 도메인 구분** (`folder=board`, `folder=academy`)  
✅ **S3 버킷 내 폴더 구조로 물리적 분리**  
✅ **각 도메인 서비스는 S3Service만 주입받아 사용**

### 향후 개선 방향

**필요 시 하이브리드로 마이그레이션:**

1. 각 도메인에 파일 메타데이터 테이블 추가
2. S3 업로드를 트랜잭션 이벤트로 분리
3. 배치 작업으로 고아 파일 정리
4. DB 쿼리로 파일 검색/통계 제공

**점진적 개선 가능:**
- 초기: 폴더 구분만 사용 (현재)
- 필요 시: 특정 도메인부터 DB 추적 추가
- 고도화: 모든 도메인에 메타데이터 관리

### 사용 패턴

1. **간단한 업로드**: 프론트엔드 → `FileController` 직접 호출
2. **비즈니스 로직 포함**: 프론트엔드 → 도메인 컨트롤러 → 도메인 서비스 → `S3Service` 호출

---

## 트랜잭션 기반 파일 업로드 패턴 (서비스별 파일 테이블 분리 환경)

### 현재 프로젝트 상황
- ✅ DB 스키마에 서비스별 파일 테이블 분리 (`board_attach`, `course_image` 등)
- ✅ S3 폴더 구분 사용 (`board/`, `course/`, `academy/` 등)
- ⚠️ 트랜잭션 실패 시 고아 파일 발생 가능 → **해결 필요**

### 고아 파일 문제

```java
// ❌ 문제 상황: 트랜잭션 실패 시 S3 파일만 남음
@Transactional
public void createBoard(MultipartFile file) {
    String fileUrl = s3Service.uploadFile(file, "board");  // S3 업로드 성공
    Board board = boardRepository.save(new Board(...));    // DB 저장
    boardAttachRepository.save(new BoardAttach(fileUrl));  // 예외 발생 → 롤백
    // 결과: S3에는 파일 있음, DB에는 기록 없음 → 고아 파일
}
```

### ✅ 해결 방법: 트랜잭션 이벤트 사용

Spring의 `@TransactionalEventListener`를 사용하여 **트랜잭션 커밋 후 S3 업로드** 수행

#### 필요한 컴포넌트 구현

**1. 파일 업로드 이벤트 클래스**

`service/event/FileUploadEvent.java`
```java
package com.softwarecampus.backend.service.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Getter
@RequiredArgsConstructor
public class FileUploadEvent {
    private final Long entityId;           // 연관된 엔티티 ID (Board ID, Course ID 등)
    private final MultipartFile file;      // 업로드할 파일
    private final String folder;           // S3 폴더 (board, course, academy 등)
    private final String entityType;       // 엔티티 타입 (BOARD, COURSE 등)
}
```

**2. 파일 삭제 이벤트 클래스**

`service/event/FileDeleteEvent.java`
```java
package com.softwarecampus.backend.service.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FileDeleteEvent {
    private final String fileUrl;  // 삭제할 S3 파일 URL
}
```

**3. 이벤트 리스너**

`service/event/FileEventListener.java`
```java
package com.softwarecampus.backend.service.event;

import com.softwarecampus.backend.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileEventListener {

    private final S3Service s3Service;

    /**
     * 트랜잭션 커밋 후 파일 업로드 수행
     * DB 저장이 성공한 경우에만 S3 업로드 실행
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileUpload(FileUploadEvent event) {
        try {
            log.info("Processing file upload after transaction commit - entityId: {}, folder: {}", 
                    event.getEntityId(), event.getFolder());
            
            String fileUrl = s3Service.uploadFile(event.getFile(), event.getFolder());
            
            log.info("File uploaded successfully - entityId: {}, fileUrl: {}", 
                    event.getEntityId(), fileUrl);
            
            // TODO: 각 도메인 서비스에서 파일 URL을 DB에 업데이트하는 로직 필요
            // 예: boardService.updateFileUrl(event.getEntityId(), fileUrl);
            
        } catch (Exception e) {
            log.error("Failed to upload file after transaction commit - entityId: {}", 
                    event.getEntityId(), e);
            // 실패 시 보상 트랜잭션 또는 재시도 큐에 추가 가능
        }
    }

    /**
     * 트랜잭션 커밋 후 파일 삭제 수행
     * DB 삭제가 성공한 경우에만 S3 파일 삭제 실행
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileDelete(FileDeleteEvent event) {
        try {
            log.info("Processing file delete after transaction commit - fileUrl: {}", 
                    event.getFileUrl());
            
            s3Service.deleteFile(event.getFileUrl());
            
            log.info("File deleted successfully - fileUrl: {}", event.getFileUrl());
            
        } catch (Exception e) {
            log.error("Failed to delete file after transaction commit - fileUrl: {}", 
                    event.getFileUrl(), e);
            // S3 삭제 실패는 로그만 남기고 무시 (고아 파일은 배치로 정리)
        }
    }
}
```

### 사용 패턴별 구현 방법

#### 패턴 1: 트랜잭션 이벤트 사용 (🥇 권장)

**장점**: 고아 파일 완전 방지, DB 우선 저장  
**단점**: 2단계 처리 (DB 저장 → S3 업로드 → URL 업데이트)

**예시: 게시판 파일 업로드**

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final ApplicationEventPublisher eventPublisher;
    private final BoardRepository boardRepository;
    private final BoardAttachRepository boardAttachRepository;
    
    @Transactional
    public BoardResponse createBoard(BoardRequest request, MultipartFile file) {
        // 1. 게시글 먼저 저장
        Board board = Board.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .build();
        boardRepository.save(board);
        
        // 2. 트랜잭션 커밋 후 파일 업로드 이벤트 발행
        if (file != null && !file.isEmpty()) {
            eventPublisher.publishEvent(
                new FileUploadEvent(board.getId(), file, "board", "BOARD")
            );
        }
        
        return BoardResponse.from(board);
    }
    
    // 이벤트 리스너에서 업로드 후 호출되는 메서드
    @Transactional
    public void updateBoardFileUrl(Long boardId, String fileUrl, String originalName, Long fileSize) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new BoardNotFoundException());
        
        BoardAttach attach = BoardAttach.builder()
            .board(board)
            .fileUrl(fileUrl)
            .originalName(originalName)
            .fileSize(fileSize)
            .uploadedAt(LocalDateTime.now())
            .build();
        
        boardAttachRepository.save(attach);
    }
}
```

**예시: 강좌 이미지 업로드**

```java
@Service
@RequiredArgsConstructor
public class CourseService {
    private final ApplicationEventPublisher eventPublisher;
    private final CourseRepository courseRepository;
    private final CourseImageRepository courseImageRepository;
    
    @Transactional
    public CourseResponse createCourse(CourseRequest request, MultipartFile thumbnail) {
        Course course = Course.builder()
            .name(request.getName())
            .description(request.getDescription())
            .build();
        courseRepository.save(course);
        
        // 썸네일 업로드 이벤트 발행
        if (thumbnail != null && !thumbnail.isEmpty()) {
            eventPublisher.publishEvent(
                new FileUploadEvent(course.getId(), thumbnail, "course", "COURSE")
            );
        }
        
        return CourseResponse.from(course);
    }
    
    @Transactional
    public void updateCourseThumbnail(Long courseId, String fileUrl, String originalName) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        
        CourseImage image = CourseImage.builder()
            .course(course)
            .imageUrl(fileUrl)
            .originalName(originalName)
            .build();
        
        courseImageRepository.save(image);
    }
}
```

#### 패턴 2: 직접 업로드 후 DB 저장 (🥈 간단함)

**장점**: 단순한 구조, 한 번에 처리  
**단점**: 트랜잭션 실패 시 고아 파일 발생 가능

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final S3Service s3Service;
    private final BoardRepository boardRepository;
    private final BoardAttachRepository boardAttachRepository;
    
    @Transactional
    public BoardResponse createBoard(BoardRequest request, MultipartFile file) {
        // 1. S3 업로드 먼저 수행
        String fileUrl = null;
        String originalName = null;
        Long fileSize = null;
        
        if (file != null && !file.isEmpty()) {
            fileUrl = s3Service.uploadFile(file, "board");
            originalName = file.getOriginalFilename();
            fileSize = file.getSize();
        }
        
        // 2. 게시글 저장
        Board board = Board.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .build();
        boardRepository.save(board);
        
        // 3. 파일 정보 저장
        if (fileUrl != null) {
            BoardAttach attach = BoardAttach.builder()
                .board(board)
                .fileUrl(fileUrl)
                .originalName(originalName)
                .fileSize(fileSize)
                .uploadedAt(LocalDateTime.now())
                .build();
            boardAttachRepository.save(attach);
        }
        
        return BoardResponse.from(board);
    }
}
```

⚠️ **주의**: DB 저장 실패 시 S3에 파일만 남을 수 있음 → 배치 작업으로 고아 파일 정리 필요

#### 패턴 3: 2단계 커밋 (🥉 가장 안전하지만 복잡)

**장점**: 고아 파일 완전 방지  
**단점**: 구현 복잡도 높음, 컨트롤러에서 2번 호출 필요

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final S3Service s3Service;
    private final BoardRepository boardRepository;
    private final BoardAttachRepository boardAttachRepository;
    
    // Step 1: 게시글만 먼저 저장
    @Transactional
    public Long createBoardStep1(BoardRequest request) {
        Board board = Board.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .build();
        boardRepository.save(board);
        return board.getId();
    }
    
    // Step 2: 트랜잭션 밖에서 S3 업로드
    public String uploadFileStep2(MultipartFile file, String folder) {
        return s3Service.uploadFile(file, folder);
    }
    
    // Step 3: 파일 URL 업데이트
    @Transactional
    public void updateFileUrlStep3(Long boardId, String fileUrl, MultipartFile file) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new BoardNotFoundException());
        
        BoardAttach attach = BoardAttach.builder()
            .board(board)
            .fileUrl(fileUrl)
            .originalName(file.getOriginalFilename())
            .fileSize(file.getSize())
            .uploadedAt(LocalDateTime.now())
            .build();
        
        boardAttachRepository.save(attach);
    }
}

// 컨트롤러에서 사용
@RestController
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;
    
    @PostMapping("/api/board")
    public ResponseEntity<?> createBoard(
            @RequestPart BoardRequest request,
            @RequestPart MultipartFile file) {
        
        // Step 1: DB 저장
        Long boardId = boardService.createBoardStep1(request);
        
        // Step 2: S3 업로드
        String fileUrl = boardService.uploadFileStep2(file, "board");
        
        // Step 3: URL 업데이트
        boardService.updateFileUrlStep3(boardId, fileUrl, file);
        
        return ResponseEntity.ok().build();
    }
}
```

### 파일 삭제 패턴

#### 이벤트 기반 삭제 (권장)

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final ApplicationEventPublisher eventPublisher;
    private final BoardRepository boardRepository;
    
    @Transactional
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new BoardNotFoundException());
        
        // 첨부파일 URL 수집
        List<String> fileUrls = board.getAttachments().stream()
            .map(BoardAttach::getFileUrl)
            .collect(Collectors.toList());
        
        // 1. DB에서 먼저 삭제
        boardRepository.delete(board);
        
        // 2. 트랜잭션 커밋 후 S3 파일 삭제 이벤트 발행
        for (String fileUrl : fileUrls) {
            eventPublisher.publishEvent(new FileDeleteEvent(fileUrl));
        }
    }
}
```

#### 직접 삭제

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final S3Service s3Service;
    private final BoardRepository boardRepository;
    
    @Transactional
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new BoardNotFoundException());
        
        // 첨부파일 URL 수집
        List<String> fileUrls = board.getAttachments().stream()
            .map(BoardAttach::getFileUrl)
            .collect(Collectors.toList());
        
        // 1. DB 먼저 삭제
        boardRepository.delete(board);
        
        // 2. 트랜잭션 외부에서 S3 삭제
        for (String fileUrl : fileUrls) {
            try {
                s3Service.deleteFile(fileUrl);
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패 (배치로 정리 예정): {}", fileUrl, e);
                // S3 삭제 실패는 무시하고 계속 진행
            }
        }
    }
}
```

### Soft Delete 패턴 (🥇 권장 - 현재 프로젝트 정책)

현재 프로젝트는 **복구 가능한 soft-delete**를 기본으로 사용합니다.

#### Soft Delete 전략

**핵심 원칙:**
1. 게시글/데이터 삭제 시 `is_deleted=true`, `deleted_at=현재시간` 설정
2. S3 파일은 **즉시 삭제하지 않고 유지**
3. 복구 기간(예: 30일) 이후 배치 작업으로 hard-delete 수행
4. Hard-delete 시 DB와 S3 파일 함께 삭제

#### 엔티티 구조

```java
@Entity
public class Board {
    @Id
    private Long id;
    
    private String title;
    private String content;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    private List<BoardAttach> attachments;
    
    // Soft delete 메서드
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}

@Entity
public class BoardAttach {
    @Id
    private Long id;
    
    @ManyToOne
    private Board board;
    
    private String fileUrl;
    private String originalName;
    private Long fileSize;
    private LocalDateTime uploadedAt;
}
```

#### Soft Delete 구현

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    
    /**
     * Soft Delete: DB에서 삭제 플래그만 설정, S3 파일은 유지
     */
    @Transactional
    public void softDeleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new BoardNotFoundException());
        
        // is_deleted = true, deleted_at = 현재시간 설정
        board.softDelete();
        boardRepository.save(board);
        
        log.info("Board soft-deleted: boardId={}, deletedAt={}", 
                boardId, board.getDeletedAt());
        
        // S3 파일은 삭제하지 않음 (복구 가능)
    }
    
    /**
     * 복구: Soft delete된 게시글 복구
     */
    @Transactional
    public void restoreBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new BoardNotFoundException());
        
        if (!board.getIsDeleted()) {
            throw new IllegalStateException("삭제되지 않은 게시글입니다.");
        }
        
        board.setIsDeleted(false);
        board.setDeletedAt(null);
        boardRepository.save(board);
        
        log.info("Board restored: boardId={}", boardId);
    }
}
```

#### Hard Delete (배치 작업에서만 실행)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DeletedBoardCleanupScheduler {
    
    private final BoardRepository boardRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Value("${app.board.recovery-period-days:30}")
    private int recoveryPeriodDays;
    
    /**
     * 매일 새벽 2시: 복구 기간이 지난 soft-deleted 게시글 영구 삭제
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupDeletedBoards() {
        log.info("Starting deleted board cleanup batch job");
        
        LocalDateTime threshold = LocalDateTime.now().minusDays(recoveryPeriodDays);
        
        // 복구 기간이 지난 soft-deleted 게시글 조회
        List<Board> expiredBoards = boardRepository
            .findByIsDeletedTrueAndDeletedAtBefore(threshold);
        
        log.info("Found {} boards to hard-delete (deleted before {})", 
                expiredBoards.size(), threshold);
        
        for (Board board : expiredBoards) {
            hardDeleteBoard(board);
        }
        
        log.info("Deleted board cleanup batch job completed");
    }
    
    /**
     * Hard Delete: DB와 S3 파일 모두 영구 삭제
     */
    @Transactional
    public void hardDeleteBoard(Board board) {
        Long boardId = board.getId();
        
        // 첨부파일 URL 수집
        List<String> fileUrls = board.getAttachments().stream()
            .map(BoardAttach::getFileUrl)
            .collect(Collectors.toList());
        
        // 1. DB에서 영구 삭제
        boardRepository.delete(board);
        
        log.info("Board hard-deleted from DB: boardId={}, fileCount={}", 
                boardId, fileUrls.size());
        
        // 2. 트랜잭션 커밋 후 S3 파일 삭제 이벤트 발행
        for (String fileUrl : fileUrls) {
            eventPublisher.publishEvent(new FileDeleteEvent(fileUrl));
        }
    }
}

// Repository에 쿼리 메서드 추가
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    
    // Soft-deleted 게시글 중 특정 날짜 이전 것 조회
    List<Board> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime threshold);
    
    // 일반 사용자는 삭제되지 않은 것만 조회
    List<Board> findByIsDeletedFalse();
}
```

#### 조회 쿼리 수정

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    
    /**
     * 일반 사용자: 삭제되지 않은 게시글만 조회
     */
    public List<BoardResponse> getBoards() {
        return boardRepository.findByIsDeletedFalse().stream()
            .map(BoardResponse::from)
            .collect(Collectors.toList());
    }
    
    /**
     * 관리자: 삭제된 게시글도 포함하여 조회
     */
    public List<BoardResponse> getAllBoardsIncludingDeleted() {
        return boardRepository.findAll().stream()
            .map(BoardResponse::from)
            .collect(Collectors.toList());
    }
    
    /**
     * 휴지통: soft-deleted 게시글만 조회 (복구용)
     */
    public List<BoardResponse> getDeletedBoards() {
        return boardRepository.findByIsDeletedTrue().stream()
            .map(BoardResponse::from)
            .collect(Collectors.toList());
    }
}
```

#### Soft Delete vs Hard Delete 비교

| 항목 | Soft Delete | Hard Delete |
|------|------------|-------------|
| 실행 주체 | 사용자/관리자 | 배치 작업만 |
| DB 처리 | `is_deleted=true` 설정 | 레코드 완전 삭제 |
| S3 파일 | 유지 (복구 가능) | 영구 삭제 |
| 복구 가능 | ✅ 가능 | ❌ 불가능 |
| 실행 시점 | 즉시 | 복구 기간 후 배치 |

#### 설정 파일

`application.properties`
```properties
# 게시글 복구 기간 (일)
app.board.recovery-period-days=30

# 강좌 복구 기간 (일)
app.course.recovery-period-days=90

# 아카데미 복구 기간 (일)
app.academy.recovery-period-days=60
```

#### 컨트롤러 예시

```java
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;
    
    /**
     * Soft Delete
     */
    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long boardId) {
        boardService.softDeleteBoard(boardId);
        return ResponseEntity.ok(Map.of("message", "게시글이 삭제되었습니다. 30일 이내 복구 가능합니다."));
    }
    
    /**
     * 복구
     */
    @PostMapping("/{boardId}/restore")
    public ResponseEntity<?> restoreBoard(@PathVariable Long boardId) {
        boardService.restoreBoard(boardId);
        return ResponseEntity.ok(Map.of("message", "게시글이 복구되었습니다."));
    }
    
    /**
     * 휴지통 조회
     */
    @GetMapping("/deleted")
    public ResponseEntity<List<BoardResponse>> getDeletedBoards() {
        return ResponseEntity.ok(boardService.getDeletedBoards());
    }
}
```

### 배치 작업 실행 순서 및 전략

⚠️ **중요**: Soft-delete 정책 하에서는 배치 작업 실행 순서가 중요합니다.

```
1. 새벽 2시: Hard-Delete 배치 (복구 기간 지난 soft-deleted 게시글 영구 삭제)
   - DB에서 레코드 삭제
   - S3 파일 삭제 이벤트 발행

2. 새벽 3시: 고아 파일 정리 배치 (DB에 없는 S3 파일 정리)
   - Hard-delete된 파일 중 삭제 누락된 것 정리
   - ⚠️ Soft-deleted 게시글의 파일은 보존 (DB에 기록 있음)
```

### 배치 작업: 고아 파일 정리 (선택 사항)

트랜잭션 이벤트를 사용하더라도 완벽하지 않을 수 있으므로, 주기적으로 고아 파일을 정리하는 배치 작업 구현 권장

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrphanFileCleanupScheduler {
    
    private final S3Service s3Service;
    private final BoardAttachRepository boardAttachRepository;
    private final CourseImageRepository courseImageRepository;
    // ... 기타 파일 리포지토리
    
    /**
     * 매일 새벽 3시: 고아 파일 정리 (hard-delete 배치 이후 실행)
     * ⚠️ 주의: Soft-deleted 게시글의 파일은 삭제하지 않음
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOrphanFiles() {
        log.info("Starting orphan file cleanup batch job");
        
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);  // 7일 이상 된 파일만 삭제
        
        cleanupBoardOrphanFiles(threshold);
        cleanupCourseOrphanFiles(threshold);
        // ... 기타 도메인
        
        log.info("Orphan file cleanup batch job completed");
    }
    
    private void cleanupBoardOrphanFiles(LocalDateTime threshold) {
        try {
            // 1. S3에서 board/ 폴더의 모든 파일 목록 조회
            List<String> s3FileUrls = s3Service.listFiles("board");
            
            // 2. DB에서 기록된 파일 URL 조회
            // ⚠️ 중요: soft-deleted 게시글의 파일도 포함해야 복구 가능
            // findAll()은 soft-deleted 포함하여 모든 첨부파일 조회
            List<String> dbFileUrls = boardAttachRepository.findAll().stream()
                .map(BoardAttach::getFileUrl)
                .collect(Collectors.toList());
            
            // 3. S3에만 있고 DB에 없는 파일 찾기
            Set<String> dbFileUrlSet = new HashSet<>(dbFileUrls);
            List<String> orphanFiles = s3FileUrls.stream()
                .filter(url -> !dbFileUrlSet.contains(url))
                .collect(Collectors.toList());
            
            log.info("Found {} potential orphan files in board/ folder", orphanFiles.size());
            
            // 4. 고아 파일 삭제 (7일 이상 된 것만)
            int deletedCount = 0;
            for (String orphanUrl : orphanFiles) {
                if (isOlderThan(orphanUrl, threshold)) {
                    try {
                        s3Service.deleteFile(orphanUrl);
                        deletedCount++;
                        log.info("Deleted orphan file: {}", orphanUrl);
                    } catch (Exception e) {
                        log.warn("Failed to delete orphan file: {}", orphanUrl, e);
                    }
                }
            }
            
            log.info("Deleted {} orphan files from board/ folder", deletedCount);
            
        } catch (Exception e) {
            log.error("Error during board orphan file cleanup", e);
        }
    }
    
    private void cleanupCourseOrphanFiles(LocalDateTime threshold) {
        // 강좌 도메인 고아 파일 정리 (board와 동일한 로직)
        // ⚠️ 주의: soft-deleted 강좌의 파일도 유지해야 함
    }
    
    private boolean isOlderThan(String fileUrl, LocalDateTime threshold) {
        // S3 파일의 생성 날짜를 확인하여 threshold보다 오래된지 체크
        // 방법 1: 파일명에 타임스탬프 포함 (UUID v7 사용 시)
        // 방법 2: S3 메타데이터 조회
        try {
            String key = extractKeyFromUrl(fileUrl);
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            HeadObjectResponse response = s3Client.headObject(headRequest);
            Instant lastModified = response.lastModified();
            
            return lastModified.isBefore(threshold.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.warn("Failed to get file metadata: {}", fileUrl, e);
            return false;  // 메타데이터 조회 실패 시 삭제하지 않음 (안전)
        }
    }
    
    private String extractKeyFromUrl(String fileUrl) {
        // URL에서 S3 키 추출: https://bucket.s3.region.amazonaws.com/key
        return fileUrl.substring(fileUrl.indexOf(".com/") + 5);
    }
}
```

**S3Service에 파일 목록 조회 메서드 추가:**

```java
@Service
public class S3Service {
    
    // ...existing code...
    
    /**
     * 특정 폴더의 모든 파일 URL 목록 조회
     * @param folder S3 폴더 (board, course 등)
     * @return 파일 URL 목록
     */
    public List<String> listFiles(String folder) {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(folder + "/")
                .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            
            return response.contents().stream()
                .map(S3Object::key)
                .map(this::getFileUrl)
                .collect(Collectors.toList());
                
        } catch (S3Exception e) {
            log.error("Failed to list S3 files in folder: {}", folder, e);
            throw new S3UploadException("S3 파일 목록 조회에 실패했습니다.", e);
        }
    }
}
```

#### 파일 업데이트 (기존 파일 삭제 후 새 파일 업로드)

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final ApplicationEventPublisher eventPublisher;
    private final BoardRepository boardRepository;
    private final BoardAttachRepository boardAttachRepository;
    
    @Transactional
    public void updateBoardFile(Long boardId, MultipartFile newFile) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new BoardNotFoundException());
        
        // 기존 파일 삭제
        List<BoardAttach> oldAttachments = board.getAttachments();
        for (BoardAttach attach : oldAttachments) {
            // DB에서 삭제
            boardAttachRepository.delete(attach);
            // 트랜잭션 커밋 후 S3 파일 삭제 이벤트 발행
            eventPublisher.publishEvent(new FileDeleteEvent(attach.getFileUrl()));
        }
        
        // 새 파일 업로드
        if (newFile != null && !newFile.isEmpty()) {
            eventPublisher.publishEvent(
                new FileUploadEvent(boardId, newFile, "board", "BOARD")
            );
        }
    }
}
```

### 권장 구조

```
서비스별 파일 테이블 분리 환경 → 하이브리드 방식 사용

1. S3: 폴더로 도메인 구분 (board/, course/, academy/)
2. DB: 각 도메인별 파일 테이블 (board_attach, course_image 등)
3. 트랜잭션: 이벤트 기반 처리로 고아 파일 방지
4. 배치: 주기적으로 DB에 없는 S3 파일 정리
```

### 배치 작업: 고아 파일 정리 (선택)

```java
@Scheduled(cron = "0 0 3 * * ?")  // 매일 새벽 3시
public void cleanupOrphanFiles() {
    // 1. S3에서 모든 파일 목록 조회
    // 2. DB에서 기록된 파일 URL 조회
    // 3. S3에만 있고 DB에 없는 파일 삭제
    // 4. 일정 기간 이상 된 고아 파일만 삭제
}
```

### 사용 예시 파일

프로젝트에 추가된 파일:
- `service/event/FileUploadEvent.java` - 업로드 이벤트
- `service/event/FileDeleteEvent.java` - 삭제 이벤트
- `service/event/FileEventListener.java` - 이벤트 리스너
- `service/board/BoardFileServiceExample.java` - 사용 예시

각 도메인 서비스에서 위 패턴을 참고하여 구현하면 됩니다!

