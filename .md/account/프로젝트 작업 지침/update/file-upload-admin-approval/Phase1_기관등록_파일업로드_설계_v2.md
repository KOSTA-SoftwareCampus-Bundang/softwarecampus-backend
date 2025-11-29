# 기관 등록 및 파일 업로드 기능 - 설계 문서 v2

## 📋 개요

**목적:** 기관 회원가입 시 신규 기관 등록 신청 및 재직증명서 파일 업로드 지원  
**배경:** 사용자가 소속 기관이 DB에 없을 경우, 직접 기관 등록 신청 → 관리자 승인 → 회원가입 진행  
**방식:** Multipart/form-data를 통한 파일 업로드 및 AWS S3 저장  
**저장소:** AWS S3 (`s3://bucket-name/academy/{academyId}/` 경로)  
**메타데이터:** DB (academy_files 테이블에 S3 URL 및 키 저장)  
**S3 서비스:** 기존 구현된 S3Service 재사용

---

## 🎯 사용자 시나리오

### **시나리오 1: 기존 기관 선택 회원가입**

```
1. 사용자가 회원가입 페이지 접속
2. DB에 이미 등록된 기관 목록 조회 (APPROVED 상태만)
3. 본인 소속 기관 선택 + 회원정보 입력
4. 회원가입 신청 → Account 생성 (accountApproved = PENDING)
5. 관리자 승인 대기 (1~2영업일)
6. 관리자가 Account 승인
   → 이메일 발송: "회원가입이 승인되었습니다" (회원 이메일로)
7. 회원가입 완료 (accountApproved = APPROVED)
```

### **시나리오 2: 신규 기관 등록 + 회원가입 (2단계 프로세스)**

#### **[1단계: 기관 등록 신청]**

```
1. 사용자가 회원가입 페이지 접속
2. "소속 기관이 목록에 없습니다" 선택
3. 기관 등록 페이지로 이동
4. 기관 정보 입력
   - 기관명 (name)
   - 사업자등록번호 (businessNumber)
   - 주소 (address)
   - 이메일 (email) ← 승인 알림을 받을 이메일
5. 📎 재직증명서 파일 업로드 (필수, 최소 1개)
   - 허용 파일: PDF, JPG, JPEG, PNG, DOC, DOCX
   - 최대 크기: 10MB/파일
   - 예시: 사업자등록증, 교육기관 인증서, 재직증명서 등
6. 기관 등록 신청 → Academy 생성 (isApproved = PENDING)
7. 화면 안내 메시지:
   "등록 요청은 관리자 승인 후 처리됩니다. 
    승인까지 1~2영업일이 소요될 수 있습니다.
    승인 완료 시 등록하신 이메일({email})로 알림이 발송됩니다."
```

#### **[관리자: 기관 승인]**

```
8. 관리자가 관리자 페이지에서 대기 중인 Academy 조회
9. 첨부파일(재직증명서) 다운로드 및 검토
10. 승인 또는 거절 처리
    - 승인: PATCH /academies/{id}/approve
      → isApproved = APPROVED
      → 이메일 발송: "기관 등록이 승인되었습니다. 이제 회원가입을 진행해주세요." (기관 이메일로)
    - 거절: PATCH /academies/{id}/reject
      → isApproved = REJECTED
      → 이메일 발송: "기관 등록이 거절되었습니다. 사유: {reason}" (기관 이메일로)
```

#### **[2단계: 회원가입]**

```
11. 사용자가 승인 완료 이메일을 받고 다시 회원가입 페이지 접속
12. 이제 승인된 본인 기관이 목록에 표시됨
13. 해당 기관 선택 + 회원정보 입력
14. 회원가입 신청 → Account 생성 (accountApproved = PENDING)
15. 관리자 승인 대기 (1~2영업일)
16. 관리자가 Account 승인
    → 이메일 발송: "회원가입이 승인되었습니다" (회원 이메일로)
17. 회원가입 완료 (accountApproved = APPROVED)
```

---

## 🏗️ 아키텍처 설계

### 1. 데이터 모델

#### Academy 엔티티 (수정)
```java
@Entity
@Table(name = "academy")
public class Academy extends BaseSoftDeleteSupportEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String address;
    
    @Column(nullable = false, unique = true)
    private String businessNumber;
    
    @Column(nullable = false)
    private String email;  // 기관 담당자 이메일 (승인 알림용)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "is_approved", nullable = false)
    private ApprovalStatus isApproved;  // PENDING, APPROVED, REJECTED
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;  // 거절 사유
    
    // 기관 첨부파일 (재직증명서)
    @OneToMany(mappedBy = "academy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AcademyFile> files = new ArrayList<>();
    
    // 승인 메서드
    public void approve() {
        this.isApproved = ApprovalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }
    
    // 거절 메서드
    public void reject(String reason) {
        this.isApproved = ApprovalStatus.REJECTED;
        this.approvedAt = null;
        this.rejectionReason = reason;
    }
}
```

#### AcademyFile 엔티티 (신규)
```java
@Entity
@Table(name = "academy_files")
@EntityListeners(AuditingEntityListener.class)
public class AcademyFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;
    
    @Column(nullable = false, length = 255)
    private String originalFileName;  // 원본 파일명
    
    @Column(nullable = false, length = 1000)
    private String fileUrl;           // S3 파일 URL
    
    @Column(nullable = false, length = 500)
    private String s3Key;             // S3 객체 키
    
    @Column(nullable = false)
    private Long fileSize;            // 파일 크기 (bytes)
    
    @Column(nullable = false, length = 100)
    private String contentType;       // MIME 타입
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
```

---

### 2. DTO 설계

#### AcademyCreateRequest (수정)
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademyCreateRequest {
    
    @NotBlank(message = "기관명은 필수입니다")
    private String name;
    
    @NotBlank(message = "주소는 필수입니다")
    private String address;
    
    @NotBlank(message = "사업자등록번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", 
             message = "사업자등록번호 형식이 올바르지 않습니다 (예: 123-45-67890)")
    private String businessNumber;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String email;  // 승인 알림을 받을 이메일
    
    // 재직증명서 파일 (최소 1개 필수)
    @NotEmpty(message = "재직증명서 파일은 최소 1개 이상 필요합니다")
    private List<MultipartFile> files;
}
```

#### AcademyResponse (수정)
```java
@Getter
@Builder
public class AcademyResponse {
    private Long id;
    private String name;
    private String address;
    private String businessNumber;
    private String email;
    private ApprovalStatus isApproved;
    private LocalDateTime approvedAt;
    private String rejectionReason;  // 거절 사유 (REJECTED 상태일 때)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 첨부파일 정보
    private List<FileInfo> attachedFiles;
    
    @Getter
    @Builder
    public static class FileInfo {
        private Long id;
        private String originalFileName;
        private String downloadUrl;  // Presigned URL 또는 다운로드 엔드포인트
        private Long fileSize;
        private String contentType;
        private LocalDateTime uploadedAt;
    }
}
```

#### AcademyRejectRequest (신규)
```java
@Getter
@Setter
public class AcademyRejectRequest {
    
    @NotBlank(message = "거절 사유는 필수입니다")
    @Size(max = 500, message = "거절 사유는 500자 이내로 입력해주세요")
    private String reason;
}
```

---

## 📝 API 명세

### 1. 기관 등록 신청 (일반 사용자)

**Endpoint:** `POST /academies`  
**권한:** 인증 불필요 (누구나 신청 가능)  
**Content-Type:** `multipart/form-data`

**Request (Form-data):**
```
name: 코스타 소프트웨어 아카데미
address: 경기도 성남시 분당구
businessNumber: 123-45-67890
email: contact@academy.com
files: [business-registration.pdf, certificate.jpg]
```

**Response (201 Created):**
```json
{
  "id": 5,
  "name": "코스타 소프트웨어 아카데미",
  "address": "경기도 성남시 분당구",
  "businessNumber": "123-45-67890",
  "email": "contact@academy.com",
  "isApproved": "PENDING",
  "approvedAt": null,
  "rejectionReason": null,
  "createdAt": "2025-11-28T10:00:00",
  "updatedAt": "2025-11-28T10:00:00",
  "attachedFiles": [
    {
      "id": 1,
      "originalFileName": "business-registration.pdf",
      "downloadUrl": "/academies/5/files/1",
      "fileSize": 1048576,
      "contentType": "application/pdf",
      "uploadedAt": "2025-11-28T10:00:00"
    }
  ]
}
```

**Error Cases:**
- 400: 파일 형식 불일치 (허용: PDF, JPG, JPEG, PNG, DOC, DOCX)
- 400: 첨부파일 없음
- 400: 사업자등록번호 중복
- 413: 파일 크기 초과 (최대 10MB)

---

### 2. 기관 목록 조회 (승인된 기관만)

**Endpoint:** `GET /academies?approved=true`  
**권한:** 인증 불필요

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "코스타 소프트웨어 아카데미",
    "address": "경기도 성남시 분당구",
    "businessNumber": "123-45-67890",
    "email": "contact@academy.com",
    "isApproved": "APPROVED",
    "approvedAt": "2025-11-27T14:30:00",
    "attachedFiles": []
  }
]
```

---

### 3. 기관 승인 (관리자 전용)

**Endpoint:** `PATCH /academies/{academyId}/approve`  
**권한:** ADMIN

**Response (200 OK):**
```json
{
  "id": 5,
  "name": "코스타 소프트웨어 아카데미",
  "isApproved": "APPROVED",
  "approvedAt": "2025-11-28T15:00:00"
}
```

**승인 시 이메일 발송:**
```
받는 사람: contact@academy.com (기관 등록 시 입력한 이메일)
제목: [코스타] 기관 등록이 승인되었습니다
내용:
안녕하세요,

코스타 소프트웨어 아카데미 기관 등록이 승인되었습니다.
이제 회원가입을 진행하실 수 있습니다.

회원가입 링크: https://example.com/signup

감사합니다.
```

---

### 4. 기관 거절 (관리자 전용)

**Endpoint:** `PATCH /academies/{academyId}/reject`  
**권한:** ADMIN

**Request Body:**
```json
{
  "reason": "제출하신 사업자등록증이 유효하지 않습니다. 재등록 부탁드립니다."
}
```

**Response (200 OK):**
```json
{
  "id": 5,
  "name": "코스타 소프트웨어 아카데미",
  "isApproved": "REJECTED",
  "approvedAt": null,
  "rejectionReason": "제출하신 사업자등록증이 유효하지 않습니다. 재등록 부탁드립니다."
}
```

**거절 시 이메일 발송:**
```
받는 사람: contact@academy.com
제목: [코스타] 기관 등록이 거절되었습니다
내용:
안녕하세요,

코스타 소프트웨어 아카데미 기관 등록이 거절되었습니다.

거절 사유:
제출하신 사업자등록증이 유효하지 않습니다. 재등록 부탁드립니다.

수정 후 다시 신청해주시기 바랍니다.

감사합니다.
```

---

### 5. 첨부파일 다운로드 (관리자 전용)

**Endpoint:** `GET /academies/{academyId}/files/{fileId}`  
**권한:** ADMIN (관리자만 재직증명서 열람 가능)

**Response:**
- **302 Redirect** to S3 Presigned URL (1시간 유효)

---

### 6. 회원 승인 (관리자 전용)

**Endpoint:** `PATCH /accounts/{accountId}/approve`  
**권한:** ADMIN

**Response (200 OK):**
```json
{
  "id": 10,
  "email": "user@example.com",
  "userName": "홍길동",
  "accountType": "ACADEMY",
  "accountApproved": "APPROVED"
}
```

**승인 시 이메일 발송:**
```
받는 사람: user@example.com (회원가입 시 입력한 이메일)
제목: [코스타] 회원가입이 승인되었습니다
내용:
안녕하세요, 홍길동님

코스타 소프트웨어 아카데미 회원가입이 승인되었습니다.
이제 로그인하여 서비스를 이용하실 수 있습니다.

로그인 링크: https://example.com/login

감사합니다.
```

---

## 🛠️ 구현 레이어

### 1. Controller Layer

#### AcademyController
```java
@RestController
@RequestMapping("/academies")
@RequiredArgsConstructor
public class AcademyController {

    private final AcademyService academyService;
    private final AcademyFileService academyFileService;

    /**
     * 기관 등록 신청 (일반 사용자)
     * 파일 업로드 포함
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AcademyResponse> createAcademy(
            @Valid @ModelAttribute AcademyCreateRequest request) {
        AcademyResponse response = academyService.createAcademy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 승인된 기관 목록 조회 (회원가입용)
     */
    @GetMapping
    public ResponseEntity<List<AcademyResponse>> getApprovedAcademies(
            @RequestParam(defaultValue = "true") boolean approved) {
        List<AcademyResponse> academies = academyService.getAcademiesByApprovalStatus(
            approved ? ApprovalStatus.APPROVED : null
        );
        return ResponseEntity.ok(academies);
    }
    
    /**
     * 기관 상세 조회
     */
    @GetMapping("/{academyId}")
    public ResponseEntity<AcademyResponse> getAcademyDetails(@PathVariable Long academyId) {
        AcademyResponse response = academyService.getAcademyDetails(academyId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 기관 승인 (관리자 전용)
     */
    @PatchMapping("/{academyId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademyResponse> approveAcademy(@PathVariable Long academyId) {
        AcademyResponse response = academyService.approveAcademy(academyId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 기관 거절 (관리자 전용)
     */
    @PatchMapping("/{academyId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AcademyResponse> rejectAcademy(
            @PathVariable Long academyId,
            @Valid @RequestBody AcademyRejectRequest request) {
        AcademyResponse response = academyService.rejectAcademy(academyId, request.getReason());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 첨부파일 다운로드 (관리자 전용)
     */
    @GetMapping("/{academyId}/files/{fileId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> downloadFile(
            @PathVariable Long academyId,
            @PathVariable Long fileId) {
        String presignedUrl = academyFileService.getFileUrl(fileId);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(presignedUrl))
            .build();
    }
}
```

#### AccountController (수정)
```java
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    
    /**
     * 회원 승인 (관리자 전용)
     * 승인 시 이메일 발송
     */
    @PatchMapping("/{accountId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> approveAccount(@PathVariable Long accountId) {
        AccountResponse response = accountService.approveAccount(accountId);
        return ResponseEntity.ok(response);
    }
}
```

---

### 2. Service Layer

#### AcademyServiceImpl (수정)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademyServiceImpl implements AcademyService {
    
    private final AcademyRepository academyRepository;
    private final AcademyFileService academyFileService;
    private final EmailService emailService;  // 이메일 발송 서비스
    
    /**
     * 기관 등록 신청 (파일 업로드 포함)
     * 작성자: [Your Name]
     * 작성일: 2025-11-28
     */
    @Override
    @Transactional
    public AcademyResponse createAcademy(AcademyCreateRequest request) {
        // 1. Academy 엔티티 생성 및 저장
        Academy academy = Academy.builder()
            .name(request.getName())
            .address(request.getAddress())
            .businessNumber(request.getBusinessNumber())
            .email(request.getEmail())
            .isApproved(ApprovalStatus.PENDING)
            .build();
        
        Academy savedAcademy = academyRepository.save(academy);
        
        // 2. 파일 업로드 (S3)
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            for (MultipartFile file : request.getFiles()) {
                academyFileService.uploadFile(file, savedAcademy.getId());
            }
        }
        
        // 3. 응답 생성
        return AcademyResponse.from(savedAcademy);
    }
    
    /**
     * 기관 승인 (관리자)
     * 승인 시 이메일 발송
     * 작성자: [Your Name]
     * 작성일: 2025-11-28
     */
    @Override
    @Transactional
    public AcademyResponse approveAcademy(Long academyId) {
        Academy academy = findAcademyOrThrow(academyId);
        academy.approve();
        
        // 승인 완료 이메일 발송
        emailService.sendAcademyApprovalEmail(
            academy.getEmail(),
            academy.getName()
        );
        
        return AcademyResponse.from(academy);
    }
    
    /**
     * 기관 거절 (관리자)
     * 거절 시 이메일 발송
     * 작성자: [Your Name]
     * 작성일: 2025-11-28
     */
    @Override
    @Transactional
    public AcademyResponse rejectAcademy(Long academyId, String reason) {
        Academy academy = findAcademyOrThrow(academyId);
        academy.reject(reason);
        
        // 거절 이메일 발송
        emailService.sendAcademyRejectionEmail(
            academy.getEmail(),
            academy.getName(),
            reason
        );
        
        return AcademyResponse.from(academy);
    }
    
    /**
     * 승인된 기관 목록 조회
     */
    @Override
    public List<AcademyResponse> getAcademiesByApprovalStatus(ApprovalStatus status) {
        List<Academy> academies = status != null 
            ? academyRepository.findByIsApproved(status)
            : academyRepository.findAll();
        
        return academies.stream()
            .map(AcademyResponse::from)
            .collect(Collectors.toList());
    }
    
    private Academy findAcademyOrThrow(Long id) {
        return academyRepository.findById(id)
            .orElseThrow(() -> new AcademyException(AcademyErrorCode.ACADEMY_NOT_FOUND));
    }
}
```

#### AccountServiceImpl (수정)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {
    
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    
    /**
     * 회원 승인 (관리자)
     * 승인 시 이메일 발송
     * 작성자: [Your Name]
     * 작성일: 2025-11-28
     */
    @Override
    @Transactional
    public AccountResponse approveAccount(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        account.setAccountApproved(ApprovalStatus.APPROVED);
        
        // 승인 완료 이메일 발송
        emailService.sendAccountApprovalEmail(
            account.getEmail(),
            account.getUserName()
        );
        
        return AccountResponse.from(account);
    }
}
```

#### EmailService (신규 메서드 추가)
```java
public interface EmailService {
    
    /**
     * 기관 승인 완료 이메일 발송
     * 작성자: [Your Name]
     * 작성일: 2025-11-28
     */
    void sendAcademyApprovalEmail(String toEmail, String academyName);
    
    /**
     * 기관 거절 이메일 발송
     * 작성자: [Your Name]
     * 작성일: 2025-11-28
     */
    void sendAcademyRejectionEmail(String toEmail, String academyName, String reason);
    
    /**
     * 회원 승인 완료 이메일 발송
     * 작성자: [Your Name]
     * 작성일: 2025-11-28
     */
    void sendAccountApprovalEmail(String toEmail, String userName);
}
```

---

## 🔐 보안 고려사항

### 1. 파일 업로드 보안
- **확장자 화이트리스트:** PDF, JPG, JPEG, PNG, DOC, DOCX만 허용
- **MIME 타입 검증:** Content-Type 헤더 확인
- **파일 크기 제한:** 최대 10MB
- **파일명 새니타이징:** UUID 기반 파일명 생성으로 경로 조작 방지

### 2. S3 보안
- **Private 버킷:** 퍼블릭 접근 차단
- **Presigned URL:** 임시 접근 권한 (1시간 유효)
- **관리자 전용:** 첨부파일 다운로드는 ADMIN만 가능

### 3. 접근 제어
- **기관 등록:** 인증 불필요 (누구나 신청 가능)
- **기관 승인/거절:** ADMIN만 가능
- **회원 승인:** ADMIN만 가능
- **파일 다운로드:** ADMIN만 가능

### 4. 데이터 검증
- **사업자등록번호 중복 방지:** UNIQUE 제약
- **이메일 형식 검증:** @Email 어노테이션
- **필수 파일 검증:** @NotEmpty 어노테이션

---

## ✅ 테스트 전략

### 1. 단위 테스트

#### AcademyServiceImplTest
```java
@Test
void 기관_등록_성공() {
    // given
    AcademyCreateRequest request = createRequest();
    
    // when
    AcademyResponse response = academyService.createAcademy(request);
    
    // then
    assertThat(response.getIsApproved()).isEqualTo(ApprovalStatus.PENDING);
    assertThat(response.getAttachedFiles()).hasSize(2);
}

@Test
void 기관_승인_성공_이메일_발송() {
    // given
    Academy academy = createPendingAcademy();
    
    // when
    academyService.approveAcademy(academy.getId());
    
    // then
    verify(emailService).sendAcademyApprovalEmail(
        eq(academy.getEmail()),
        eq(academy.getName())
    );
}
```

#### AccountServiceImplTest
```java
@Test
void 회원_승인_성공_이메일_발송() {
    // given
    Account account = createPendingAccount();
    
    // when
    accountService.approveAccount(account.getId());
    
    // then
    verify(emailService).sendAccountApprovalEmail(
        eq(account.getEmail()),
        eq(account.getUserName())
    );
}
```

### 2. 통합 테스트

#### AcademyControllerIntegrationTest
```java
@Test
void 기관_등록_multipart_요청() {
    MockMultipartFile file = new MockMultipartFile(
        "files", "test.pdf", "application/pdf", "content".getBytes()
    );
    
    mockMvc.perform(multipart("/academies")
            .file(file)
            .param("name", "테스트 아카데미")
            .param("businessNumber", "123-45-67890")
            .param("email", "test@academy.com"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.isApproved").value("PENDING"));
}
```

---

## 📅 구현 일정

### Phase 1: 엔티티 및 DTO (0.5일)
- [x] AcademyFile 엔티티 생성
- [x] Academy 엔티티 수정 (rejectionReason 필드 추가)
- [x] AcademyCreateRequest 수정 (files 필드 추가)
- [x] AcademyResponse 수정 (attachedFiles, rejectionReason 추가)
- [x] AcademyRejectRequest 생성

### Phase 2: 파일 업로드 (1일)
- [x] AcademyFileService 구현
- [x] AcademyFileRepository 생성
- [x] S3Service 연동 (FileType.ACADEMY_FILE 추가)

### Phase 3: 승인/거절 로직 (1일)
- [x] AcademyService 수정 (approve, reject 메서드)
- [x] AcademyController에서 AdminController로 이동 (approve, reject 엔드포인트)
- [x] AdminController 추가 (account approve, academy approve/reject 엔드포인트)

### Phase 4: 이메일 발송 (1일)
- [x] EmailSendService 메서드 추가 (3종)
  - sendAcademyApprovalEmail
  - sendAcademyRejectionEmail
  - sendAccountApprovalEmail
- [x] EmailSendServiceImpl 구현 (HTML 인라인 템플릿)
- [x] 이메일 발송 로직 통합

### Phase 5: 테스트 및 검증 (1일)
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] Postman 시나리오 테스트
- [ ] 프론트엔드 연동 테스트

**총 예상 기간:** 4.5일

---

## 📌 프론트엔드 연동 가이드

### 1. 기관 등록 폼
```javascript
// 파일 업로드 포함 FormData 생성
const formData = new FormData();
formData.append('name', '코스타 아카데미');
formData.append('businessNumber', '123-45-67890');
formData.append('address', '경기도 성남시');
formData.append('email', 'contact@academy.com');

// 파일 추가 (다중 파일)
files.forEach(file => {
  formData.append('files', file);
});

// API 호출
const response = await fetch('/academies', {
  method: 'POST',
  body: formData,
  // Content-Type 헤더는 자동 설정됨 (multipart/form-data)
});
```

### 2. 승인된 기관 목록 조회 (회원가입용)
```javascript
const response = await fetch('/academies?approved=true');
const academies = await response.json();
```

### 3. 안내 메시지 표시
```javascript
if (response.status === 201) {
  alert(`
    기관 등록 신청이 완료되었습니다.
    
    등록 요청은 관리자 승인 후 처리됩니다.
    승인까지 1~2영업일이 소요될 수 있습니다.
    승인 완료 시 ${email}로 알림이 발송됩니다.
  `);
}
```

---

## 🎯 핵심 차이점 (v1 대비)

| 항목 | v1 (기존) | v2 (현재) |
|------|----------|----------|
| **기관 등록 주체** | 관리자만 | **일반 사용자도 가능** |
| **파일 업로드 용도** | 불명확 | **재직증명서 (필수)** |
| **승인 프로세스** | 1단계 | **2단계 (기관 → 회원)** |
| **이메일 알림** | 없음 | **3종 추가** |
| **거절 기능** | 없음 | **거절 사유 저장** |
| **파일 접근 권한** | 불명확 | **관리자 전용** |

---

**작성자:** [Your Name]  
**작성일:** 2025년 11월 28일  
**문서 버전:** 2.0  
**변경 이력:**
- v1.0 (2025-11-28): 초기 작성
- v2.0 (2025-11-28): 시나리오 재정의, 이메일 알림 추가, 거절 기능 추가
