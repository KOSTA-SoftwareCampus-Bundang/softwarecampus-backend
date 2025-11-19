package com.softwarecampus.backend.service.banner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements  FileService {


    @Override
    public String uploadFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            log.info("파일 업로드 시작 : {} (디렉토리: {})", file.getOriginalFilename(), directory);
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            return "https://storage.example.com/" + directory + "/" + fileName;
        } catch (Exception e) {
            log.error("파일 업로드 중 오류 발생", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        log.info("S3 파일 삭제 요청: {}", fileUrl);
    }
}
