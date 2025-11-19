# CORS 설정 상세

## 📌 목적
프론트엔드와 백엔드 간 Cross-Origin 요청 허용

---

## 🎯 설계

### 클래스: `WebConfig`
**패키지:** `com.softwarecampus.backend.config`
**역할:** CORS 정책 설정

---

## 📋 설정 내용

### 1. 허용할 Origin
```
http://localhost:${FRONTEND_PORT}
```
- 환경변수 `FRONTEND_PORT`에서 포트 읽기 (기본값: 3000)
- 프로덕션 환경에서는 실제 도메인으로 변경 필요

### 2. 허용할 HTTP 메서드
```
GET, POST, PUT, PATCH, DELETE, OPTIONS
```

### 3. 허용할 헤더
```
Authorization, Content-Type, X-Requested-With
```

### 4. 자격증명 허용
```
allowCredentials = true
```
- 쿠키/인증 헤더 전송 허용

### 5. Preflight 캐시
```
maxAge = 3600 (1시간)
```

---

## 💡 구현 방법

### Option A: `WebMvcConfigurer` 구현 (권장)
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${FRONTEND_PORT:3000}")
    private String frontendPort;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:" + frontendPort)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### Option B: `CorsConfigurationSource` Bean (Spring Security 연동)
```java
@Configuration
public class WebConfig {
    
    @Value("${FRONTEND_PORT:3000}")
    private String frontendPort;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:" + frontendPort));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

**선택 기준:**
- Phase 11: Option A (간단)
- Phase 12 (Security 추가 후): Option B로 마이그레이션 고려

---

## ⚠️ 주의사항

### 1. 환경변수 관리

본 프로젝트는 `.env` 파일을 사용하여 환경변수를 관리합니다 (dotenv 라이브러리 필요).

**구조:**
1. `.env` 파일에 실제 값 정의
2. `application.properties`에서 환경변수 참조
3. Java 코드에서 프로퍼티 값 주입

**예시:**

`.env` 파일 (gitignore 대상):
```properties
FRONTEND_PORT=3000
```

`application.properties` (또는 직접 환경변수 참조):
```properties
# Option 1: application.properties에서 재정의
frontend.port=${FRONTEND_PORT:3000}

# Option 2: Java 코드에서 직접 환경변수 참조 (본 프로젝트 방식)
```

Java 코드:
```java
@Value("${FRONTEND_PORT:3000}")  // 환경변수 직접 참조
private String frontendPort;
```

**참고:** 
- `.env` 방식은 표준 Spring Boot 기능이 아니므로 `spring-dotenv` 또는 유사 라이브러리가 필요합니다.
- `.env` 파일은 `.gitignore`에 포함되어 GitHub에 업로드되지 않습니다.
- 프로덕션 환경에서는 시스템 환경변수 또는 CI/CD secrets를 통해 값을 주입합니다

### 2. 프로덕션 설정
현재는 개발 환경만 고려. 프로덕션에서는:
```java
// 환경별 분기 또는 프로파일 사용
.allowedOrigins(
    "http://localhost:" + frontendPort,
    "https://softwarecampus.com"  // 프로덕션 도메인
)
```

### 3. Security와 충돌 방지
- Phase 12에서 `SecurityConfig`에 `cors()` 활성화 필요
- 두 설정이 충돌하지 않도록 주의

---

## ✅ 검증 방법

### 1. 빌드 확인
```bash
./mvnw clean compile
```

### 2. 애플리케이션 실행
```bash
./mvnw spring-boot:run
```

### 3. CORS 헤더 확인 (curl)
```bash
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     http://localhost:8081/api/v1/auth/signup \
     -v
```

**예상 응답 헤더:**
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Credentials: true
```

---

## 📚 참고

- [Spring CORS 공식 문서](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)
- [MDN CORS 가이드](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
