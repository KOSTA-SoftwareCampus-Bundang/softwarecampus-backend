/**
 * AWS S3 클라이언트 설정
 * DefaultCredentialsProvider를 사용하여 다음 순서로 자격 증명을 자동 조회:
 * 1. 환경변수 (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 * 2. 시스템 프로퍼티
 * 3. ~/.aws/credentials 파일
 * 4. IAM Role (EC2 인스턴스 프로필)
 * 5. ECS 컨테이너 자격 증명
 * 
 * 로컬 개발: .env에 AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY 설정
 * AWS 배포: IAM Role 사용 (환경변수 불필요, 더 안전)
 */
package com.softwarecampus.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    /**
     * S3 클라이언트 빈 생성
     * DefaultCredentialsProvider를 사용하여 AWS 표준 자격 증명 체인 적용
     * 
     * @return S3Client 인스턴스
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}

