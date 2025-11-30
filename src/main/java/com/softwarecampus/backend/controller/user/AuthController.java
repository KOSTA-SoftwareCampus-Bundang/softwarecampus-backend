package com.softwarecampus.backend.controller.user;

import com.softwarecampus.backend.dto.auth.RefreshTokenRequest;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.LoginRequest;
import com.softwarecampus.backend.dto.user.LoginResponse;
import com.softwarecampus.backend.dto.user.MessageResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.dto.user.VerifyPasswordRequest;
import com.softwarecampus.backend.dto.user.VerifyPasswordResponse;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import com.softwarecampus.backend.service.auth.TokenService;
import com.softwarecampus.backend.service.user.login.LoginService;
import com.softwarecampus.backend.service.user.signup.SignupService;
import com.softwarecampus.backend.util.EmailUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * 회원가입 및 인증 API 컨트롤러
 * 
 * 엔드포인트:
 * - POST /api/auth/signup: 회원가입
 * - POST /api/auth/login: 로그인
 * - GET /api/auth/check-email: 이메일 중복 확인
 * - POST /api/auth/refresh: Access Token 갱신
 * 
 * RESTful 원칙:
 * - HTTP 201 Created + Location 헤더 (리소스 URI)
 * - Bean Validation (@Valid)
 * - RFC 9457 ProblemDetail 오류 응답
 * 
 * @author 태윤
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;
    private final LoginService loginService;
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 API
     * 
     * @param request 회원가입 요청 (email, password, userName, phoneNumber,
     *                address, affiliation, position, accountType, academyId)
     * @return 201 Created + Location 헤더 + AccountResponse
     * 
     * @throws InvalidInputException   400 - 이메일 형식 오류 (RFC 5322, RFC 1035)
     * @throws DuplicateEmailException 409 - 이메일 중복
     * @throws InvalidInputException   400 - 전화번호 중복
     * @throws InvalidInputException   400 - ADMIN 계정 회원가입 시도
     * @throws InvalidInputException   400 - ACADEMY 타입 academyId 누락
     */
    @PostMapping("/signup")
    public ResponseEntity<AccountResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("회원가입 요청 - accountType: {}", request.accountType());
        if (log.isDebugEnabled()) {
            log.debug("회원가입 요청 - affiliation: {}, academyId: {}",
                    request.affiliation() != null ? "있음" : "없음",
                    request.academyId() != null ? "있음" : "없음");
        }

        AccountResponse response = signupService.signup(request);

        // Location 헤더 생성 (RESTful)
        URI location = URI.create("/api/accounts/" + response.id());

        log.info("회원가입 성공 - accountId: {}, accountType: {}, approvalStatus: {}",
                response.id(), response.accountType(), response.approvalStatus());

        return ResponseEntity
                .created(location)
                .body(response);
    }

    /**
     * 이메일 중복 확인 API
     * 
     * ⚠️ 보안 고려사항:
     * - Rate Limiting 필수 (이메일 열거 공격 방지)
     * - IP 기반 제한 권장: 60 req/min per IP
     * - 로깅 및 모니터링 필요
     * 
     * 선택사항: Rate Limiter 추후 구현
     * - 이메일 중복 체크 API에 Rate Limiting 적용 권장
     * - 구현 시 Bucket4j 또는 Spring Cloud Gateway 사용 고려
     * - IP 기반 제한: @RateLimit(permits=60, window=1, unit=MINUTES)
     * - 초과 시: 429 Too Many Requests 응답
     * 
     * @param email 확인할 이메일
     * @return 200 OK - 사용 가능 여부
     * 
     * @throws InvalidInputException        400 - 이메일 형식 오류
     * @throws ConstraintViolationException 400 - Bean Validation 실패 (@Email)
     */
    @GetMapping("/check-email")
    public ResponseEntity<MessageResponse> checkEmail(
            @RequestParam @Email(message = "올바른 이메일 형식이 아닙니다.") String email) {
        log.info("이메일 중복 확인 요청: email={}", EmailUtils.maskEmail(email));

        boolean available = signupService.isEmailAvailable(email);

        String message = available
                ? "사용 가능한 이메일입니다."
                : "이미 사용 중인 이메일입니다.";

        log.info("이메일 중복 확인 결과 - available: {}", available);

        return ResponseEntity.ok(MessageResponse.of(message));
    }

    /**
     * 현재 비밀번호 확인 API
     * 
     * 용도: 마이페이지 비밀번호 변경 Step 1
     * - 현재 비밀번호를 확인하여 본인 인증
     * - 성공 시 이메일 인증 코드 발송 진행 가능
     * 
     * @param userDetails Spring Security 인증 정보 (JWT에서 추출)
     * @param request     현재 비밀번호
     * @return 200 OK - 확인 결과
     * 
     * @throws AccountNotFoundException 404 - 계정 없음
     */
    @PostMapping("/verify-password")
    public ResponseEntity<VerifyPasswordResponse> verifyPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VerifyPasswordRequest request) {

        String email = userDetails.getUsername();
        log.info("현재 비밀번호 확인 요청");

        // 계정 조회
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(
                        () -> new com.softwarecampus.backend.exception.user.AccountNotFoundException("계정을 찾을 수 없습니다."));

        // 비밀번호 검증
        boolean matches = passwordEncoder.matches(request.getCurrentPassword(), account.getPassword());

        if (matches) {
            log.info("비밀번호 확인 성공");
            return ResponseEntity.ok(VerifyPasswordResponse.success());
        } else {
            log.warn("비밀번호 확인 실패 - 불일치");
            return ResponseEntity.badRequest().body(VerifyPasswordResponse.failure());
        }
    }

    /**
     * 로그인 API
     * 
     * @param request 로그인 요청 (email, password)
     * @return 200 OK + LoginResponse (accessToken, refreshToken, account)
     * 
     * @throws com.softwarecampus.backend.exception.user.InvalidCredentialsException 401
     *                                                                               -
     *                                                                               이메일
     *                                                                               없음
     *                                                                               또는
     *                                                                               비밀번호
     *                                                                               불일치
     * @throws com.softwarecampus.backend.exception.user.InvalidCredentialsException 401
     *                                                                               -
     *                                                                               비활성화된
     *                                                                               계정
     * @throws com.softwarecampus.backend.exception.user.InvalidCredentialsException 401
     *                                                                               -
     *                                                                               미승인
     *                                                                               ACADEMY
     *                                                                               계정
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("로그인 API 호출");

        LoginResponse response = loginService.login(request);

        log.info("로그인 성공 - accountType: {}", response.account().accountType());

        return ResponseEntity.ok(response);
    }

    /**
     * Access Token 갱신 API
     * 
     * 보안 검증:
     * - 현재 인증된 사용자의 이메일과 요청 이메일이 일치하는지 확인
     * - Refresh Token 유효성 검증 (Redis 조회)
     * 
     * @param request Refresh Token 갱신 요청 (refreshToken, email)
     * @return 200 OK - 새로운 Access Token
     * 
     * @throws IllegalArgumentException                                     401 -
     *                                                                      Refresh
     *                                                                      Token
     *                                                                      유효하지 않음
     * @throws org.springframework.web.bind.MethodArgumentNotValidException 400 -
     *                                                                      Bean
     *                                                                      Validation
     *                                                                      실패
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            // 보안 검증: 현재 인증된 사용자와 요청 이메일 일치 여부 확인
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // 인증되지 않은 경우 또는 익명 사용자인 경우
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                log.warn("Unauthenticated refresh attempt for email: {}", EmailUtils.maskEmail(request.email()));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String authenticatedEmail = auth.getName();

            // 인증된 이메일과 요청 이메일이 다른 경우 (보안 위협)
            if (!authenticatedEmail.equals(request.email())) {
                log.warn("Email mismatch - authenticated: {}, requested: {}",
                        EmailUtils.maskEmail(authenticatedEmail),
                        EmailUtils.maskEmail(request.email()));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Refresh Token으로 새 Access Token 발급
            String newAccessToken = tokenService.refreshAccessToken(
                    request.email(),
                    request.refreshToken());

            log.info("Access Token refreshed for user: {}", EmailUtils.maskEmail(request.email()));

            // JWT 실제 만료 시간을 동적으로 가져오기 (밀리초 → 초)
            long expiresInSeconds = jwtTokenProvider.getExpiration() / 1000;

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "expiresIn", expiresInSeconds,
                    "tokenType", "Bearer"));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid refresh token for user: {}", EmailUtils.maskEmail(request.email()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
