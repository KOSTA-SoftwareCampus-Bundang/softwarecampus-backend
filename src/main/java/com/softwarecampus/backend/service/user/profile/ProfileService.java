package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.ChangePasswordRequest;
import com.softwarecampus.backend.dto.user.ResetPasswordRequest;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;

/**
 * 계정 조회 및 프로필 관리 Service 인터페이스
 */
public interface ProfileService {

    /**
     * ID로 계정 조회
     * 
     * @param accountId 계정 ID
     * @return 계정 정보
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException 계정이
     *                                                                            존재하지
     *                                                                            않는
     *                                                                            경우
     */
    AccountResponse getAccountById(Long accountId);

    /**
     * 이메일로 계정 조회
     * 
     * @param email 이메일
     * @return 계정 정보
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException 계정이
     *                                                                            존재하지
     *                                                                            않는
     *                                                                            경우
     */
    AccountResponse getAccountByEmail(String email);

    /**
     * 프로필 수정
     * 
     * @param email   이메일
     * @param request 수정할 프로필 정보
     * @return 수정된 계정 정보
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException          계정이
     *                                                                                     존재하지
     *                                                                                     않는
     *                                                                                     경우
     * @throws com.softwarecampus.backend.exception.user.PhoneNumberAlreadyExistsException 전화번호
     *                                                                                     중복
     */
    AccountResponse updateProfile(String email, UpdateProfileRequest request);

    /**
     * 계정 삭제 (소프트 삭제)
     * 
     * @param email 이메일
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException 계정이
     *                                                                            존재하지
     *                                                                            않는
     *                                                                            경우
     */
    void deleteAccount(String email);

    /**
     * 비밀번호 재설정 (이메일 인증 코드 검증) - 비로그인용
     * 
     * @param email   이메일
     * @param request 인증 코드 및 새 비밀번호
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException          계정이
     *                                                                                     존재하지
     *                                                                                     않는
     *                                                                                     경우
     * @throws com.softwarecampus.backend.exception.email.EmailVerificationException       인증
     *                                                                                     코드가
     *                                                                                     존재하지
     *                                                                                     않는
     *                                                                                     경우
     * @throws com.softwarecampus.backend.exception.email.VerificationCodeExpiredException 인증
     *                                                                                     코드가
     *                                                                                     만료된
     *                                                                                     경우
     * @throws com.softwarecampus.backend.exception.email.TooManyAttemptsException         인증
     *                                                                                     시도
     *                                                                                     횟수
     *                                                                                     초과
     */
    void resetPassword(String email, ResetPasswordRequest request);

    /**
     * 비밀번호 변경 (이메일 인증 코드 검증) - 로그인 사용자용
     * 
     * 보안 요구사항:
     * - 로그인 상태에서만 호출 가능 (JWT 토큰 필요)
     * - 이메일 인증 코드 검증 필수 (PASSWORD_CHANGE 타입)
     * - 이중 인증 방식: JWT + 이메일 인증
     * 
     * 사용 시나리오:
     * 1. POST /api/auth/verify-password - 현재 비밀번호 확인
     * 2. POST /api/auth/email/send-change-code - 이메일 인증 코드 발송
     * 3. PATCH /api/mypage/password - 인증 코드 + 새 비밀번호로 변경 (이 메서드)
     * 
     * @param email   이메일
     * @param request 인증 코드 및 새 비밀번호
     * @throws com.softwarecampus.backend.exception.user.AccountNotFoundException          계정이
     *                                                                                     존재하지
     *                                                                                     않는
     *                                                                                     경우
     * @throws com.softwarecampus.backend.exception.email.EmailVerificationException       인증
     *                                                                                     코드가
     *                                                                                     존재하지
     *                                                                                     않는
     *                                                                                     경우
     * @throws com.softwarecampus.backend.exception.email.VerificationCodeExpiredException 인증
     *                                                                                     코드가
     *                                                                                     만료된
     *                                                                                     경우
     * @throws com.softwarecampus.backend.exception.email.TooManyAttemptsException         인증
     *                                                                                     시도
     *                                                                                     횟수
     *                                                                                     초과
     */
    void changePassword(String email, ChangePasswordRequest request);
}
