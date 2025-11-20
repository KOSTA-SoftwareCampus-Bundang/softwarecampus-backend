package com.softwarecampus.backend.service.banner;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     *  파일을 s3에 업로드하고, 저장된 파일의 URL을 반환합니다.
     */
    String uploadFile(MultipartFile file, String directory);

    /**
     *  s3에 저장된 파일을 삭제합니다.
     */
    void deleteFile(String fileUrl);
}
