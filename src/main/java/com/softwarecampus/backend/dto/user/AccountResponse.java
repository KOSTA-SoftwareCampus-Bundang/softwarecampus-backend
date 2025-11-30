package com.softwarecampus.backend.dto.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;

/**
 * 계정 정보 응답 DTO
 * 
 * @param id             계정 ID
 * @param email          이메일
 * @param userName       사용자명
 * @param phoneNumber    전화번호
 * @param accountType    계정 타입 (USER, ACADEMY, ADMIN)
 * @param approvalStatus 승인 상태 (PENDING, APPROVED, REJECTED)
 * @param address        주소
 * @param affiliation    소속
 * @param position       직책
 * @param profileImage   프로필 이미지 URL
 */
public record AccountResponse(
        Long id,
        String email,
        String userName,
        String phoneNumber,
        AccountType accountType,
        ApprovalStatus approvalStatus,
        String address,
        String affiliation,
        String position,
        String profileImage) {
}
