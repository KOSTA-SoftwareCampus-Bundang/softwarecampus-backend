# 엔티티 및 레포지토리 설계 (Account 도메인)

> 📌 **작성 범위**: 이 문서는 **Account 도메인(로그인/회원가입/마이페이지/보안)** 담당 부분만 작성되었습니다.

## 1. 도메인 그룹 구조

### 현재 구현된 도메인 (Account 담당 부분)
```
src/main/java/com/softwarecampus/backend/domain/
├── user/
│   └── Account.java              ✅ 구현 완료
└── common/
    ├── BaseTimeEntity.java       ✅ 기존 파일
    ├── BaseSoftDeleteSupportEntity.java  ✅ 기존 파일
    ├── AccountType.java          ✅ 구현 완료
    ├── ApprovalStatus.java       ✅ 구현 완료
    ├── BoardCategory.java        ✅ 구현 완료 (향후 사용)
    └── CourseCategoryType.java   ✅ 구현 완료 (향후 사용)
```

### 전체 프로젝트 도메인 구조 (참고용)
```
src/main/java/com/softwarecampus/backend/domain/
├── user/                         ✅ Account 담당 (완료)
│   └── Account.java
├── academy/                      ⏳ 다른 팀원 담당
│   ├── Academy.java
│   ├── AcademyQuestion.java
│   └── AcademyAnswer.java
├── course/                       ⏳ 다른 팀원 담당
│   ├── Course.java
│   ├── CourseCategory.java
│   ├── CourseReview.java
│   ├── CourseCurriculum.java
│   ├── CourseQuestion.java
│   ├── CourseAnswer.java
│   ├── CourseFavorite.java
│   └── CourseTag.java
├── board/                        ⏳ 다른 팀원 담당
│   ├── Board.java
│   ├── BoardCategory.java
│   ├── Comment.java
│   └── BoardAttach.java
└── common/
    ├── BaseTimeEntity.java
    ├── BaseSoftDeleteSupportEntity.java
    └── File.java
```

## 2. 공통 베이스 엔티티

> ⚠️ **주의**: 아래 베이스 클래스들은 **이미 프로젝트에 존재하는 파일**로, 다른 곳에서 사용 중이므로 **절대 수정하면 안 됩니다**.

### BaseTimeEntity (기존 파일 - 수정 금지)
```java
@MappedSuperclass
@Getter
public abstract class BaseTimeEntity {
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### BaseSoftDeleteSupportEntity (기존 파일 - 수정 금지)
```java
@Getter
@MappedSuperclass
public abstract class BaseSoftDeleteSupportEntity extends BaseTimeEntity {
    @Column(name="is_deleted", nullable = false)
    protected Boolean isDeleted = false;

    @Column(name="deleted_at")
    protected LocalDateTime deletedAt;

    public void markDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    public boolean isActive() {
        return !Boolean.TRUE.equals(this.isDeleted);
    }
}
```

## 3. Account 도메인 상세 설계 (담당 부분)

### Account.java

> ✅ **구현 완료**: SQL 스키마와 100% 일치하는 엔티티 작성 완료

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "account",
    indexes = {
        @Index(name = "idx_account_email", columnList = "email"),
        @Index(name = "idx_account_type_approved", columnList = "account_type,account_approved")
    }
)
public class Account extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;

    private String nickname;
    
    @Column(nullable = false)
    private String password;
    
    private String address;
    private String affiliation;
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_approved")
    private ApprovalStatus accountApproved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id")
    private Academy academy;
    
    // 연관관계 편의 메소드
    public void assignAcademy(Academy academy) {
        this.academy = academy;
    }
}
```

### Account 필드 설명

| 필드명 | 타입 | 설명 | 제약조건 |
|--------|------|------|----------|
| id | Long | 계정 고유 ID | PK, AUTO_INCREMENT |
| email | String | 이메일 (로그인 ID) | UNIQUE, NOT NULL |
| accountType | AccountType | 계정 유형 (USER/ACADEMY/ADMIN) | ENUM |
| nickname | String | 닉네임 | nullable |
| password | String | 암호화된 비밀번호 | NOT NULL |
| address | String | 주소 | nullable |
| affiliation | String | 소속 | nullable |
| position | String | 직책 | nullable |
| accountApproved | ApprovalStatus | 승인 상태 | ENUM (PENDING/APPROVED/REJECTED) |
| academy | Academy | 소속 학원 (ACADEMY 타입만) | FK, nullable |

### 공통 ENUM 클래스들

#### AccountType.java
```java
package com.softwarecampus.backend.domain.common;

public enum AccountType {
    USER,      // 일반 사용자
    ACADEMY,   // 기관 계정
    ADMIN      // 관리자
}
```

#### ApprovalStatus.java
```java
package com.softwarecampus.backend.domain.common;

public enum ApprovalStatus {
    PENDING,   // 승인 대기
    APPROVED,  // 승인 완료
    REJECTED   // 승인 거부
}
```

#### BoardCategory.java (향후 Board 도메인에서 사용)
```java
package com.softwarecampus.backend.domain.common;

public enum BoardCategory {
    NOTICE,    // 공지사항
    QUESTION,  // 문의사항
    CAREER,    // 진로이야기
    CODING     // 코딩이야기
}
```

#### CourseCategoryType.java (향후 Course 도메인에서 사용)
```java
package com.softwarecampus.backend.domain.common;

public enum CourseCategoryType {
    WORKER,              // 재직자
    JOB_SEEKER          // 취업예정자
}
```

## 4. Account 레포지토리 설계 (담당 부분)

### AccountRepository.java

> ✅ **구현 완료**: 로그인, 회원가입, 관리자 기능을 위한 쿼리 메소드 작성 완료

```java
package com.softwarecampus.backend.repository.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.user.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    // 로그인용: 이메일로 계정 조회
    Optional<Account> findByEmail(String email);
    
    // 회원가입 중복 체크: 이메일 존재 여부
    boolean existsByEmail(String email);
    
    // 승인 대기중인 기관 계정 조회 (관리자용)
    @Query("SELECT a FROM Account a WHERE a.accountType = 'ACADEMY' AND a.accountApproved = 'PENDING' AND a.isDeleted = false")
    List<Account> findPendingAcademyAccounts();
    
    // 타입별 계정 목록 조회 (관리자용)
    List<Account> findByAccountTypeAndIsDeleted(AccountType accountType, Boolean isDeleted);
}
```

### AccountRepository 메소드 설명

| 메소드명 | 반환 타입 | 용도 | 사용 시나리오 |
|----------|-----------|------|---------------|
| `findByEmail(String)` | `Optional<Account>` | 이메일로 계정 조회 | 로그인 시 이메일 검증 |
| `existsByEmail(String)` | `boolean` | 이메일 중복 확인 | 회원가입 시 중복 체크 |
| `findPendingAcademyAccounts()` | `List<Account>` | 승인 대기 기관 목록 | 관리자 승인 페이지 |
| `findByAccountTypeAndIsDeleted()` | `List<Account>` | 타입별 계정 조회 | 관리자 회원 관리 |

---

## 5. 다른 도메인 참고 예시 (Academy, Course, Board)

> ⚠️ **주의**: 아래 내용은 **다른 팀원이 담당하는 도메인**의 예시입니다. 참고용으로만 사용하세요.

<details>
<summary>Academy 도메인 예시 (펼쳐보기)</summary>

#### Academy.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "academy",
    indexes = {
        @Index(name = "idx_academy_approved", columnList = "is_approved,is_deleted"),
        @Index(name = "idx_academy_business_number", columnList = "business_number")
    }
)
public class Academy extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    
    private String address;
    
    @Column(name = "business_number")
    private String businessNumber;
    
    private String phone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_approved", nullable = false)
    private ApprovalStatus isApproved;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "academy")
    private List<Course> courses = new ArrayList<>();
    
    @OneToMany(mappedBy = "academy")
    private List<AcademyQuestion> questions = new ArrayList<>();
    
    // 연관관계 편의 메소드
    public void addCourse(Course course) {
        courses.add(course);
        course.assignAcademy(this);
    }
    
    public void addQuestion(AcademyQuestion question) {
        questions.add(question);
        question.assignAcademy(this);
    }
}
```

#### AcademyQuestion.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "academy_question")
public class AcademyQuestion extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @Column(name = "question_number")
    private String questionNumber;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String text;
    
    @OneToMany(mappedBy = "academyQuestion")
    private List<AcademyAnswer> answers = new ArrayList<>();
    
    // 연관관계 편의 메소드
    public void assignAcademy(Academy academy) {
        this.academy = academy;
    }
    
    public void addAnswer(AcademyAnswer answer) {
        answers.add(answer);
        answer.assignQuestion(this);
    }
}
```

#### AcademyAnswer.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "academy_answer")
public class AcademyAnswer extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_question_id", nullable = false)
    private AcademyQuestion academyQuestion;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_approved")
    private ApprovalStatus isApproved;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    // 연관관계 편의 메소드
    public void assignQuestion(AcademyQuestion question) {
        this.academyQuestion = question;
    }
}
```

</details>

<details>
<summary>Course 도메인 예시 (펼쳐보기)</summary>

#### Course.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course",
    indexes = {
        @Index(name = "idx_course_approved", columnList = "is_approved,is_deleted"),
        @Index(name = "idx_course_academy", columnList = "academy_id"),
        @Index(name = "idx_course_category", columnList = "course_category_id"),
        @Index(name = "idx_course_recruit_dates", columnList = "recrut_start,recrut_end")
    }
)
public class Course extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_category_id", nullable = false)
    private CourseCategory courseCategory;

    @Column(nullable = false)
    private String name;
    
    @Column(name = "recrut_start")
    private LocalDateTime recrutStart;
    
    @Column(name = "recrut_end")
    private LocalDateTime recrutEnd;
    
    @Column(name = "course_start")
    private LocalDateTime courseStart;
    
    @Column(name = "course_end")
    private LocalDateTime courseEnd;
    
    private String cost;
    
    @Column(name = "class_day")
    private String classDay;
    
    private String location;
    
    @Column(name = "is_nailbaeum")
    private boolean isNailbaeum;
    
    @Column(name = "is_kdt")
    private boolean isKdt;
    
    private String requirement;
    
    @Column(name = "is_offline")
    private boolean isOffline;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_approved", nullable = false)
    private ApprovalStatus isApproved;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "course")
    private List<CourseReview> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "course")
    private List<CourseQuestion> questions = new ArrayList<>();
    
    @OneToMany(mappedBy = "course")
    private List<CourseCurriculum> curriculums = new ArrayList<>();
    
    // 연관관계 편의 메소드
    public void assignAcademy(Academy academy) {
        this.academy = academy;
    }
    
    public void assignCategory(CourseCategory category) {
        this.courseCategory = category;
    }
    
    public void addReview(CourseReview review) {
        reviews.add(review);
        review.assignCourse(this);
    }
    
    public void addQuestion(CourseQuestion question) {
        questions.add(question);
        question.assignCourse(this);
    }
}
```

#### CourseCategory.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_category")
public class CourseCategory extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name")
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    private CourseCategoryType categoryType; // 재직자, 취업예정자

    @OneToMany(mappedBy = "courseCategory")
    private List<Course> courses = new ArrayList<>();
}
```

#### CourseReview.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_review")
public class CourseReview extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private String title;
    
    private Integer section1Point;
    @Column(name = "section1_text", columnDefinition = "TEXT")
    private String section1Text;
    
    private Integer section2Point;
    @Column(name = "section2_text", columnDefinition = "TEXT")
    private String section2Text;
    
    private Integer section3Point;
    @Column(name = "section3_text", columnDefinition = "TEXT")
    private String section3Text;
    
    private Integer section4Point;
    @Column(name = "section4_text", columnDefinition = "TEXT")
    private String section4Text;
    
    private Integer section5Point;
    @Column(name = "section5_text", columnDefinition = "TEXT")
    private String section5Text;
    
    private Integer section6Point;
    @Column(name = "section6_text", columnDefinition = "TEXT")
    private String section6Text;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_review_approved")
    private ApprovalStatus courseReviewApproved;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    // 연관관계 편의 메소드
    public void assignCourse(Course course) {
        this.course = course;
    }
}
```

#### CourseQuestion.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_question")
public class CourseQuestion extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "question_number")
    private String questionNumber;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String text;
    
    @OneToMany(mappedBy = "courseQuestion")
    private List<CourseAnswer> answers = new ArrayList<>();
    
    // 연관관계 편의 메소드
    public void assignCourse(Course course) {
        this.course = course;
    }
    
    public void addAnswer(CourseAnswer answer) {
        answers.add(answer);
        answer.assignQuestion(this);
    }
}
```

#### CourseAnswer.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_answer")
public class CourseAnswer extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_question_id", nullable = false)
    private CourseQuestion courseQuestion;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_approved")
    private ApprovalStatus isApproved;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    // 연관관계 편의 메소드
    public void assignQuestion(CourseQuestion question) {
        this.courseQuestion = question;
    }
}
```

#### CourseCurriculum.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_curriculum")
public class CourseCurriculum {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;
    
    @Column(name = "chapter_name")
    private String chapterName;
    
    @Column(name = "chapter_detail", columnDefinition = "TEXT")
    private String chapterDetail;
    
    @Column(name = "chapter_time")
    private Integer chapterTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_tag_id")
    private CourseTag courseTag;
    
    // 연관관계 편의 메소드
    public void assignCourse(Course course) {
        this.course = course;
    }
}
```

#### CourseFavorite.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_favorite")
public class CourseFavorite extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
```

#### CourseTag.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "course_tag")
public class CourseTag {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;
}
```

</details>

<details>
<summary>Board 도메인 예시 (펼쳐보기)</summary>

#### Board.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "board",
    indexes = {
        @Index(name = "idx_board_category", columnList = "category,is_deleted"),
        @Index(name = "idx_board_account", columnList = "account_id"),
        @Index(name = "idx_board_created", columnList = "created_at")
    }
)
public class Board extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardCategory category;

    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;
    
    @Column(nullable = false)
    private int hit = 0;
    
    @Column(nullable = false)
    private int recommend = 0;

    @OneToMany(mappedBy = "board")
    private List<Comment> comments = new ArrayList<>();
    
    @OneToMany(mappedBy = "board")
    private List<BoardAttach> attachments = new ArrayList<>();
    
    // 연관관계 편의 메소드
    public void assignAccount(Account account) {
        this.account = account;
    }
    
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.assignBoard(this);
    }
    
    public void increaseHit() {
        this.hit++;
    }
    
    public void increaseRecommend() {
        this.recommend++;
    }
}
```

#### BoardCategory.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "board_category")
public class BoardCategory extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    
    @Column(name = "last_number")
    private Integer lastNumber;
}
```

#### Comment.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "comment")
public class Comment extends BaseSoftDeleteSupportEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment parentComment;

    @Column(columnDefinition = "TEXT")
    private String comment;
    
    private Integer recommend;
    
    @OneToMany(mappedBy = "parentComment")
    private List<Comment> replies = new ArrayList<>();
    
    // 연관관계 편의 메소드
    public void assignBoard(Board board) {
        this.board = board;
    }
    
    public void assignParentComment(Comment parent) {
        this.parentComment = parent;
    }
}
```

#### BoardAttach.java
```java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "board_attach")
public class BoardAttach {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    
    // File 정보는 공통 File 엔티티 참조 또는 직접 필드로 관리
    @Column(name = "file_name")
    private String filename;
    
    @Column(name = "original_filename")
    private String originalFilename;
    
    @Column(name = "file_path")
    private String filePath;
}
```

}
```

</details>

---

## 6. 레포지토리 패키지 구조

### 현재 구현된 구조 (Account 담당)
```
src/main/java/com/softwarecampus/backend/repository/
└── user/
    └── AccountRepository.java  ✅ 구현 완료
```

### 전체 프로젝트 구조 (참고용)
```
src/main/java/com/softwarecampus/backend/repository/
├── user/                       ✅ Account 담당 (완료)
│   └── AccountRepository.java
├── academy/                    ⏳ 다른 팀원 담당
│   ├── AcademyRepository.java
│   ├── AcademyQuestionRepository.java
│   └── AcademyAnswerRepository.java
├── course/                     ⏳ 다른 팀원 담당
│   ├── CourseRepository.java
│   ├── CourseCategoryRepository.java
│   ├── CourseReviewRepository.java
│   ├── CourseQuestionRepository.java
│   ├── CourseAnswerRepository.java
│   ├── CourseCurriculumRepository.java
│   ├── CourseFavoriteRepository.java
│   └── CourseTagRepository.java
└── board/                      ⏳ 다른 팀원 담당
    ├── BoardRepository.java
    ├── BoardCategoryRepository.java
    ├── CommentRepository.java
    └── BoardAttachRepository.java
```

<details>
<summary>다른 도메인 레포지토리 예시 (펼쳐보기)</summary>

#### AcademyRepository.java
```java
@Repository
public interface AcademyRepository extends JpaRepository<Academy, Long> {
    // 승인된 기관 목록 (isDeleted는 Boolean 타입)
    List<Academy> findByIsApprovedAndIsDeletedOrderByCreatedAtDesc(ApprovalStatus approved, Boolean isDeleted);
    
    // 승인 대기중인 기관 목록
    List<Academy> findByIsApprovedAndIsDeleted(ApprovalStatus approved, Boolean isDeleted);
    
    // 사업자번호로 검색
    Optional<Academy> findByBusinessNumberAndIsDeleted(String businessNumber, Boolean isDeleted);
    
    boolean existsByBusinessNumber(String businessNumber);
}
```

#### AcademyQuestionRepository.java
```java
@Repository
public interface AcademyQuestionRepository extends JpaRepository<AcademyQuestion, Long> {
    // 기관별 질문 목록 (isDeleted는 Boolean 타입)
    List<AcademyQuestion> findByAcademyAndIsDeletedOrderByCreatedAtDesc(Academy academy, Boolean isDeleted);
    
    // 페이징 처리
    Page<AcademyQuestion> findByAcademyAndIsDeleted(Academy academy, Boolean isDeleted, Pageable pageable);
}
```

#### CourseRepository.java
```java
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    // 카테고리별 승인된 과정 목록 조회 (isDeleted는 Boolean 타입)
    List<Course> findByCourseCategoryAndIsApprovedAndIsDeletedOrderByCreatedAtDesc(
        CourseCategory category, 
        ApprovalStatus approved,
        Boolean isDeleted
    );
    
    // 기관별 과정 목록 조회
    List<Course> findByAcademyAndIsDeletedOrderByCreatedAtDesc(Academy academy, Boolean isDeleted);
    
    // 승인 상태별 조회
    Page<Course> findByIsApprovedAndIsDeleted(ApprovalStatus approved, Boolean isDeleted, Pageable pageable);
    
    // 커스텀 검색 쿼리
    @Query("SELECT c FROM Course c WHERE " +
           "c.isApproved = 'APPROVED' AND " +
           "c.isDeleted = false AND " +
           "(:category IS NULL OR c.courseCategory = :category) AND " +
           "(:keyword IS NULL OR c.name LIKE %:keyword%)")
    Page<Course> searchCourses(
        @Param("category") CourseCategory category,
        @Param("keyword") String keyword,
        Pageable pageable
    );
    
    // 진행중인 과정 (모집 기간 기준)
    @Query("SELECT c FROM Course c WHERE " +
           "c.isApproved = 'APPROVED' AND " +
           "c.isDeleted = false AND " +
           "c.recrutStart <= :now AND c.recrutEnd >= :now")
    List<Course> findOngoingRecruitment(@Param("now") LocalDateTime now);
}
```

#### CourseReviewRepository.java
```java
@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    // 과정별 승인된 리뷰 목록 (isDeleted는 Boolean 타입)
    List<CourseReview> findByCourseAndCourseReviewApprovedAndIsDeletedOrderByCreatedAtDesc(
        Course course, 
        ApprovalStatus approved,
        Boolean isDeleted
    );
    
    // 과정별 리뷰 페이징
    Page<CourseReview> findByCourseAndIsDeleted(Course course, Boolean isDeleted, Pageable pageable);
}
```

#### BoardRepository.java
```java
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    // 카테고리별 게시글 목록 조회 (isDeleted는 Boolean 타입)
    Page<Board> findByCategoryAndIsDeletedOrderByCreatedAtDesc(
        BoardCategory category,
        Boolean isDeleted,
        Pageable pageable
    );
    
    // 내가 작성한 글 목록
    List<Board> findByAccountAndIsDeletedOrderByCreatedAtDesc(Account account, Boolean isDeleted);
    
    // 제목 또는 내용 검색
    @Query("SELECT b FROM Board b WHERE " +
           "b.isDeleted = false AND " +
           "(:category IS NULL OR b.category = :category) AND " +
           "(b.title LIKE %:keyword% OR b.text LIKE %:keyword%)")
    Page<Board> searchBoards(
        @Param("category") BoardCategory category,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
```

#### CommentRepository.java
```java
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 게시글별 댓글 목록 (최상위 댓글만, isDeleted는 Boolean 타입)
    List<Comment> findByBoardAndParentCommentIsNullAndIsDeletedOrderByCreatedAtAsc(Board board, Boolean isDeleted);
    
    // 대댓글 목록
    List<Comment> findByParentCommentAndIsDeletedOrderByCreatedAtAsc(Comment parentComment, Boolean isDeleted);
}
```

}
```

</details>

---

## 7. 공통 ENUM 클래스 목록

> ✅ **모두 구현 완료**: 현재 사용 중인 Enum과 향후 사용 예정 Enum 모두 작성됨

| Enum 클래스 | 상태 | 위치 | 사용 도메인 |
|-------------|------|------|-------------|
| AccountType | ✅ 구현 완료 | domain.common | Account |
| ApprovalStatus | ✅ 구현 완료 | domain.common | Account, Academy, Course 등 |
| BoardCategory | ✅ 구현 완료 | domain.common | Board (향후) |
| CourseCategoryType | ✅ 구현 완료 | domain.common | Course (향후) |

---

## 8. 연관관계 매핑 주의사항 (Account 도메인 기준)

1. **지연 로딩(LAZY) 사용**
   ```java
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "academy_id")
   private Academy academy;
   ```
   - Account는 Academy와 다대일 관계 (ACADEMY 타입일 경우만 연결)
   - 지연 로딩으로 성능 최적화

2. **연관관계 편의 메소드 구현**
   ```java
   public void assignAcademy(Academy academy) {
       this.academy = academy;
   }
   ```
   - 양방향 관계에서 객체 그래프 탐색 지원

---

## 9. 인덱스 설정 (Account 도메인)

Account 엔티티의 주요 조회 패턴을 고려한 인덱스 설정:

```java
@Table(name = "account",
    indexes = {
        @Index(name = "idx_account_email", columnList = "email"),
        @Index(name = "idx_account_type_approved", columnList = "account_type,account_approved")
    }
)
```

### 인덱스 설명

| 인덱스명 | 컬럼 | 목적 | 사용 쿼리 |
|----------|------|------|-----------|
| `idx_account_email` | email | 로그인 성능 최적화 | `findByEmail()` |
| `idx_account_type_approved` | account_type, account_approved | 관리자 승인 페이지 성능 최적화 | `findPendingAcademyAccounts()` |

<details>
<summary>다른 도메인 인덱스 예시 (펼쳐보기)</summary>

// Academy
@Table(name = "academy",
    indexes = {
        @Index(name = "idx_academy_approved", columnList = "isApproved,isDeleted"),
        @Index(name = "idx_academy_business_number", columnList = "businessNumber")
    }
)

// Course
@Table(name = "course",
    indexes = {
        @Index(name = "idx_course_approved", columnList = "isApproved,isDeleted"),
        @Index(name = "idx_course_academy", columnList = "academy_id"),
        @Index(name = "idx_course_category", columnList = "course_category_id"),
        @Index(name = "idx_course_recruit_dates", columnList = "recrutStart,recrutEnd")
    }
```

</details>

---

## 10. JPA Auditing 설정

> ⚠️ **주의**: 기존 프로젝트는 `@PrePersist`/`@PreUpdate` 방식을 사용하므로 **JPA Auditing 설정은 생략**합니다.

```java
// JpaConfig는 생성하지 않습니다 (기존 방식 사용)
// BaseTimeEntity가 이미 @PrePersist/@PreUpdate로 타임스탬프 관리 중
```

---

## 11. 개발 가이드라인 (Account 도메인 기준)

> ⚠️ **중요**: 부모 클래스(`BaseTimeEntity`, `BaseSoftDeleteSupportEntity`)는 **절대 수정 금지**

### 엔티티 작성 규칙
   - 모든 엔티티는 `BaseTimeEntity` 또는 `BaseSoftDeleteSupportEntity` 상속
   - **Lombok 사용**: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` 추가
   - `@Column(nullable = false)` 적극 활용
   - 열거형은 `@Enumerated(EnumType.STRING)` 사용
   - **DB 컬럼명 규칙**: snake_case 사용 (`@Column(name = "created_at")`)
   - **isDeleted 타입**: `Boolean` (대문자 B) - 부모 클래스 규칙 준수

### 레포지토리 작성 규칙
   - 메소드 명명규칙 준수 (findBy-, countBy- 등)
   - **isDeleted 조회 시**: `findByIsDeleted(Boolean isDeleted)` 형식 사용 (False가 아닌 Boolean 파라미터)
   - 복잡한 쿼리는 `@Query` 사용
   - 페이징이 필요한 경우 `Page<T>` 반환 타입 사용

### 테스트 작성 규칙
   - 레포지토리 테스트는 `@DataJpaTest` 사용
   - 테스트용 더미 데이터는 `@SQL` 스크립트로 관리
   - 트랜잭션 롤백 주의

---

## 12. Account 도메인 구현 체크리스트

### ✅ 완료된 작업
- [x] BaseTimeEntity 확인 (기존 파일)
- [x] BaseSoftDeleteSupportEntity 확인 (기존 파일)
- [x] AccountType Enum 생성
- [x] ApprovalStatus Enum 생성
- [x] BoardCategory Enum 생성 (향후 사용)
- [x] CourseCategoryType Enum 생성 (향후 사용)
- [x] Account 엔티티 작성 (SQL 스키마 일치)
- [x] AccountRepository 작성 (쿼리 메소드 4개)
- [x] 인덱스 설정 (email, account_type+account_approved)
- [x] 연관관계 매핑 (Academy와 다대일)

### ⏳ 다음 단계 작업
- [ ] AccountService 인터페이스 작성
- [ ] AccountServiceImpl 구현 (회원가입/로그인 비즈니스 로직)
- [ ] DTO 작성 (SignupRequest, LoginRequest, LoginResponse, MyPageResponse)
- [ ] AuthController 작성 (회원가입/로그인 API)
- [ ] Spring Security 설정 (JWT 인증)
- [ ] AccountRepository 단위 테스트 (@DataJpaTest)
- [ ] AccountService 통합 테스트

---

## 13. 다음 단계 가이드

### Service Layer 작성 시 참고사항


1. **인터페이스 위치**: `src/main/java/com/softwarecampus/backend/service/user/AccountService.java`
2. **구현체 위치**: `src/main/java/com/softwarecampus/backend/service/user/impl/AccountServiceImpl.java`
3. **주요 메소드**:
   - `signup(SignupRequest)`: 회원가입
   - `login(LoginRequest)`: 로그인 (JWT 토큰 발급)
   - `getMyInfo(Long)`: 마이페이지 정보 조회
   - `updateMyInfo(Long, UpdateRequest)`: 회원정보 수정

### Controller Layer 작성 시 참고사항

1. **위치**: `src/main/java/com/softwarecampus/backend/controller/user/AuthController.java`
2. **엔드포인트 예시**:
   - `POST /api/auth/signup`: 회원가입
   - `POST /api/auth/login`: 로그인
   - `GET /api/auth/me`: 내 정보 조회
   - `PUT /api/auth/me`: 내 정보 수정

### DTO 작성 시 참고사항

1. **위치**: `src/main/java/com/softwarecampus/backend/dto/user/`
2. **패키지 구조**:
   ```
   dto/user/
   ├── request/
   │   ├── SignupRequest.java
   │   ├── LoginRequest.java
   │   └── UpdateMyInfoRequest.java
   └── response/
       ├── LoginResponse.java
       └── MyPageResponse.java
   ```

### 테스트 작성 시 참고사항

1. **Repository 테스트**: `@DataJpaTest` 사용
2. **Service 테스트**: `@SpringBootTest` + `@Transactional`
3. **Controller 테스트**: `@WebMvcTest` + MockMvc

---

## 14. 참고 자료

- **SQL 스키마**: `ninja gaiden/softcampus.sql` - account 테이블 (line 158-188)
- **JPA 가이드라인**: `ninja gaiden/설계md파일/코드작업 지침/JPA_GUIDELINE.md`
- **API 가이드라인**: `ninja gaiden/설계md파일/코드작업 지침/API_GUIDELINES.md`
- **구현 가이드**: `ninja gaiden/설계md파일/코드작업 지침/implementation_guide.md`