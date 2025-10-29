package com.softwarecampus.backend.dto.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import com.softwarecampus.backend.domain.academy.ApprovalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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

    // entity -> DTO 변환을 위한 메서드
    public static AcademyResponse from(Academy academy) {
        return AcademyResponse.builder()
                .id(academy.getId())
                .name(academy.getName())
                .address(academy.getAddress())
                .businessNumber(academy.getBusinessNumber()) // 도메인 필드명에 맞춤
                .email(academy.getEmail())
                .isApproved(academy.getIsApproved())
                .approvedAt(academy.getApprovedAt())
                .createdAt(academy.getCreatedAt())
                .updatedAt(academy.getUpdatedAt())
                .build();
    }

}
