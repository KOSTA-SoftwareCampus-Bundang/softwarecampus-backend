package com.softwarecampus.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * Spring Security 설정
 * JWT 기반 Stateless 인증 구현
 * 
 * @since 2025-11-19
 */
@Configuration
@EnableWebSecurity
//@PreAuthorize 사용하기 위해 추가
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 보안 필터 체인 설정
     * - JWT 기반 인증 사용
     * - CSRF 비활성화 (JWT는 CSRF 공격에 안전)
     * - CORS는 WebConfig에서 처리
     * - Session은 STATELESS (서버에 세션 저장하지 않음)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화: JWT 사용으로 불필요
                .csrf(csrf -> csrf.disable())

                // CORS 설정은 WebConfig에 위임
                .cors(cors -> {
                })

                // 엔드포인트별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,"/api/boards").authenticated()
                        .requestMatchers(new RegexRequestMatcher("/api/boards/\\d+","PATCH")).authenticated()
                        .requestMatchers(new RegexRequestMatcher("/api/boards/\\d+","DELETE")).authenticated()
                        .requestMatchers(new RegexRequestMatcher("/api/boards/\\d+/comments","POST")).authenticated()
                        .requestMatchers(new RegexRequestMatcher("/api/boards/\\d+/comments/\\d+",null)).authenticated()
                        .requestMatchers(new RegexRequestMatcher("/api/boards/\\d+/recommends",null)).authenticated()
                        .requestMatchers(new RegexRequestMatcher("/api/boards/\\d+/comments/\\d+/recommends",null)).authenticated()
                        // 인증 불필요 (누구나 접근 가능)
                        .requestMatchers(
                                "/api/auth/**", // 회원가입, 로그인
                                "/api/academies/**", // 학원 목록 조회
                                "/api/courses/**", // 강좌 목록 조회
                                "/api/home/**", // 메인페이지 데이터
                                "/api/boards/**", // 커뮤니티 게시글
                                "/swagger-ui/**", // Swagger UI
                                "/swagger-ui.html", // Swagger UI 진입점
                                "/v3/api-docs/**", // OpenAPI 문서
                                "/api-docs/**", // API 문서
                                "/webjars/**", // Swagger 리소스
                                "/error")
                        .permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated())

                // 인증 실패 시 401 Unauthorized 반환
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // Session을 사용하지 않음 (JWT 기반)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 암호화 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
