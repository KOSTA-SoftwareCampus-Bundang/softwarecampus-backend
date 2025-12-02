package com.softwarecampus.backend.controller.user;

import com.softwarecampus.backend.dto.mypage.MyCommentResponseDTO;
import com.softwarecampus.backend.dto.mypage.MyPostResponseDTO;
import com.softwarecampus.backend.dto.mypage.MyStatsResponseDTO;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.ChangePasswordRequest;
import com.softwarecampus.backend.dto.user.UpdateProfileRequest;
import com.softwarecampus.backend.security.CustomUserDetails;
import com.softwarecampus.backend.service.user.profile.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지 API Controller
 * 
 * 엔드포인트:
 * - GET /api/mypage/profile: 프로필 조회
 * - PATCH /api/mypage/profile: 프로필 수정
 * - DELETE /api/mypage/account: 계정 삭제
 * - PUT /api/mypage/password: 비밀번호 변경 (이중 인증)
 * - GET /api/mypage/posts: 내가 쓴 글 목록
 * - GET /api/mypage/comments: 내가 쓴 댓글 목록
 * - GET /api/mypage/stats: 활동 통계
 * 
 * 인증: 모든 엔드포인트 JWT 토큰 필수
 */
@Slf4j
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final ProfileService profileService;

    /**
     * 프로필 조회
     * 
     * @param userDetails Spring Security 인증 정보
     * @return 200 OK + AccountResponse
     */
    @GetMapping("/profile")
    public ResponseEntity<AccountResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        log.info("프로필 조회 요청");

        AccountResponse response = profileService.getAccountByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * 프로필 수정
     * 
     * @param userDetails Spring Security 인증 정보
     * @param request     수정할 프로필 정보
     * @return 200 OK + AccountResponse
     */
    @PatchMapping("/profile")
    public ResponseEntity<AccountResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        String email = userDetails.getUsername();
        log.info("프로필 수정 요청");

        AccountResponse response = profileService.updateProfile(email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 계정 삭제 (소프트 삭제)
     * 
     * @param userDetails Spring Security 인증 정보
     * @return 204 No Content
     */
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        log.info("계정 삭제 요청");

        profileService.deleteAccount(email);
        return ResponseEntity.noContent().build();
    }

    /**
     * 비밀번호 변경 (이중 인증 - 로그인 사용자용)
     * 
     * 선행 조건:
     * 1. POST /api/auth/verify-password (현재 비밀번호 확인)
     * 2. POST /api/auth/email/send-change-code (인증 코드 발송)
     * 
     * @param userDetails Spring Security 인증 정보
     * @param request     인증 코드 및 새 비밀번호
     * @return 200 OK
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        String email = userDetails.getUsername();
        log.info("비밀번호 변경 요청");

        profileService.changePassword(email, request);
        return ResponseEntity.ok().build();
    }

    // ===== 활동 내역 API =====

    /**
     * 내가 쓴 글 목록 조회
     * 
     * @param userDetails Spring Security 인증 정보
     * @param pageable    페이징 정보 (page, size, sort)
     * @return 200 OK + Page<MyPostResponseDTO>
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/posts")
    public ResponseEntity<Page<MyPostResponseDTO>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("내가 쓴 글 목록 조회 요청: accountId={}", userDetails.getId());
        return ResponseEntity.ok(profileService.getMyPosts(userDetails.getId(), pageable));
    }

    /**
     * 내가 쓴 댓글 목록 조회
     * 
     * @param userDetails Spring Security 인증 정보
     * @param pageable    페이징 정보 (page, size, sort)
     * @return 200 OK + Page<MyCommentResponseDTO>
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/comments")
    public ResponseEntity<Page<MyCommentResponseDTO>> getMyComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("내가 쓴 댓글 목록 조회 요청: accountId={}", userDetails.getId());
        return ResponseEntity.ok(profileService.getMyComments(userDetails.getId(), pageable));
    }

    /**
     * 활동 통계 조회
     * 
     * @param userDetails Spring Security 인증 정보
     * @return 200 OK + MyStatsResponseDTO
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/stats")
    public ResponseEntity<MyStatsResponseDTO> getMyStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("활동 통계 조회 요청: accountId={}", userDetails.getId());
        return ResponseEntity.ok(profileService.getMyStats(userDetails.getId()));
    }
}
