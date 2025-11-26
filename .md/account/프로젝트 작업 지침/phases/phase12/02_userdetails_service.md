# Phase 12-2: CustomUserDetailsService 구현

## 📌 개요

Spring Security의 `UserDetailsService`를 구현하여 DB에서 사용자 정보를 조회하고 인증에 사용합니다.

---

## 🔧 구현 내용

### CustomUserDetailsService

**파일:** `src/main/java/com/softwarecampus/backend/security/CustomUserDetailsService.java`

**역할:**
- Spring Security 인증 시 사용자 정보 로드
- DB의 `Account` 엔티티 → Spring Security의 `UserDetails` 변환

**주요 메서드:**

#### `loadUserByUsername(String email)`
```java
@Override
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Account account = accountRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
    
    return User.builder()
        .username(account.getEmail())
        .password(account.getPassword())
        .authorities(getAuthorities(account.getAccountType()))
        .build();
}
```

**파라미터:**
- `email`: 사용자 이메일 (우리 시스템에서는 email이 username 역할)

**반환:**
- `UserDetails` 구현체 (Spring Security의 `User` 클래스 사용)

**예외:**
- `UsernameNotFoundException`: 이메일에 해당하는 계정이 없을 때

---

## 🔐 권한(Authority) 매핑

### AccountType → GrantedAuthority

```java
private Collection<? extends GrantedAuthority> getAuthorities(AccountType accountType) {
    return Collections.singletonList(
        new SimpleGrantedAuthority("ROLE_" + accountType.name())
    );
}
```

**매핑 규칙:**
- `AccountType.USER` → `ROLE_USER`
- `AccountType.ACADEMY` → `ROLE_ACADEMY`
- `AccountType.ADMIN` → `ROLE_ADMIN`

**주의사항:**
- Spring Security는 권한에 `ROLE_` 접두사를 붙이는 것이 관례
- `@PreAuthorize("hasRole('USER')")` 사용 시 자동으로 `ROLE_USER` 검색

---

## 🔍 AccountRepository 메서드

### `findByEmail(String email)`

이미 Phase 2에서 구현된 메서드입니다:

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);
    // ...
}
```

**참고:**
- `@Query` 불필요 (Spring Data JPA가 자동 생성)
- `Optional` 반환으로 null 안전성 보장

---

## 🧩 Spring Security와의 연동

### 1. SecurityConfig에서 UserDetailsService 사용

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
```

### 2. JWT Filter에서 UserDetails 로드

```java
// JwtAuthenticationFilter.java
String email = jwtTokenProvider.getEmailFromToken(token);
UserDetails userDetails = userDetailsService.loadUserByUsername(email);

UsernamePasswordAuthenticationToken authentication = 
    new UsernamePasswordAuthenticationToken(
        userDetails,
        null,
        userDetails.getAuthorities()
    );

SecurityContextHolder.getContext().setAuthentication(authentication);
```

---

## 🚨 예외 처리

### UsernameNotFoundException

**발생 시점:**
- 존재하지 않는 이메일로 인증 시도

**처리 방법:**
- Spring Security가 자동으로 `BadCredentialsException`으로 변환
- 클라이언트에게는 "잘못된 인증 정보입니다" 메시지 반환 (보안상 이유)

**예시:**
```java
// 존재하지 않는 이메일
userDetailsService.loadUserByUsername("nonexistent@example.com");
// → UsernameNotFoundException 발생
// → Spring Security가 BadCredentialsException으로 변환
// → 클라이언트: "Invalid credentials"
```

---

## ✅ 검증 포인트

1. ✅ `@Service` 어노테이션 추가
2. ✅ `AccountRepository` 의존성 주입
3. ✅ `loadUserByUsername()` 메서드 구현
4. ✅ `Account` → `UserDetails` 변환 로직
5. ✅ 권한 매핑 (`ROLE_` 접두사)
6. ✅ 존재하지 않는 이메일 시 `UsernameNotFoundException` 발생
7. ✅ 컴파일 성공 (`mvn clean compile`)

---

## 📝 UserDetails vs Account

| 항목 | Account (우리 도메인) | UserDetails (Spring Security) |
|------|---------------------|------------------------------|
| 식별자 | email | username |
| 비밀번호 | password | password |
| 권한 | accountType (enum) | authorities (Collection) |
| 상태 | accountApproved | enabled, locked, expired 등 |

**변환 이유:**
- Spring Security는 `UserDetails` 인터페이스만 인식
- 우리 도메인 모델을 Spring Security가 이해할 수 있도록 변환 필요

---

## 🔗 다음 단계

CustomUserDetailsService 구현 완료 후:
- **Phase 12-3**: JwtAuthenticationFilter 구현
- **Phase 12-4**: SecurityConfig 완성
