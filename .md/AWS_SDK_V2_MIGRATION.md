# AWS SDK v2 마이그레이션 가이드

## 개요
AWS SDK for Java v2는 v1과 비교하여 많은 부분이 재설계되었습니다. 이 문서는 S3 Service 구현 시 발견된 주요 변경사항과 해결 방법을 설명합니다.

## 주요 변경 사항

### 1. 패키지 구조 변경

#### v1 (레거시)
```java
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
```

#### v2 (현재)
```java
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
```

**변경 포인트:**
- 루트 패키지: `com.amazonaws` → `software.amazon.awssdk`
- 클라이언트 명명: `AmazonS3` → `S3Client`
- 자격 증명: `BasicAWSCredentials` → `AwsBasicCredentials`

---

### 2. 클라이언트 빌더 패턴 변경

#### v1
```java
BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
    .withRegion(Regions.AP_NORTHEAST_2)
    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
    .build();
```

#### v2
```java
AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
S3Client s3Client = S3Client.builder()
    .region(Region.of(region))
    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
    .build();
```

**변경 포인트:**
- 생성자 대신 **정적 팩토리 메서드** 사용: `new BasicAWSCredentials()` → `AwsBasicCredentials.create()`
- 메서드 명명: `withRegion()` → `region()`
- Region 타입: `Regions` enum → `Region` 클래스

---

### 3. 🔴 ServiceClientConfiguration 제거 (본 이슈)

#### v1에서 가능했던 방법
```java
// v1에서는 클라이언트로부터 region 정보 추출 가능
String region = s3Client.getRegion().toString();
```

#### v2에서 시도했으나 실패한 방법
```java
// ❌ 컴파일 에러 - serviceClientConfiguration() 메서드 존재하지 않음
String region = s3Client.serviceClientConfiguration().region().id();
```

**에러 메시지:**
```
Cannot resolve method 'serviceClientConfiguration()'
```

#### ✅ v2 해결 방법
**S3Client 인스턴스로부터 region을 추출할 수 없으므로, 설정값을 직접 주입받아 사용:**

```java
@Service
public class S3Service {
    private final S3Client s3Client;
    
    // Region을 환경변수로부터 직접 주입
    @Value("${aws.s3.region}")
    private String region;
    
    private String getFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                region,  // 주입받은 region 사용
                key);
    }
}
```

**이유:**
- AWS SDK v2는 불변성(immutability)과 쓰레드 안전성을 강화
- 클라이언트 설정을 외부에서 조회하는 것을 제한하여 캡슐화 강화
- 설정은 빌더를 통해 주입만 가능하고, 이후 조회는 불가능

---

### 4. 파일 업로드 방식 변경

#### v1
```java
s3Client.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), metadata));
```

#### v2
```java
PutObjectRequest putObjectRequest = PutObjectRequest.builder()
    .bucket(bucketName)
    .key(key)
    .contentType(file.getContentType())
    .build();

// RequestBody로 데이터 래핑 필수
s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
```

**변경 포인트:**
- 모든 요청은 **Builder 패턴** 사용
- 파일 데이터는 `RequestBody`로 래핑 필수
- InputStream, byte[], File 등 다양한 소스 지원

---

### 5. 예외 처리 변경

#### v1
```java
try {
    s3Client.putObject(...);
} catch (AmazonServiceException e) {
    // AWS 서비스 에러
} catch (AmazonClientException e) {
    // 클라이언트 에러 (네트워크 등)
}
```

#### v2
```java
try {
    s3Client.putObject(...);
} catch (S3Exception e) {
    // S3 관련 예외 (서비스 에러 포함)
    String errorCode = e.awsErrorDetails().errorCode();
    String errorMessage = e.awsErrorDetails().errorMessage();
}
```

**변경 포인트:**
- 서비스별 구체적 예외: `S3Exception`, `DynamoDbException` 등
- 에러 상세정보: `awsErrorDetails()` 메서드로 접근
- 더 명확한 예외 계층 구조

---

### 6. Region 처리 변경

#### v1
```java
import com.amazonaws.regions.Regions;

s3Client = AmazonS3ClientBuilder.standard()
    .withRegion(Regions.AP_NORTHEAST_2)
    .build();
```

#### v2
```java
import software.amazon.awssdk.regions.Region;

S3Client s3Client = S3Client.builder()
    .region(Region.AP_NORTHEAST_2)  // 상수 사용
    // 또는
    .region(Region.of("ap-northeast-2"))  // 문자열로부터 생성
    .build();
```

**변경 포인트:**
- `Regions` enum → `Region` 클래스
- 동적 region 생성: `Region.of(String)` 메서드 사용

---

### 7. Presigned URL 생성 변경

#### v1
```java
Date expiration = new Date();
expiration.setTime(expiration.getTime() + 3600 * 1000); // 1시간

GeneratePresignedUrlRequest generatePresignedUrlRequest = 
    new GeneratePresignedUrlRequest(bucketName, key)
        .withMethod(HttpMethod.GET)
        .withExpiration(expiration);

URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
```

#### v2
```java
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.time.Duration;

// 별도의 Presigner 클라이언트 필요
S3Presigner presigner = S3Presigner.builder()
    .region(Region.of(region))
    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
    .build();

GetObjectRequest getObjectRequest = GetObjectRequest.builder()
    .bucket(bucketName)
    .key(key)
    .build();

GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
    .signatureDuration(Duration.ofHours(1))
    .getObjectRequest(getObjectRequest)
    .build();

PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
String url = presignedRequest.url().toString();

presigner.close(); // 사용 후 반드시 close
```

**변경 포인트:**
- 별도의 `S3Presigner` 클라이언트 사용
- `Date` 대신 `Duration` 사용 (Java 8 Time API)
- Builder 패턴으로 복잡도 증가
- 리소스 관리 필요 (close 호출)

---

### 8. Multipart Upload 변경

#### v1
```java
InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(
    new InitiateMultipartUploadRequest(bucketName, key));
String uploadId = initResponse.getUploadId();
```

#### v2
```java
CreateMultipartUploadRequest createMultipartUploadRequest = 
    CreateMultipartUploadRequest.builder()
        .bucket(bucketName)
        .key(key)
        .build();

CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createMultipartUploadRequest);
String uploadId = response.uploadId();
```

**변경 포인트:**
- 메서드명 변경: `initiateMultipartUpload` → `createMultipartUpload`
- 요청/응답 객체 모두 Builder 패턴
- Getter 네이밍: `getUploadId()` → `uploadId()` (lombok 스타일)

---

## 우리 프로젝트에서 적용된 해결 방법

### 문제: Region 정보 조회 불가

```java
// ❌ v2에서 불가능
private String getFileUrl(String key) {
    return String.format("https://%s.s3.%s.amazonaws.com/%s",
            bucketName,
            s3Client.serviceClientConfiguration().region().id(), // 존재하지 않는 메서드
            key);
}
```

### 해결: 환경변수 직접 주입

```java
// ✅ v2 해결 방법
@Service
public class S3Service {
    
    @Value("${aws.s3.region}")
    private String region;  // application.properties에서 주입
    
    private String getFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                region,  // 주입받은 값 사용
                key);
    }
}
```

**설정 파일 (application.properties):**
```properties
aws.s3.region=${AWS_S3_REGION:ap-northeast-2}
```

**환경변수 (.env):**
```properties
AWS_S3_REGION=ap-northeast-2
```

---

## v2의 장점

### 1. **성능 향상**
- Non-blocking I/O 지원 (비동기 클라이언트)
- 메모리 효율성 개선
- 더 빠른 시작 시간

### 2. **타입 안전성**
- Builder 패턴으로 컴파일 타임 검증
- 불변 객체 사용으로 스레드 안전성 보장

### 3. **모던 Java 지원**
- Java 8+ 기능 활용 (Duration, CompletableFuture 등)
- 람다, 스트림 API와 잘 통합

### 4. **명확한 API**
- 일관된 네이밍 컨벤션
- 서비스별 구체적 예외 타입

---

## 마이그레이션 체크리스트

- [x] 패키지 임포트 변경 (`com.amazonaws` → `software.amazon.awssdk`)
- [x] 클라이언트 빌더 패턴 적용
- [x] Region 정보를 환경변수로부터 주입받도록 변경
- [x] RequestBody를 사용한 파일 업로드 구현
- [x] S3Exception 예외 처리
- [x] PutObjectRequest, DeleteObjectRequest 빌더 패턴 적용
- [x] application.properties에 region 설정 추가
- [ ] (필요시) Presigned URL 생성 로직 마이그레이션
- [ ] (필요시) Multipart Upload 로직 마이그레이션

---

## 추가 참고 자료

- [AWS SDK for Java 2.x 공식 문서](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [마이그레이션 가이드](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/migration.html)
- [S3 Code Examples](https://github.com/awsdocs/aws-doc-sdk-examples/tree/main/javav2/example_code/s3)
- [API Reference](https://sdk.amazonaws.com/java/api/latest/)

---

## 결론

AWS SDK v2는 v1에 비해 더 모던하고 안전한 API를 제공하지만, 일부 편의 기능(클라이언트 설정 조회 등)은 제거되었습니다. 

**핵심 원칙:**
- 설정은 빌더를 통해 주입만 가능
- 런타임 조회가 필요한 값은 별도로 관리 (환경변수, 필드 등)
- 불변성과 타입 안전성을 우선시

이러한 제약은 더 안전하고 예측 가능한 코드를 작성하도록 유도합니다.

