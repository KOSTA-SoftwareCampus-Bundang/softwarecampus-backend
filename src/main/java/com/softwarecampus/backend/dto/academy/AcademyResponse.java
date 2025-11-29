package com.softwarecampus.backend.dto.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.ApprovalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 기관 응답 DTO
 * 첨부파일 정보를 포함합니다.
 */
@Getter
@Builder
public class AcademyResponse {
    private Long id;
    private String name;
    private String address;
    private String businessNumber;
    private String email;
    private ApprovalStatus isApproved;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 첨부파일 목록 (사업자등록증, 교육기관 인증서 등)
     */
    private List<FileInfo> attachedFiles;

    /**
     * 파일 정보 DTO
     */
    @Getter
    @Builder
    public static class FileInfo {
        private Long id;
        private String originalFileName;
        private String downloadUrl;  // 파일 다운로드 URL (Presigned URL)
        private Long fileSize;
        private String contentType;
        private LocalDateTime uploadedAt;
    }

    /**
     * entity -> DTO 변환을 위한 메서드
     */
    public static AcademyResponse from(Academy academy) {
        // 첨부파일 정보 매핑 (files가 null일 경우 빈 리스트 반환)
        List<FileInfo> fileInfos = academy.getFiles() != null 
            ? academy.getFiles().stream()
                .map(file -> FileInfo.builder()
                    .id(file.getId())
                    .originalFileName(file.getOriginalFileName())
                    .downloadUrl(file.getFileUrl())  // S3 URL (추후 Presigned URL로 대체 가능)
                    .fileSize(file.getFileSize())
                    .contentType(file.getContentType())
                    .uploadedAt(file.getUploadedAt())
                    .build())
                .collect(Collectors.toList())
            : Collections.emptyList();
        
        return AcademyResponse.builder()
                .id(academy.getId())
                .name(academy.getName())
                .address(academy.getAddress())
                .businessNumber(academy.getBusinessNumber())
                .email(academy.getEmail())
                .isApproved(academy.getIsApproved())
                .approvedAt(academy.getApprovedAt())
                .createdAt(academy.getCreatedAt())
                .updatedAt(academy.getUpdatedAt())
                .attachedFiles(fileInfos)
                .build();
    }
}
