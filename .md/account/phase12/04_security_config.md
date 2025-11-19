# Phase 12-4: SecurityConfig 완성

## 📌 개요

Spring Security의 보안 설정을 완성하여 JWT 인증 필터를 등록하고, 엔드포인트별 접근 권한을 설정합니다.

---

## 🔧 구현 내용

### SecurityConfig

**파일:** `src/main/java/com/softwarecampus/backend/config/SecurityConfig.java`

**역할:**
- SecurityFilterChain 구성
- JWT Filter 등록
- 엔드포인트별 권한 설정
- CORS 활성화
- CSRF disable

---

## 📝 핵심 설정

### 1. SecurityFilterChain 전체 구성

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // JWT 사용으로 CSRF 비활성화
            .cors(cors -> cors.disable())  // CORS는 WebConfig에서 처리
            
            .authorizeHttpRequests(auth -> auth
                // 인증 불필요 (public)
                .requestMatchers("/api/v1/auth/signup").permitAll()
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/check-email").permitAll()
                
                // 인증 필요
                .requestMatchers("/api/v1/mypage/**").authenticated()
                
                // 나머지 요청은 인증 필요 (기본값)
                .anyRequest().authenticated()
            )
            
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 세션 사용 안 함
            )
            
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
```

---

## 🔐 주요 설정 설명

### 1. CSRF Disable

```java
.csrf(csrf -> csrf.disable())
```

**이유:**
- JWT는 쿠키가 아닌 `Authorization` 헤더로 전송
- CSRF 공격은 쿠키 기반 인증에서 발생
- JWT 사용 시 CSRF 보호 불필요

### 2. CORS 설정

```java
.cors(cors -> cors.disable())
```

**이유:**
- CORS는 `WebConfig`에서 이미 설정 (Phase 11)
- SecurityConfig에서는 비활성화하여 중복 방지

**참고:** Phase 11의 `WebConfig.java`:
```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:3000")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
        .allowedHeaders("*")
        .allowCredentials(true);
}
```

### 3. 엔드포인트별 권한 설정

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/signup").permitAll()
    .requestMatchers("/api/v1/auth/login").permitAll()
    .requestMatchers("/api/v1/auth/check-email").permitAll()
    .requestMatchers("/api/v1/mypage/**").authenticated()
    .anyRequest().authenticated()
)
```

**설정 규칙:**
- `permitAll()`: 인증 없이 접근 가능
- `authenticated()`: 인증 필요 (JWT 토큰 필요)
- `hasRole()`: 특정 권한 필요 (예: `hasRole("ADMIN")`)

**주의사항:**
- 순서가 중요! 먼저 선언된 규칙이 우선 적용
- `anyRequest()`는 마지막에 배치

### 4. 세션 정책

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

**SessionCreationPolicy 옵션:**
- `STATELESS`: 세션 사용 안 함 (JWT 사용 시)
- `ALWAYS`: 항상 세션 생성
- `IF_REQUIRED`: 필요 시 세션 생성 (기본값)
- `NEVER`: 세션 생성 안 함 (기존 세션은 사용)

**JWT 사용 시 STATELESS 필수:**
- 서버에서 세션 상태를 유지하지 않음
- 확장성 향상 (Stateless 서버)

### 5. JWT Filter 등록

```java
.addFilterBefore(
    jwtAuthenticationFilter,
    UsernamePasswordAuthenticationFilter.class
)
```

**필터 체인 순서:**
1. `JwtAuthenticationFilter` ← JWT 토큰 검증 및 인증 설정
2. `UsernamePasswordAuthenticationFilter` ← 폼 로그인 (사용 안 함)
3. `FilterSecurityInterceptor` ← 권한 검증

**왜 Before인가?**
- 폼 로그인보다 JWT 인증을 먼저 처리
- JWT 토큰이 있으면 폼 로그인 시도 안 함

---

## 🧩 Bean 설정

### 1. PasswordEncoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**역할:**
- 비밀번호 암호화 (`signup` 시)
- 비밀번호 검증 (`login` 시)

**이미 Phase 2에서 설정됨:**
- `SecurityConfig.java`에서 이미 Bean 등록
- 중복 등록 방지 (기존 코드 유지)

### 2. AuthenticationManager

```java
@Bean
public AuthenticationManager authenticationManager(
    AuthenticationConfiguration authConfig
) throws Exception {
    return authConfig.getAuthenticationManager();
}
```

**역할:**
- 로그인 시 인증 처리
- `login()` 메서드에서 사용 (Phase 14)

---

## 🔍 동작 흐름

### 시나리오 1: 회원가입 요청

```http
POST /api/v1/auth/signup
(JWT 토큰 없음)
```

1. ✅ `JwtAuthenticationFilter`: 토큰 없음 → 인증 설정 안 함
2. ✅ `SecurityFilterChain`: `/api/v1/auth/signup`는 `permitAll()`
3. ✅ `AuthController.signup()` 실행
4. ✅ 200 OK 반환

### 시나리오 2: 마이페이지 접근 (인증 성공)

```http
GET /api/v1/mypage/profile
Authorization: Bearer valid_jwt_token
```

1. ✅ `JwtAuthenticationFilter`: 토큰 검증 성공 → `SecurityContext`에 인증 설정
2. ✅ `SecurityFilterChain`: `/api/v1/mypage/**`는 `authenticated()`
3. ✅ 인증 정보 있음 → 접근 허용
4. ✅ `MyPageController.getProfile()` 실행
5. ✅ 200 OK 반환

### 시나리오 3: 마이페이지 접근 (인증 실패)

```http
GET /api/v1/mypage/profile
(JWT 토큰 없음)
```

1. ⚠️ `JwtAuthenticationFilter`: 토큰 없음 → 인증 설정 안 함
2. ❌ `SecurityFilterChain`: `/api/v1/mypage/**`는 `authenticated()`
3. ❌ 인증 정보 없음 → 접근 거부
4. ❌ 403 Forbidden 반환

---

## 🚨 예외 처리

### 1. 인증 실패 (401 Unauthorized)
- 발생: 잘못된 자격 증명 (로그인 실패)
- 처리: `AuthenticationEntryPoint`에서 처리 (기본 제공)

### 2. 접근 거부 (403 Forbidden)
- 발생: 인증은 성공했으나 권한 부족
- 처리: `AccessDeniedHandler`에서 처리 (기본 제공)

### 커스텀 예외 처리 (선택사항)

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint(customAuthenticationEntryPoint)
    .accessDeniedHandler(customAccessDeniedHandler)
)
```

**Phase 12에서는 기본 처리 사용:**
- 커스텀 핸들러는 필요 시 추가 (선택사항)

---

## ✅ 검증 포인트

1. ✅ `@EnableWebSecurity` 어노테이션 추가
2. ✅ `JwtAuthenticationFilter` 의존성 주입
3. ✅ `SecurityFilterChain` Bean 생성
4. ✅ CSRF disable
5. ✅ 세션 정책 STATELESS
6. ✅ JWT Filter 등록 (`addFilterBefore`)
7. ✅ 엔드포인트별 권한 설정 (`permitAll`, `authenticated`)
8. ✅ `PasswordEncoder`, `AuthenticationManager` Bean 등록
9. ✅ 컴파일 성공 (`mvn clean compile`)
10. ✅ 기존 테스트 90/90 유지 (`mvn test`)

---

## 📝 Phase 11 vs Phase 12 비교

| 항목 | Phase 11 (기본 Security) | Phase 12 (JWT 완성) |
|------|------------------------|-------------------|
| 인증 방식 | PasswordEncoder만 | JWT 토큰 기반 |
| Filter | 없음 | JwtAuthenticationFilter |
| 엔드포인트 | 모두 허용 (permitAll) | 세밀한 권한 설정 |
| 세션 | 기본값 | STATELESS |
| CSRF | 활성화 | 비활성화 |

---

## 🔗 다음 단계

SecurityConfig 완성 후:
- **빌드 검증**: `mvn clean compile`
- **테스트 검증**: `mvn test` (90/90 유지)
- **Phase 13**: JWT + Security 단위 테스트 작성 (15-20개)
