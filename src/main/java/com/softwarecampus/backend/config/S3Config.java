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

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;
    
    //스프링에서 자동 등록되는 빈(기본적으로 application.properties 
    //환경변수 값만 가지고있지만 import 설정을 통해 .env 설정값도 가지고있음
    @Autowired
    private Environment env;

    //설정 클래스가 빈으로 등록되고 aws access key가 환경변수로 등록되게함
    //이떄 AWS_ACCESS_KEY_ID 이름으로 등록되면 DefaultCredentialProvider가 인식하지 못함
    //따라서 aws.accessKeyId와 같은 표준으로 환경변수값 등록
    @PostConstruct
    public void init() {
        System.setProperty("aws.accessKeyId", env.getProperty("AWS_ACCESS_KEY_ID"));
        System.setProperty("aws.secretAccessKey", env.getProperty("AWS_SECRET_ACCESS_KEY"));
    }
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

    /**
     * S3 Presigner 빈 생성
     * Presigned URL 생성을 위한 전용 클라이언트
     * 
     * @return S3Presigner 인스턴스
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}

