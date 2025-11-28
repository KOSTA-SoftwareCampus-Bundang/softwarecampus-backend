# 약관 동의 이력 저장 (Terms Agreement Tracking)

## 1. 개요
회원가입 시 사용자의 **이용약관** 및 **개인정보 처리방침** 동의 여부와 **동의 일시**를 데이터베이스에 저장하여 법적 근거를 마련하고 규정 준수(Compliance)를 강화합니다.

## 2. 데이터베이스 변경 (Database Changes)

`account` 테이블에 약관 동의 관련 컬럼을 추가합니다.

### 2.1 추가 컬럼
| 컬럼명 | 타입 | 설명 | 필수 여부 |
|--------|------|------|-----------|
| `terms_agreed` | `BOOLEAN` | 이용약관 동의 여부 | NOT NULL (Default: false) |
| `terms_agreed_at` | `DATETIME` | 이용약관 동의 일시 | NULLABLE |
| `privacy_agreed` | `BOOLEAN` | 개인정보 처리방침 동의 여부 | NOT NULL (Default: false) |
| `privacy_agreed_at` | `DATETIME` | 개인정보 처리방침 동의 일시 | NULLABLE |
| `marketing_agreed` | `BOOLEAN` | 마케팅 수신 동의 여부 | NOT NULL (Default: false) |
| `marketing_agreed_at` | `DATETIME` | 마케팅 수신 동의 일시 | NULLABLE |

## 3. 코드 변경 (Code Changes)

### 3.1 Entity (`Account.java`)
새로운 필드를 엔티티에 추가합니다.

```java
// 약관 동의 필드
@Column(name = "terms_agreed", nullable = false)
private boolean termsAgreed;

@Column(name = "terms_agreed_at")
private LocalDateTime termsAgreedAt;

@Column(name = "privacy_agreed", nullable = false)
private boolean privacyAgreed;

@Column(name = "privacy_agreed_at")
private LocalDateTime privacyAgreedAt;

@Column(name = "marketing_agreed", nullable = false)
private boolean marketingAgreed;

@Column(name = "marketing_agreed_at")
private LocalDateTime marketingAgreedAt;
```

### 3.2 DTO (`SignupRequest.java`)
프론트엔드로부터 동의 여부를 전달받기 위해 요청 DTO를 수정합니다.

```java
// 추가 필드
@NotNull(message = "이용약관 동의는 필수입니다")
Boolean termsAgreed,

@NotNull(message = "개인정보 처리방침 동의는 필수입니다")
Boolean privacyAgreed,

Boolean marketingAgreed // 선택 사항 (Null이면 false 처리)
```

### 3.3 Service (`AccountService.java`)
회원가입 로직 수행 시, 동의 여부를 확인하고 현재 시간(`LocalDateTime.now()`)을 동의 일시로 저장합니다.

## 4. 프론트엔드 연동 (Frontend Integration)
- **SignupPage.tsx**:
  - 약관 동의 체크박스 UI 추가 (전체 동의, 이용약관(필수), 개인정보(필수), 마케팅(선택))
  - 회원가입 API 호출 시 `termsAgreed`, `privacyAgreed`, `marketingAgreed` 값을 함께 전송

## 5. 작업 순서
1. `Account` 엔티티 수정 (DB 스키마 변경)
2. `SignupRequest` DTO 수정
3. `AccountService` 로직 수정 (매핑 및 시간 저장)
4. 프론트엔드 UI 및 API 호출부 수정
