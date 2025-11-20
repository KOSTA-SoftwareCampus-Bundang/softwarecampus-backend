package com.softwarecampus.backend.controller.banner;

import com.softwarecampus.backend.dto.banner.FileDeleteRequest;
import com.softwarecampus.backend.service.banner.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private static final String DEFAULT_FILE_DIR = "files";

    /**
     *  파일 업로드 API
     */
    @PostMapping(value = "/api/files/upload/", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) {

        String fileUrl = fileService.uploadFile(file, DEFAULT_FILE_DIR);

        return ResponseEntity.status(HttpStatus.CREATED).body(fileUrl);
    }

    /**
     *  관리자용 파일 삭제 API
     */
    @DeleteMapping("/api/admin/files/delete")
    public ResponseEntity<Void> deleteFile(@RequestBody FileDeleteRequest request) {

        fileService.deleteFile(request.getFileUrl());

        return ResponseEntity.noContent().build();
    }
}
