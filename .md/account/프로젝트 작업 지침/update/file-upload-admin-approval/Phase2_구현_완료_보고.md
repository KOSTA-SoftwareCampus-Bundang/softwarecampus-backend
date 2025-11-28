# 기관 등록 및 파일 업로드 기능 구현 완료 보고 (Implementation Report)

## 📋 1. 개요

**목적:** 기관 회원가입 시 신규 기관 등록 신청 및 재직증명서 파일 업로드 지원  
**구현 기간:** 2025년 11월 28일  
**작성자:** GitHub Copilot  
**구현 범위:** Backend API (Spring Boot 3.5.6)

---

## ✅ 2. 구현 완료 사항

### 2.1 엔티티 수정

#### `Academy.java`
기관 거절 사유를 저장하기 위한 필드 추가 및 거절 메서드 수정

| 추가/수정 항목 | 타입 | 설명 | 비고 |
|---------------|------|------|------|
| `rejectionReason` | `String` | 거절 사유 | VARCHAR(500) |
| `reject(String reason)` | Method | 거절 처리 메서드 | 사유를 파라미터로 받도록 수정 |

**변경 코드:**
```java
@Column(name = "rejection_reason", length = 500)
private String rejectionReason;

public void reject(String reason) {
    this.isApproved = ApprovalStatus.REJECTED;
    this.approvedAt = null;
    this.rejectionReason = reason;
}
```

---

### 2.2 DTO 추가

#### `AcademyRejectRequest.java` (신규)
관리자가 기관 등록을 거절할 때 사유를 입력받는 DTO

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

### 2.3 Service Layer 수정

#### `AcademyService.java`
인터페이스에 `rejectAcademy` 메서드 시그니처 추가

```java
AcademyResponse rejectAcademy(Long id, String reason);
```

#### `AcademyServiceImpl.java`
파일 업로드 및 이메일 발송 로직 통합

**주요 변경 사항:**

1. **의존성 주입**
   - `AcademyFileService`: 파일 업로드/다운로드 처리
   - `EmailSendService`: 이메일 발송

2. **`createAcademy()` 메서드 수정**
   - 기관 생성 후 첨부파일(재직증명서) S3 업로드
   ```java
   for (var file : request.getFiles()) {
       academyFileService.uploadFile(file, savedAcademy.getId());
   }
   ```

3. **`approveAcademy()` 메서드 수정**
   - 승인 처리 후 기관 담당자 이메일로 승인 알림 발송
   ```java
   emailSendService.sendAcademyApprovalEmail(
       academy.getEmail(), 
       academy.getName()
   );
   ```

4. **`rejectAcademy()` 메서드 구현**
   - 거절 처리 후 거절 사유 포함 이메일 발송
   ```java
   public AcademyResponse rejectAcademy(Long id, String reason) {
       Academy academy = findAcademyOrThrow(id);
       academy.reject(reason);
       
       emailSendService.sendAcademyRejectionEmail(
           academy.getEmail(),
           academy.getName(),
           reason
       );
       
       return AcademyResponse.from(academy);
   }
   ```

---

### 2.4 Controller Layer 수정

#### `AcademyController.java` → `AdminController.java` 이동

**관리자 전용 기능을 `/admin` prefix로 통일하기 위해 다음 엔드포인트를 `AdminController`로 이동:**

- ❌ `PATCH /academies/{id}/approve` (삭제)
- ❌ `PATCH /academies/{id}/reject` (삭제)
- ❌ `GET /academies/{academyId}/files/{fileId}` (삭제)

**이유:** 프로젝트 컨벤션상 모든 관리자 기능은 `/admin`으로 시작

#### `AcademyController.java` (수정)

1. **파일 업로드 지원**
   - `consumes = MediaType.MULTIPART_FORM_DATA_VALUE` 추가
   - `@RequestBody` → `@ModelAttribute` 변경
   ```java
   @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   public ResponseEntity<AcademyResponse> createAcademy(
           @Valid @ModelAttribute AcademyCreateRequest request) {
       // ...
   }
   ```

2. **불필요한 import 제거**
   - `AcademyRejectRequest` (AdminController로 이동)
   - `PreAuthorize` (관리자 기능 제거)
   - `URI`, `java.net.URI` (파일 다운로드 제거)

#### `AdminController.java` (추가)

**새로운 엔드포인트:**

| 엔드포인트 | 메서드 | 설명 | 권한 |
|-----------|--------|------|------|
| `/admin/academies/{id}/approve` | PATCH | 기관 등록 승인 | ADMIN |
| `/admin/academies/{id}/reject` | PATCH | 기관 등록 거절 | ADMIN |
| `/admin/academies/{academyId}/files/{fileId}` | GET | 재직증명서 다운로드 | ADMIN |
| `/admin/accounts/{accountId}/approve` | PATCH | 회원 승인 | ADMIN |

**의존성 주입:**
```java
private final AccountRepository accountRepository;
private final EmailSendService emailSendService;
private final AcademyService academyService;          // 추가
private final AcademyFileService academyFileService;  // 추가
```

**주요 메서드:**

1. **기관 승인**
   ```java
   @PatchMapping("/academies/{id}/approve")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<AcademyResponse> approveAcademy(@PathVariable Long id) {
       AcademyResponse academyResponse = academyService.approveAcademy(id);
       return ResponseEntity.ok(academyResponse);
   }
   ```

2. **기관 거절**
   ```java
   @PatchMapping("/academies/{id}/reject")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<AcademyResponse> rejectAcademy(
           @PathVariable Long id,
           @Valid @RequestBody AcademyRejectRequest request) {
       AcademyResponse response = academyService.rejectAcademy(id, request.getReason());
       return ResponseEntity.ok(response);
   }
   ```

3. **파일 다운로드 (S3 Presigned URL 리다이렉트)**
   ```java
   @GetMapping("/academies/{academyId}/files/{fileId}")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<Void> downloadAcademyFile(
           @PathVariable Long academyId,
           @PathVariable Long fileId) {
       String presignedUrl = academyFileService.getFileUrl(fileId);
       return ResponseEntity.status(HttpStatus.FOUND)
           .location(URI.create(presignedUrl))
           .build();
   }
   ```

---

### 2.5 Email Service 구현

#### `EmailSendService.java` (인터페이스)
3개의 새로운 메서드 시그니처 추가

```java
void sendAcademyApprovalEmail(String toEmail, String academyName);
void sendAcademyRejectionEmail(String toEmail, String academyName, String reason);
void sendAccountApprovalEmail(String toEmail, String userName);
```

#### `EmailSendServiceImpl.java` (구현체)

**1. 기관 승인 이메일**
```java
@Override
public void sendAcademyApprovalEmail(String toEmail, String academyName) {
    try {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom("noreply@softwarecampus.com");
        helper.setTo(toEmail);
        helper.setSubject("[코스타] 기관 등록이 승인되었습니다");
        
        String htmlContent = String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>기관 등록 승인 안내</h2>
                <p>안녕하세요,</p>
                <p><strong>%s</strong> 기관 등록이 승인되었습니다.</p>
                <p>이제 회원가입을 진행하실 수 있습니다.</p>
                <hr/>
                <p style="color: #888;">감사합니다.</p>
            </body>
            </html>
            """, academyName);
        
        helper.setText(htmlContent, true);
        javaMailSender.send(message);
        
        log.info("기관 승인 이메일 발송 완료: {}", toEmail);
    } catch (Exception e) {
        log.error("기관 승인 이메일 발송 실패: {}", toEmail, e);
        throw new EmailException(EmailErrorCode.EMAIL_SEND_FAILED);
    }
}
```

**2. 기관 거절 이메일**
```java
@Override
public void sendAcademyRejectionEmail(String toEmail, String academyName, String reason) {
    // HTML 템플릿에 거절 사유 포함
    String htmlContent = String.format("""
        <html>
        <body style="font-family: Arial, sans-serif;">
            <h2>기관 등록 거절 안내</h2>
            <p>안녕하세요,</p>
            <p><strong>%s</strong> 기관 등록이 거절되었습니다.</p>
            <div style="background-color: #f5f5f5; padding: 15px; margin: 20px 0;">
                <strong>거절 사유:</strong><br/>
                %s
            </div>
            <p>수정 후 다시 신청해주시기 바랍니다.</p>
            <hr/>
            <p style="color: #888;">감사합니다.</p>
        </body>
        </html>
        """, academyName, reason);
    // ...
}
```

**3. 회원 승인 이메일**
```java
@Override
public void sendAccountApprovalEmail(String toEmail, String userName) {
    String htmlContent = String.format("""
        <html>
        <body style="font-family: Arial, sans-serif;">
            <h2>회원가입 승인 안내</h2>
            <p>안녕하세요, <strong>%s</strong>님</p>
            <p>코스타 소프트웨어 아카데미 회원가입이 승인되었습니다.</p>
            <p>이제 로그인하여 서비스를 이용하실 수 있습니다.</p>
            <hr/>
            <p style="color: #888;">감사합니다.</p>
        </body>
        </html>
        """, userName);
    // ...
}
```

---

## 🔄 3. 변경 사항 요약

### 3.1 수정된 파일

| 파일 경로 | 변경 유형 | 주요 변경 내용 |
|----------|----------|---------------|
| `domain/academy/Academy.java` | 수정 | `rejectionReason` 필드 추가, `reject()` 메서드 수정 |
| `dto/academy/AcademyRejectRequest.java` | 신규 생성 | 거절 사유 DTO |
| `service/academy/AcademyService.java` | 수정 | `rejectAcademy()` 메서드 시그니처 추가 |
| `service/academy/AcademyServiceImpl.java` | 수정 | 파일 업로드, 이메일 발송 로직 통합 |
| `controller/academy/AcademyController.java` | 수정 | Multipart 지원, 관리자 엔드포인트 제거 |
| `controller/admin/AdminController.java` | 수정 | 기관/회원 승인/거절 엔드포인트 추가 |
| `service/user/email/EmailSendService.java` | 수정 | 3개 메서드 시그니처 추가 |
| `service/user/email/EmailSendServiceImpl.java` | 수정 | 3개 이메일 발송 메서드 구현 |

### 3.2 기존 재사용 파일 (변경 없음)

| 파일 경로 | 용도 |
|----------|------|
| `domain/academy/AcademyFile.java` | 파일 메타데이터 엔티티 |
| `repository/academy/AcademyFileRepository.java` | 파일 Repository |
| `service/academy/AcademyFileService.java` | 파일 Service 인터페이스 |
| `service/academy/AcademyFileServiceImpl.java` | 파일 Service 구현체 (S3 연동) |
| `dto/academy/AcademyCreateRequest.java` | 기관 등록 요청 DTO (files 필드 포함) |
| `dto/academy/AcademyResponse.java` | 기관 응답 DTO (attachedFiles 포함) |
| `common/FileType.java` | ACADEMY_FILE 열거형 |
| `service/S3Service.java` | S3 Presigned URL 생성 |

---

## 📡 4. API 엔드포인트 변경

### 4.1 신규 엔드포인트 (AdminController)

| 엔드포인트 | 메서드 | 설명 | 권한 | 상태 |
|-----------|--------|------|------|------|
| `/admin/academies/{id}/approve` | PATCH | 기관 승인 | ADMIN | ✅ 구현 완료 |
| `/admin/academies/{id}/reject` | PATCH | 기관 거절 | ADMIN | ✅ 구현 완료 |
| `/admin/academies/{academyId}/files/{fileId}` | GET | 파일 다운로드 | ADMIN | ✅ 구현 완료 |
| `/admin/accounts/{accountId}/approve` | PATCH | 회원 승인 | ADMIN | ✅ 구현 완료 |

### 4.2 수정된 엔드포인트 (AcademyController)

| 엔드포인트 | 변경 사항 |
|-----------|----------|
| `POST /academies` | `consumes = MULTIPART_FORM_DATA_VALUE` 추가<br/>`@RequestBody` → `@ModelAttribute` 변경 |

### 4.3 삭제된 엔드포인트

| 기존 경로 | 새 경로 | 사유 |
|----------|---------|------|
| `PATCH /academies/{id}/approve` | `PATCH /admin/academies/{id}/approve` | 관리자 기능 컨벤션 통일 |
| `PATCH /academies/{id}/reject` | `PATCH /admin/academies/{id}/reject` | 관리자 기능 컨벤션 통일 |
| `GET /academies/{academyId}/files/{fileId}` | `GET /admin/academies/{academyId}/files/{fileId}` | 관리자 기능 컨벤션 통일 |

---

## 🎯 5. 핵심 개선 사항

### 5.1 API 설계 개선

**Before:**
```
PATCH /academies/{id}/approve     (관리자 전용)
PATCH /accounts/{accountId}/approve (관리자 전용)
```

**After:**
```
PATCH /admin/academies/{id}/approve
PATCH /admin/accounts/{accountId}/approve
```

**개선 효과:**
- ✅ 관리자 기능의 명확한 식별 (`/admin` prefix)
- ✅ RESTful 컨벤션 준수
- ✅ 프론트엔드 라우팅 간소화

### 5.2 이메일 발송 자동화

**2단계 승인 프로세스 완성:**

```
1단계: 기관 등록 승인
   ↓
[이메일 발송] "기관 등록이 승인되었습니다"
   ↓
2단계: 회원가입
   ↓
[이메일 발송] "회원가입이 승인되었습니다"
```

**개선 효과:**
- ✅ 사용자 경험 향상 (승인 알림 자동화)
- ✅ 관리자 업무 부담 감소
- ✅ 거절 시 사유 전달로 재신청 유도

---

## 🔐 6. 보안 고려사항

### 6.1 구현된 보안 조치

| 보안 항목 | 구현 내용 |
|----------|----------|
| **권한 제어** | `@PreAuthorize("hasRole('ADMIN')")` 적용 |
| **파일 접근 제한** | Presigned URL (1시간 유효) |
| **파일 크기 제한** | 최대 10MB (application.properties) |
| **파일 형식 검증** | PDF, JPG, JPEG, PNG, DOC, DOCX만 허용 |
| **S3 버킷 보안** | Private 버킷 사용 |

### 6.2 추가 권장 사항

- [ ] CSRF 토큰 검증 (Multipart 업로드 시)
- [ ] Rate Limiting (파일 업로드 남용 방지)
- [ ] 파일 바이러스 스캔 (AWS S3 Object Lambda 활용)
- [ ] 이메일 발송 실패 시 재시도 로직

---

## ✅ 7. 테스트 체크리스트

### 7.1 컴파일 검증

- [x] `Academy.java` - 컴파일 오류 없음
- [x] `AcademyRejectRequest.java` - 컴파일 오류 없음
- [x] `AcademyServiceImpl.java` - 컴파일 오류 없음
- [x] `AcademyController.java` - 컴파일 오류 없음
- [x] `AdminController.java` - 컴파일 오류 없음
- [x] `EmailSendServiceImpl.java` - 컴파일 오류 없음

### 7.2 필요한 테스트 (미완료)

#### 단위 테스트
- [ ] `AcademyServiceImplTest.createAcademy_파일업로드_성공()`
- [ ] `AcademyServiceImplTest.approveAcademy_이메일발송_검증()`
- [ ] `AcademyServiceImplTest.rejectAcademy_이메일발송_검증()`
- [ ] `EmailSendServiceImplTest.sendAcademyApprovalEmail_성공()`
- [ ] `EmailSendServiceImplTest.sendAcademyRejectionEmail_성공()`
- [ ] `EmailSendServiceImplTest.sendAccountApprovalEmail_성공()`

#### 통합 테스트
- [ ] `AdminControllerTest.approveAcademy_ADMIN권한_성공()`
- [ ] `AdminControllerTest.rejectAcademy_사유포함_성공()`
- [ ] `AdminControllerTest.downloadFile_Presigned URL_리다이렉트()`
- [ ] `AcademyControllerTest.createAcademy_Multipart_업로드()`

#### E2E 시나리오 테스트 (Postman)
- [ ] 기관 등록 → 파일 업로드 → S3 저장 확인
- [ ] 기관 승인 → 이메일 수신 확인
- [ ] 기관 거절 → 거절 사유 포함 이메일 수신
- [ ] 회원 승인 → 이메일 수신 확인
- [ ] 파일 다운로드 → Presigned URL 유효성 확인

---

## 📝 8. 향후 작업

### 8.1 우선순위 높음 (Phase 5)

1. **테스트 작성**
   - 단위 테스트 (Service Layer)
   - 통합 테스트 (Controller Layer)
   - E2E 시나리오 테스트

2. **프론트엔드 연동**
   - 기관 등록 폼 (파일 업로드 포함)
   - 관리자 페이지 (승인/거절 UI)
   - 이메일 링크 연동

3. **이메일 템플릿 개선**
   - HTML 파일로 분리 (현재 인라인 문자열)
   - 템플릿 엔진 도입 (Thymeleaf 등)

### 8.2 우선순위 보통

- [ ] 거절 사유 템플릿화 (드롭다운 선택)
- [ ] 파일 다운로드 로그 기록
- [ ] 이메일 발송 실패 시 관리자 알림
- [ ] 파일 업로드 진행률 표시 (프론트엔드)

### 8.3 우선순위 낮음

- [ ] 기관 승인 통계 대시보드
- [ ] 파일 자동 압축 (이미지)
- [ ] 다국어 이메일 템플릿
- [ ] PDF 미리보기 기능

---

## 📊 9. 구현 통계

### 9.1 코드 변경 통계

| 구분 | 파일 수 | 라인 수 (추정) |
|------|---------|---------------|
| 신규 생성 | 1개 | ~20 라인 |
| 수정 | 7개 | ~200 라인 |
| 재사용 (기존) | 8개 | - |
| **합계** | **16개** | **~220 라인** |

### 9.2 기능 구현 진행률

| Phase | 상태 | 완료율 |
|-------|------|--------|
| Phase 1: 엔티티/DTO | ✅ 완료 | 100% |
| Phase 2: 파일 업로드 | ✅ 완료 | 100% |
| Phase 3: 승인/거절 로직 | ✅ 완료 | 100% |
| Phase 4: 이메일 발송 | ✅ 완료 | 100% |
| Phase 5: 테스트 | ⏳ 진행 중 | 0% |
| **전체 진행률** | - | **80%** |

---

## 🎓 10. 학습 포인트 및 베스트 프랙티스

### 10.1 구현 시 적용한 원칙

1. **단일 책임 원칙 (SRP)**
   - `AcademyFileService`: 파일 업로드만 담당
   - `EmailSendService`: 이메일 발송만 담당
   - Service 레이어에서 조합

2. **RESTful API 설계**
   - 자원 중심 URL 설계
   - HTTP 메서드 의미에 맞는 사용 (PATCH: 부분 업데이트)
   - 상태 코드 활용 (302: Redirect)

3. **보안 강화**
   - 관리자 기능 `@PreAuthorize` 적용
   - Presigned URL로 임시 접근 권한 부여
   - 민감 정보 (재직증명서) 접근 제한

4. **코드 추적성**
   - 모든 수정 사항에 작성자/작성일 주석 추가
   - Git commit 메시지 컨벤션 준수

### 10.2 개선 가능한 부분

1. **이메일 템플릿 관리**
   - 현재: 인라인 HTML 문자열 (하드코딩)
   - 개선안: Thymeleaf 템플릿 엔진 도입

2. **파일 검증 강화**
   - 현재: 확장자 기반 검증
   - 개선안: MIME 타입 + Magic Number 검증

3. **에러 핸들링**
   - 현재: 서비스 레이어에서 예외 발생
   - 개선안: 커스텀 예외 + GlobalExceptionHandler

---

## 🔗 11. 관련 문서

- [설계 문서 v2](./Phase1_기관등록_파일업로드_설계_v2.md)
- [회원가입 API 명세](../../../docs/api/01_signup.md)
- [약관 동의 구현 보고](../terms/Phase2_구현_완료_보고.md)

---

## 📞 12. 문의 및 피드백

**작성자:** GitHub Copilot  
**작성일:** 2025년 11월 28일  
**문서 버전:** 1.0  
**최종 업데이트:** 2025년 11월 28일 15:30

---

**구현 완료 확인:**
- [x] 백엔드 API 구현 (80%)
- [x] 컴파일 오류 해결
- [x] 관리자 컨벤션 준수 (`/admin` prefix)
- [ ] 테스트 작성 (0%)
- [ ] 프론트엔드 연동 (0%)

**다음 작업:** Phase 5 - 테스트 및 검증
