# Phase 12-1: JWT Provider 구현

## 📌 개요

JWT(JSON Web Token) 토큰 생성 및 검증 로직을 구현합니다.

---

## 🔧 구현 내용

### 1. JwtProperties (설정 클래스)

**파일:** `src/main/java/com/softwarecampus/backend/security/jwt/JwtProperties.java`

**역할:**
- `application.properties`에서 JWT 설정 읽기
- `@ConfigurationProperties("jwt")` 사용

**필드:**
```java
private String secret;        // JWT 서명 키
private long expiration;      // 만료 시간 (밀리초)
private String issuer;        // 발급자 (softwarecampus)
```

**주의사항:**
- `@ConfigurationProperties` 사용 시 `@Component` 또는 `@EnableConfigurationProperties` 필요
- Lombok `@Data` 사용으로 Getter/Setter 자동 생성

---

### 2. JwtTokenProvider (핵심 로직)

**파일:** `src/main/java/com/softwarecampus/backend/security/jwt/JwtTokenProvider.java`

**역할:**
- JWT 토큰 생성
- JWT 토큰 검증
- Claims 추출 (email, role)

**주요 메서드:**

#### 1) `generateToken(String email, String role)`
- **반환:** JWT 토큰 문자열
- **Claims:**
  - `sub`: 이메일 (주체)
  - `role`: 권한 (USER, ACADEMY, ADMIN)
  - `iat`: 발급 시간
  - `exp`: 만료 시간
  - `iss`: 발급자 (softwarecampus)

#### 2) `validateToken(String token)`
- **반환:** `boolean` (유효하면 true)
- **검증 내용:**
  - 서명 검증 (secret key)
  - 만료 시간 검증
  - 발급자 검증

#### 3) `getEmailFromToken(String token)`
- **반환:** 이메일 문자열
- **추출:** Claims의 `sub` (subject)

#### 4) `getRoleFromToken(String token)`
- **반환:** 권한 문자열 (ROLE_USER, ROLE_ACADEMY, ROLE_ADMIN)
- **추출:** Claims의 `role` (custom claim)

---

## 🔐 JJWT 0.13.0 사용법

### 토큰 생성 예시
```java
String token = Jwts.builder()
    .subject(email)                                      // sub
    .claim("role", "ROLE_" + accountType.name())        // custom claim
    .issuer(jwtProperties.getIssuer())                  // iss
    .issuedAt(new Date())                               // iat
    .expiration(new Date(System.currentTimeMillis() + expiration))  // exp
    .signWith(getSigningKey())                          // 서명
    .compact();
```

### 토큰 파싱 예시
```java
Claims claims = Jwts.parser()
    .verifyWith(getSigningKey())                        // 서명 검증
    .requireIssuer(jwtProperties.getIssuer())           // iss 검증
    .build()
    .parseSignedClaims(token)
    .getPayload();
```

### SigningKey 생성
```java
private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(
        jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
    );
}
```

---

## ⚙️ 환경 설정

### 1. pom.xml (의존성 추가)
```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
```

### 2. application.properties
```properties
# JWT 설정
jwt.secret=${JWT_SECRET}
jwt.expiration=1800000
jwt.issuer=softwarecampus
```

### 3. .env 파일
```bash
# JWT Secret (최소 32자 이상 권장)
JWT_SECRET=your-secret-key-at-least-32-characters-long
```

**주의사항:**
- Secret 키는 **최소 256비트(32바이트)** 이상
- 운영 환경에서는 환경변수로 관리 (절대 하드코딩 금지)
- `.env` 파일은 `.gitignore`에 추가

---

## 🔍 예외 처리

### 주요 예외
- `JwtException`: JWT 관련 모든 예외의 부모 클래스
- `ExpiredJwtException`: 만료된 토큰
- `MalformedJwtException`: 잘못된 형식
- `SignatureException`: 서명 검증 실패
- `UnsupportedJwtException`: 지원하지 않는 토큰

### 처리 방법
```java
public boolean validateToken(String token) {
    try {
        Jwts.parser()
            .verifyWith(getSigningKey())
            .requireIssuer(jwtProperties.getIssuer())
            .build()
            .parseSignedClaims(token);
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        log.error("Invalid JWT token: {}", e.getMessage());
        return false;
    }
}
```

---

## ✅ 검증 포인트

1. ✅ `JwtProperties`에서 설정 값 정상 로드
2. ✅ `generateToken()` 호출 시 유효한 JWT 문자열 반환
3. ✅ `validateToken()`으로 생성한 토큰 검증 성공
4. ✅ `getEmailFromToken()`으로 이메일 추출 성공
5. ✅ `getRoleFromToken()`으로 권한 추출 성공
6. ✅ 만료된 토큰은 `validateToken()` false 반환
7. ✅ 변조된 토큰은 `validateToken()` false 반환

---

## 📝 참고 자료

- [JJWT GitHub](https://github.com/jwtk/jjwt)
- [JWT.io](https://jwt.io/) - 토큰 디버깅
- [RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519) - JWT 표준
