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

        private Integer courseCount;
        private Integer reviewCount;
        private Double rating;

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
                private String downloadUrl; // 파일 다운로드 URL (Presigned URL)
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
                                                                .downloadUrl(file.getFileUrl()) // S3 URL (추후 Presigned
                                                                                                // URL로 대체 가능)
                                                                .fileSize(file.getFileSize())
                                                                .contentType(file.getContentType())
                                                                .uploadedAt(file.getUploadedAt())
                                                                .build())
                                                .collect(Collectors.toList())
                                : Collections.emptyList();

                // 과정 수 계산 (삭제되지 않은 과정만)
                int courseCount = 0;
                int reviewCount = 0;
                double rating = 0.0;

                if (academy.getCourses() != null) {
                        var activeCourses = academy.getCourses().stream()
                                        .filter(c -> c.isActive())
                                        .toList();

                        courseCount = activeCourses.size();

                        // 리뷰 수 및 평점 계산
                        var activeReviews = activeCourses.stream()
                                        .filter(c -> c.getReviews() != null)
                                        .flatMap(c -> c.getReviews().stream())
                                        .filter(r -> r.isActive() && r
                                                        .getApprovalStatus() == com.softwarecampus.backend.domain.common.ApprovalStatus.APPROVED)
                                        .toList();

                        reviewCount = activeReviews.size();

                        if (reviewCount > 0) {
                                rating = activeReviews.stream()
                                                .mapToDouble(com.softwarecampus.backend.domain.course.CourseReview::calculateAverageScore)
                                                .average()
                                                .orElse(0.0);
                                // 소수점 1자리 반올림
                                rating = Math.round(rating * 10.0) / 10.0;
                        }
                }

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
                                .courseCount(courseCount)
                                .reviewCount(reviewCount)
                                .rating(rating)
                                .build();
        }
}
