package com.softwarecampus.backend.controller.user;

import com.softwarecampus.backend.dto.auth.RefreshTokenRequest;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.MessageResponse;
import com.softwarecampus.backend.dto.user.SignupRequest;
import com.softwarecampus.backend.security.jwt.JwtTokenProvider;
import com.softwarecampus.backend.service.auth.TokenService;
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
 * - GET /api/auth/check-email: 이메일 중복 확인
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
    private final TokenService tokenService;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * 회원가입 API
     * 
     * @param request 회원가입 요청 (email, password, userName, phoneNumber, 
     *                address, affiliation, position, accountType, academyId)
     * @return 201 Created + Location 헤더 + AccountResponse
     * 
     * @throws InvalidInputException 400 - 이메일 형식 오류 (RFC 5322, RFC 1035)
     * @throws DuplicateEmailException 409 - 이메일 중복
     * @throws InvalidInputException 400 - 전화번호 중복
     * @throws InvalidInputException 400 - ADMIN 계정 회원가입 시도
     * @throws InvalidInputException 400 - ACADEMY 타입 academyId 누락
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
        URI location = URI.create("/api/v1/accounts/" + response.id());
        
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
     * @throws InvalidInputException 400 - 이메일 형식 오류
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
     * Access Token 갱신 API
     * 
     * 보안 검증:
     * - 현재 인증된 사용자의 이메일과 요청 이메일이 일치하는지 확인
     * - Refresh Token 유효성 검증 (Redis 조회)
     * 
     * @param request Refresh Token 갱신 요청 (refreshToken, email)
     * @return 200 OK - 새로운 Access Token
     * 
     * @throws IllegalArgumentException 401 - Refresh Token 유효하지 않음
     * @throws org.springframework.web.bind.MethodArgumentNotValidException 400 - Bean Validation 실패
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
                request.refreshToken()
            );
            
            log.info("Access Token refreshed for user: {}", EmailUtils.maskEmail(request.email()));
            
            // JWT 실제 만료 시간을 동적으로 가져오기 (밀리초 → 초)
            long expiresInSeconds = jwtTokenProvider.getExpiration() / 1000;
            
            return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "expiresIn", expiresInSeconds,
                "tokenType", "Bearer"
            ));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid refresh token for user: {}", EmailUtils.maskEmail(request.email()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
