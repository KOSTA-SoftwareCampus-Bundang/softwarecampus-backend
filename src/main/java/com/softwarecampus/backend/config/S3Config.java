/**
 * 객체 스토리지 클라이언트 설정
 * Cloudflare R2(S3 호환 API)를 대상으로 StaticCredentialsProvider와 endpointOverride를 사용합니다.
 * 로컬 개발 및 서버 환경 모두 동일한 정적 자격증명 방식을 사용합니다. (IAM Role 불필요)
 */
package com.softwarecampus.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${storage.object.access-key}")
    private String accessKey;

    @Value("${storage.object.secret-key}")
    private String secretKey;

    @Value("${storage.object.region:auto}")
    private String region;

    @Value("${storage.object.endpoint}")
    private String endpoint;

    @Value("${storage.object.path-style-access-enabled:true}")
    private boolean pathStyleAccessEnabled;

    /**
     * S3 호환 클라이언트 빈 생성
     * Cloudflare R2 엔드포인트 및 정적 자격증명을 사용합니다.
     *
     * @return S3Client 인스턴스
     */
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3Configuration serviceConfig = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .build();
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfig);
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    /**
     * S3 Presigner 빈 생성
     * Presigned URL 생성을 위한 전용 클라이언트 (기관 파일 임시 다운로드 링크 발급용)
     *
     * @return S3Presigner 인스턴스
     */
    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3Configuration serviceConfig = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .build();
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfig);
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
