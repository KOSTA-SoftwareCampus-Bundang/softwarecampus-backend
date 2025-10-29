package com.softwarecampus.backend.domain.common;

/**
 * 승인 상태를 나타내는 Enum
 * - PENDING: 승인 대기
 * - APPROVED: 승인 완료
 * - REJECTED: 승인 거부
 */
public enum ApprovalStatus {
    PENDING,   // 승인 대기
    APPROVED,  // 승인 완료
    REJECTED   // 승인 거부
}
