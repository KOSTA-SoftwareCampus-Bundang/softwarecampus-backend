# AWS S3 연동 가이드

## 개요
이 프로젝트는 AWS S3를 사용하여 파일 업로드/다운로드/삭제 기능을 제공합니다.
모든 자격 증명 정보는 환경변수를 통해 주입되며, 코드에 하드코딩되지 않습니다.

## 설정 방법

### 1. AWS IAM 사용자 생성 및 권한 설정

1. AWS Console에서 IAM 서비스로 이동
2. 새 사용자 생성 (프로그래밍 방식 액세스 선택)
3. S3 권한 부여 (AmazonS3FullAccess 또는 커스텀 정책)
4. Access Key와 Secret Key 저장 (재발급 불가하므로 안전하게 보관)

### 2. S3 버킷 생성

1. AWS Console에서 S3 서비스로 이동
2. 새 버킷 생성
3. 리전 선택 (기본값: ap-northeast-2, 서울)
4. 퍼블릭 액세스 차단 설정 확인
5. 버킷 정책 설정 (필요시)

### 3. 환경변수 설정

프로젝트 루트의 `.env` 파일에 다음 항목을 설정하세요:

```properties
# AWS S3 설정
AWS_S3_ACCESS_KEY=your-access-key-here
AWS_S3_SECRET_KEY=your-secret-key-here
AWS_S3_REGION=ap-northeast-2
AWS_S3_BUCKET_NAME=your-bucket-name-here
```

**⚠️ 보안 주의사항:**
- `.env` 파일은 절대 Git에 커밋하지 마세요
- Access Key와 Secret Key는 안전하게 보관하세요
- 프로덕션 환경에서는 AWS IAM Role 사용을 권장합니다

### 4. Maven 의존성 확인

`pom.xml`에 AWS S3 SDK가 추가되었는지 확인:

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.26</version>
</dependency>
```

IDE에서 Maven 의존성을 다시 로드하거나, 다음 명령어를 실행:

```bash
mvn dependency:resolve
```

## 사용 방법

### S3Service 주입

```java
@Service
public class YourService {
    
    private final S3Service s3Service;
    
    public YourService(S3Service s3Service) {
        this.s3Service = s3Service;
    }
    
    // 사용 예시
}
```

### 파일 업로드

```java
public String uploadFile(MultipartFile file) {
    // folder: S3 내 폴더 경로 (예: "images", "documents", "profiles")
    String fileUrl = s3Service.uploadFile(file, "images");
    return fileUrl;  // 반환값: https://bucket-name.s3.region.amazonaws.com/images/uuid-filename.ext
}
```

### 파일 삭제

```java
public void deleteFile(String fileUrl) {
    s3Service.deleteFile(fileUrl);
}
```

### 컨트롤러 예시

`FileUploadController`를 참조하세요:

```bash
POST /api/files/upload
- 파라미터: file (MultipartFile), folder (String, optional)
- 응답: { "fileUrl": "...", "message": "..." }

DELETE /api/files/delete
- 파라미터: fileUrl (String)
- 응답: { "message": "..." }
```

## 파일 구조

```
src/main/java/com/softwarecampus/backend/
├── config/
│   └── S3Config.java                 # S3 클라이언트 빈 설정
├── service/
│   └── S3Service.java                # S3 파일 관리 서비스
├── exception/
│   └── S3UploadException.java        # S3 관련 예외
└── controller/sample/
    └── FileUploadController.java     # 파일 업로드 컨트롤러 예시
```

## 에러 처리

S3 업로드/삭제 실패 시 `S3UploadException`이 발생하며, `GlobalExceptionHandler`에서 다음과 같이 처리됩니다:

- HTTP 500 Internal Server Error
- Problem Details 형식의 응답
- 로그에 상세 에러 정보 기록

## 테스트

### Postman/cURL 테스트

```bash
# 파일 업로드
curl -X POST http://localhost:8080/api/files/upload \
  -F "file=@/path/to/your/file.jpg" \
  -F "folder=images"

# 파일 삭제
curl -X DELETE "http://localhost:8080/api/files/delete?fileUrl=https://bucket-name.s3.region.amazonaws.com/images/uuid-filename.jpg"
```

## 프로덕션 배포 시 고려사항

1. **IAM Role 사용**: EC2/ECS에서는 Access Key 대신 IAM Role을 사용하세요
2. **CloudFront 연동**: 정적 파일 제공 성능 향상을 위해 CloudFront CDN 연동 고려
3. **버킷 정책**: 최소 권한 원칙에 따라 버킷 정책 설정
4. **암호화**: S3 서버 측 암호화(SSE-S3 또는 SSE-KMS) 활성화
5. **비용 최적화**: S3 수명 주기 정책으로 오래된 파일 자동 삭제/아카이빙

## 문제 해결

### 의존성 다운로드 실패
```bash
# Maven 의존성 강제 재다운로드
mvn clean install -U
```

### Access Denied 에러
- IAM 사용자 권한 확인
- 버킷 정책 확인
- Access Key가 올바른지 확인

### Region 불일치
- `.env` 파일의 `AWS_S3_REGION`과 버킷 리전이 일치하는지 확인

## 참고 문서

- [AWS SDK for Java 2.x 공식 문서](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [S3 Java SDK 가이드](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3.html)
- [IAM 모범 사례](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)

