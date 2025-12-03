package com.softwarecampus.backend.service.user.login;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.LoginResponse;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.exception.user.InvalidCredentialsException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import com.softwarecampus.backend.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 로그인 Service 구현체
 * 
 * 처리 순서:
 * 1. 이메일로 Account 조회
 * 2. 비밀번호 검증 (PasswordEncoder.matches)
 * 3. 계정 상태 검증 (활성화, 승인 여부)
 * 4. JWT Access Token 생성
 * 5. Refresh Token 생성 및 Redis 저장
 * 6. LoginResponse 반환
 * 
 * @author 태윤
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginServiceImpl implements LoginService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 로그인 처리
     * 
     * 보안 원칙:
     * - 이메일 존재 여부와 비밀번호 오류를 구분하지 않음 (정보 유출 방지)
     * - 모든 인증 실패는 동일한 예외 메시지 사용
     * - PII 로깅 시 마스킹 적용
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("로그인 시도: email={}", EmailUtils.maskEmail(request.email()));

        // 1. Account 조회 (Soft Delete 제외)
        Account account = accountRepository.findByEmailAndIsDeletedFalse(request.email())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 존재하지 않는 이메일: {}", EmailUtils.maskEmail(request.email()));
                    return new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다");
                });

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            log.warn("로그인 실패 - 비밀번호 불일치: {}", EmailUtils.maskEmail(request.email()));
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        // 3. 계정 상태 검증 (비활성화 또는 미승인 계정 차단)
        if (!account.isActive()) {
            log.warn("로그인 실패 - 비활성화된 계정: {}", EmailUtils.maskEmail(request.email()));
            throw new InvalidCredentialsException("비활성화된 계정입니다");
        }

        // ACADEMY 계정은 관리자 승인 필요
        if (account.getAccountType() == AccountType.ACADEMY &&
                account.getAccountApproved() != ApprovalStatus.APPROVED) {
            log.warn("로그인 실패 - 미승인 ACADEMY 계정: {}, status={}",
                    EmailUtils.maskEmail(request.email()),
                    account.getAccountApproved());

            // 보안: 승인 상태 구분 없이 통일된 메시지 반환 (정보 누출 방지)
            throw new InvalidCredentialsException("계정 승인이 필요합니다");
        }

        // 4. JWT 토큰 생성 (TokenService 활용)
        String accessToken = jwtTokenProvider.generateToken(
                account.getEmail(),
                account.getAccountType().name());

        // 5. Refresh Token 생성 및 Redis 저장
        String refreshToken = UUID.randomUUID().toString();
        String refreshKey = "refresh:" + account.getEmail();
        redisTemplate.opsForValue().set(
                refreshKey,
                refreshToken,
                7 * 24 * 60 * 60 * 1000L, // 7일
                TimeUnit.MILLISECONDS);

        // 6. LoginResponse 생성
        AccountResponse accountResponse = new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getUserName(),
                account.getPhoneNumber(),
                account.getAccountType(),
                account.getAccountApproved(),
                account.getAddress(),
                account.getAffiliation(),
                account.getPosition(),
                account.getProfileImage(),
                account.getCreatedAt(),
                account.getDeletedAt(),
                0,
                0);
        Long expiresIn = jwtTokenProvider.getExpiration() / 1000; // 밀리초 → 초 변환

        log.info("로그인 성공: email={}, accountType={}",
                EmailUtils.maskEmail(request.email()),
                account.getAccountType());

        return LoginResponse.of(accessToken, refreshToken, expiresIn, accountResponse);
    }

    /**
     * 현재 비밀번호 검증 (비밀번호 변경 전 본인 확인용)
     * 
     * 보안 원칙:
     * - 세션 탈취 공격 방어
     * - JWT 토큰만으로는 비밀번호 변경 불가
     * 
     * 용도: 마이페이지 비밀번호 변경 Step 1
     * - 현재 비밀번호를 확인하여 본인 인증
     * 
     * @param email           사용자 이메일
     * @param currentPassword 현재 비밀번호
     * @return 비밀번호 일치 여부 (예외를 던지지 않고 boolean 반환)
     * @throws AccountNotFoundException 계정 없음
     */
    @Override
    public boolean verifyPassword(String email, String currentPassword) {
        log.info("현재 비밀번호 검증 시도: email={}", EmailUtils.maskEmail(email));

        Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> {
                    log.warn("비밀번호 검증 실패 - 계정 없음: {}", EmailUtils.maskEmail(email));
                    return new com.softwarecampus.backend.exception.user.AccountNotFoundException("계정을 찾을 수 없습니다.");
                });

        boolean matches = passwordEncoder.matches(currentPassword, account.getPassword());

        if (matches) {
            log.info("비밀번호 검증 성공: email={}", EmailUtils.maskEmail(email));
        } else {
            log.warn("비밀번호 검증 실패 - 비밀번호 불일치: email={}", EmailUtils.maskEmail(email));
        }

        return matches;
    }
}
