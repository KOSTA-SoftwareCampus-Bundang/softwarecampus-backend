package com.softwarecampus.backend.service.admin;

import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.admin.DashboardStatsResponse;
import com.softwarecampus.backend.dto.admin.InstitutionDashboardStatsResponse;
import com.softwarecampus.backend.exception.course.BadRequestException;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.repository.course.CourseRepository;
import com.softwarecampus.backend.repository.course.CourseReviewRepository;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드 서비스 구현체
 * 작성일: 2025-12-02
 * 
 * 대시보드 통계 조회 로직을 서비스 계층으로 분리하여
 * Controller-Service-Repository 레이어 규칙을 준수
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AccountRepository accountRepository;
    private final CourseRepository courseRepository;
    private final CourseReviewRepository reviewRepository;

    /**
     * 관리자 대시보드 통계 조회
     * - 전체 회원 수, 과정 수, 리뷰 수
     * - 승인 대기 중인 과정 및 리뷰 수
     */
    @Override
    public DashboardStatsResponse getDashboardStats() {
        long totalUsers = accountRepository.countByDeletedAtIsNull();
        long totalCourses = courseRepository.countByDeletedAtIsNull();
        long totalReviews = reviewRepository.countByDeletedAtIsNull();
        long pendingCourses = courseRepository.countByIsApprovedAndDeletedAtIsNull(ApprovalStatus.PENDING);
        long pendingReviews = reviewRepository.countByApprovalStatusAndDeletedAtIsNull(ApprovalStatus.PENDING);

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalCourses(totalCourses)
                .totalReviews(totalReviews)
                .pendingCourses(pendingCourses)
                .pendingReviews(pendingReviews)
                .build();
    }

    /**
     * 기관 대시보드 통계 조회
     * - 기관 소속 과정 수, 리뷰 수
     * - 승인 대기 중인 과정 및 리뷰 수
     * - Soft Delete 준수: 삭제되지 않은 데이터만 집계
     */
    @Override
    public InstitutionDashboardStatsResponse getInstitutionDashboardStats(Long academyId) {
        long totalCourses = courseRepository.countByAcademyIdAndDeletedAtIsNull(academyId);
        long pendingCourses = courseRepository.countByAcademyIdAndIsApprovedAndDeletedAtIsNull(
                academyId, ApprovalStatus.PENDING);
        long totalReviews = reviewRepository.countByAcademyIdAndDeletedAtIsNull(academyId);
        long pendingReviews = reviewRepository.countByAcademyIdAndApprovalStatusAndDeletedAtIsNull(
                academyId, ApprovalStatus.PENDING);

        return InstitutionDashboardStatsResponse.builder()
                .totalCourses(totalCourses)
                .pendingCourses(pendingCourses)
                .totalReviews(totalReviews)
                .pendingReviews(pendingReviews)
                .build();
    }

    /**
     * 계정 ID로 기관 ID 조회
     * 레이어 규칙 준수를 위해 컨트롤러에서 서비스로 이동
     * 
     * Soft Delete 준수: 삭제된 계정은 조회 불가
     * 
     * @param accountId 계정 ID
     * @return 기관 ID
     * @throws AccountNotFoundException 사용자를 찾을 수 없는 경우 (삭제된 계정 포함)
     * @throws BadRequestException 기관 정보가 없는 계정인 경우
     */
    @Override
    public Long getAcademyIdByAccountId(Long accountId) {
        // Soft Delete 준수: 삭제되지 않은 계정만 조회
        Account account = accountRepository.findByIdAndIsDeletedFalse(accountId)
                .orElseThrow(() -> new AccountNotFoundException("사용자를 찾을 수 없습니다."));
        if (account.getAcademyId() == null) {
            throw new BadRequestException("기관 정보가 없는 계정입니다.");
        }
        return account.getAcademyId();
    }
}
