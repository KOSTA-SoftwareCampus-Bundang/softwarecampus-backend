package com.softwarecampus.backend.service.banner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
//@RequiredArgsConstructor
public class FileServiceImpl implements  FileService {

//    private final AmazonS3 amazonS3Client;

//    @Value("${cloud.aws.s3.bucket}")
//    private String bucketName;
//
//    @Value("${cloud.aws.s3.url}")
//    private String baseUrl;

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try (InputStream inputStream = file.getInputStream()) {

            log.info("파일 업로드 시작 : {} (디렉토리: {})", file.getOriginalFilename(), directory);

            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + fileExtension;
            String key = directory + "/" + fileName;

            // S3 통신 로직
//            ObjectMetadata metadata = new ObjectMetadata();
//            metadata.setContentLength(file.getSize());
//            metadata.setContentType(file.getContentType());
//            amazonS3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata));

//            return baseUrl + "/" + key;

            return "https://storage.example.com/" + directory + "/" + fileName;
        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
//        if (fileUrl == null || fileUrl.isEmpty() || !fileUrl.contains(baseUrl)) {
//            log.warn("삭제할 파일 URL이 유효하지 않습니다: {}", fileUrl);
//            return;
//        }

//        try {
//            String key = fileUrl.substring(baseUrl.length() + 1);
//            log.info("S3 파일 삭제 요청 (버킷: {}, 키: {}", bucketName, key);
//
//            amazonS3Clien.deleteObject(bucketName, key);
//        }catch (Exception e) {
//            log.error("S3 파일 삭제 중 오류 발생: {}", fileUrl, e);
//            throw new RuntimeException(e);
//        }
        log.info("S3 파일 삭제 요청: {}", fileUrl);
    }
}
