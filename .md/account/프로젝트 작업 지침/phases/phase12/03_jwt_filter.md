# Phase 12-3: JwtAuthenticationFilter 구현

## 📌 개요

HTTP 요청의 `Authorization` 헤더에서 JWT 토큰을 추출하고, 유효성을 검증하여 Spring Security의 인증 컨텍스트에 설정합니다.

---

## 🔧 구현 내용

### JwtAuthenticationFilter

**파일:** `src/main/java/com/softwarecampus/backend/security/JwtAuthenticationFilter.java`

**상속:** `OncePerRequestFilter`
- 요청당 한 번만 실행되는 필터
- `doFilterInternal()` 메서드 구현 필요

**역할:**
1. `Authorization` 헤더에서 JWT 토큰 추출
2. 토큰 유효성 검증
3. 토큰에서 사용자 정보 추출
4. `SecurityContext`에 인증 정보 설정

---

## 📝 핵심 로직

### 1. 토큰 추출

```java
private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);  // "Bearer " 제거
    }
    
    return null;
}
```

**Authorization 헤더 형식:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 2. 토큰 검증 및 인증 설정

```java
@Override
protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
) throws ServletException, IOException {
    
    try {
        String token = resolveToken(request);
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
            
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    } catch (Exception e) {
        log.error("Cannot set user authentication: {}", e.getMessage());
    }
    
    filterChain.doFilter(request, response);
}
```

---

## 🔐 SecurityContext 설정

### UsernamePasswordAuthenticationToken

**생성자 파라미터:**
1. `principal`: 인증된 사용자 (`UserDetails`)
2. `credentials`: 자격 증명 (JWT 사용 시 `null`)
3. `authorities`: 권한 목록

**설정 과정:**
```java
// 1. Authentication 객체 생성
Authentication auth = new UsernamePasswordAuthenticationToken(
    userDetails,     // principal
    null,            // credentials (JWT는 토큰 자체가 인증)
    authorities      // 권한 목록
);

// 2. 추가 정보 설정 (IP 주소, 세션 ID 등)
auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

// 3. SecurityContext에 저장
SecurityContextHolder.getContext().setAuthentication(auth);
```

**결과:**
- `@AuthenticationPrincipal`로 컨트롤러에서 사용자 정보 접근 가능
- `SecurityContextHolder.getContext().getAuthentication()`으로 전역 접근 가능

---

## 🚨 예외 처리

### 1. 토큰이 없는 경우
```java
if (token == null) {
    // 필터 체인 계속 진행 (인증 없이)
    // SecurityConfig의 permitAll() 설정에 따라 허용/차단
    filterChain.doFilter(request, response);
    return;
}
```

### 2. 토큰이 유효하지 않은 경우
```java
if (!jwtTokenProvider.validateToken(token)) {
    // 인증 설정 안 함
    // → SecurityContext에 인증 정보 없음
    // → JwtAuthenticationEntryPoint를 통해 401 Unauthorized 반환
    filterChain.doFilter(request, response);
    return;
}
```

### 3. 예외 발생 시
```java
catch (Exception e) {
    log.error("Cannot set user authentication: {}", e.getMessage());
    // 인증 실패 처리
    // 필터 체인은 계속 진행 (다음 필터로)
}
```

**주의사항:**
- Filter에서 발생한 예외는 `GlobalExceptionHandler`로 전파되지 않음
- 로깅 후 필터 체인 계속 진행 (다음 필터로)
- 실제 접근 제어는 `SecurityConfig`의 설정에 따름

---

## 🔗 SecurityConfig 연동

### Filter 등록 순서

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // ...
        .addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );
    
    return http.build();
}
```

**필터 체인 순서:**
1. `JwtAuthenticationFilter` ← JWT 토큰 검증
2. `UsernamePasswordAuthenticationFilter` ← 폼 로그인 (우리는 사용 안 함)
3. `FilterSecurityInterceptor` ← 권한 검증

---

## 🧪 동작 흐름 예시

### 시나리오 1: 정상 인증

```http
GET /api/v1/mypage/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

1. ✅ `resolveToken()`: 토큰 추출 성공
2. ✅ `validateToken()`: 토큰 유효성 검증 성공
3. ✅ `getEmailFromToken()`: 이메일 추출 (`user@example.com`)
4. ✅ `loadUserByUsername()`: UserDetails 로드 성공
5. ✅ `SecurityContext`에 인증 정보 설정
6. ✅ 컨트롤러 접근 허용 (200 OK)

### 시나리오 2: 토큰 없음

```http
GET /api/v1/mypage/profile
(Authorization 헤더 없음)
```

1. ⚠️ `resolveToken()`: 토큰 없음 (null)
2. ⚠️ `SecurityContext`에 인증 정보 미설정
3. ❌ `SecurityConfig`: `/api/v1/mypage/**`는 인증 필요
4. ❌ `JwtAuthenticationEntryPoint`를 통해 401 Unauthorized 반환

### 시나리오 3: 만료된 토큰

```http
GET /api/v1/mypage/profile
Authorization: Bearer expired_token
```

1. ✅ `resolveToken()`: 토큰 추출 성공
2. ❌ `validateToken()`: 만료 토큰 검증 실패
3. ⚠️ `SecurityContext`에 인증 정보 미설정
4. ❌ `JwtAuthenticationEntryPoint`를 통해 401 Unauthorized 반환

---

## ✅ 검증 포인트

1. ✅ `OncePerRequestFilter` 상속
2. ✅ `JwtTokenProvider`, `CustomUserDetailsService` 의존성 주입
3. ✅ `doFilterInternal()` 메서드 구현
4. ✅ `resolveToken()` 메서드로 토큰 추출
5. ✅ `validateToken()`으로 토큰 검증
6. ✅ `SecurityContext`에 인증 정보 설정
7. ✅ 예외 발생 시 로깅 및 필터 체인 계속 진행
8. ✅ 컴파일 성공 (`mvn clean compile`)

---

## 📝 참고 사항

### OncePerRequestFilter vs Filter

| 항목 | OncePerRequestFilter | Filter (인터페이스) |
|------|---------------------|-------------------|
| 실행 횟수 | 요청당 1번 보장 | 중복 실행 가능 |
| 메서드 | `doFilterInternal()` | `doFilter()` |
| Forward/Include | 자동 스킵 | 수동 처리 필요 |
| Spring 통합 | Spring Bean 주입 가능 | 제한적 |

**선택 이유:** JWT 검증은 요청당 한 번만 실행되어야 하므로 `OncePerRequestFilter` 사용

---

## 🔗 다음 단계

JwtAuthenticationFilter 구현 완료 후:
- **Phase 12-4**: SecurityConfig 완성 (필터 등록, 경로 설정)
