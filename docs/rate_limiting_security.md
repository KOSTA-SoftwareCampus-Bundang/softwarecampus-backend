# Rate Limiting IP 스푸핑 보안 가이드

작성일자: 2025-12-01  
관련 파일: `RateLimitFilter.java`, `application.properties`

---

## 📋 개요

Rate Limiting은 IP 기반으로 동작하므로, 클라이언트가 IP 주소를 위조할 수 있다면 제한을 우회할 수 있습니다.  
특히 X-Forwarded-For/X-Real-IP 헤더를 무조건 신뢰하는 경우 IP 스푸핑 공격에 취약합니다.

본 문서는 IP 스푸핑 방지를 위한 보안 설정을 다룹니다.

---

## 🚨 취약점

### 기존 문제점

```java
// 취약한 코드 (수정 전)
private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");  // 무조건 신뢰
    
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("X-Real-IP");  // 무조건 신뢰
    }
    
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getRemoteAddr();
    }
    
    return ip;
}
```

**공격 시나리오:**

1. 공격자가 Rate Limiting을 우회하고 싶음
2. HTTP 헤더에 `X-Forwarded-For: 1.2.3.4` 를 임의로 추가
3. 서버는 실제 IP가 아닌 `1.2.3.4`로 Rate Limiting 적용
4. 공격자는 헤더 값을 계속 변경하며 무제한 요청 가능

---

## ✅ 해결 방법

### 1. 신뢰할 수 있는 프록시 검증

**핵심 원칙:**  
프록시 헤더(X-Forwarded-For/X-Real-IP)는 **신뢰할 수 있는 프록시에서 온 요청에만 사용**합니다.

```java
private String getClientIp(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();  // 직접 연결된 IP
    
    // 1. 프록시 헤더를 신뢰하지 않는 경우 (기본값)
    if (!trustForwardedHeaders) {
        return remoteAddr;  // RemoteAddr만 사용
    }
    
    // 2. 프록시 헤더를 신뢰하는 경우
    if (trustedProxies != null && !trustedProxies.isEmpty()) {
        String[] trustedProxyList = trustedProxies.split(",");
        boolean isTrustedProxy = false;
        
        // remoteAddr가 신뢰할 수 있는 프록시 목록에 있는지 확인
        for (String trustedProxy : trustedProxyList) {
            if (remoteAddr.equals(trustedProxy.trim())) {
                isTrustedProxy = true;
                break;
            }
        }
        
        // 신뢰할 수 없는 프록시 → RemoteAddr 사용 (스푸핑 차단)
        if (!isTrustedProxy) {
            return remoteAddr;
        }
    }
    
    // 3. 신뢰할 수 있는 프록시로 검증됨 → X-Forwarded-For 사용
    String ip = request.getHeader("X-Forwarded-For");
    // ...
}
```

### 2. 설정 방식

#### application.properties

```properties
# Rate Limiting 설정

# 프록시 헤더 신뢰 여부
# false (기본값): RemoteAddr만 사용
# true: 신뢰할 수 있는 프록시 검증 후 X-Forwarded-For 사용
rate.limit.trust-forwarded-headers=false

# 신뢰할 수 있는 프록시 IP 목록 (쉼표 구분)
# 운영 환경: 실제 Nginx, AWS ALB/ELB 등의 IP 설정
# 로컬/개발: 127.0.0.1 (또는 비워두고 trust-forwarded-headers=false 사용)
rate.limit.trusted-proxies=
```

#### .env (환경별 설정)

**로컬 개발 환경:**

```bash
# 프록시 없음 - RemoteAddr만 사용
RATE_LIMIT_TRUST_FORWARDED_HEADERS=false
RATE_LIMIT_TRUSTED_PROXIES=
```

**운영 환경 (Nginx 프록시):**

```bash
# Nginx가 10.0.1.100 IP에서 동작
RATE_LIMIT_TRUST_FORWARDED_HEADERS=true
RATE_LIMIT_TRUSTED_PROXIES=10.0.1.100
```

**운영 환경 (AWS ALB + Nginx):**

```bash
# ALB: 10.0.1.x 대역, Nginx: 10.0.2.100
RATE_LIMIT_TRUST_FORWARDED_HEADERS=true
RATE_LIMIT_TRUSTED_PROXIES=10.0.1.0/24,10.0.2.100
```

---

## 🔧 배포 환경별 설정 가이드

### 1. 로컬 개발 환경 (프록시 없음)

**설정:**

```properties
rate.limit.trust-forwarded-headers=false
rate.limit.trusted-proxies=
```

**동작:**

- `request.getRemoteAddr()` 만 사용
- X-Forwarded-For/X-Real-IP 헤더는 무시됨
- 로컬 테스트: 127.0.0.1로 Rate Limiting 적용

**장점:** IP 스푸핑 불가능, 간단한 설정

---

### 2. 운영 환경 (Nginx 프록시)

**아키텍처:**

```
Client → Nginx (10.0.1.100) → Spring Boot (localhost:8080)
```

**Nginx 설정 (nginx.conf):**

```nginx
server {
    listen 80;
    server_name api.example.com;
    
    location / {
        proxy_pass http://localhost:8080;
        
        # 클라이언트 실제 IP 전달
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Host $host;
    }
}
```

**Spring Boot 설정:**

```properties
rate.limit.trust-forwarded-headers=true
rate.limit.trusted-proxies=10.0.1.100
```

**동작:**

1. 요청이 Nginx(10.0.1.100)에서 들어옴
2. Spring Boot는 `request.getRemoteAddr()` → `10.0.1.100` 확인
3. Trusted Proxies 목록에 있음 → X-Forwarded-For 사용
4. X-Forwarded-For에서 클라이언트 실제 IP 추출

---

### 3. 운영 환경 (AWS ALB + EC2)

**아키텍처:**

```
Client → AWS ALB (10.0.1.x) → Spring Boot EC2 (10.0.2.100)
```

**Spring Boot 설정:**

```properties
rate.limit.trust-forwarded-headers=true
rate.limit.trusted-proxies=10.0.1.0/24
```

**동작:**

- ALB의 IP가 10.0.1.x 대역인 경우 X-Forwarded-For 신뢰
- ALB 외부에서 직접 EC2로 접근 시 RemoteAddr 사용

---

### 4. 운영 환경 (Spring 내장 ForwardedHeaderFilter 사용)

**권장 방법:** Spring Boot의 공식 프록시 헤더 처리 기능 사용

**application.properties:**

```properties
# Spring의 Forwarded 헤더 전략 활성화
server.forward-headers-strategy=NATIVE

# Rate Limiting 설정은 단순화 가능
rate.limit.trust-forwarded-headers=true
rate.limit.trusted-proxies=
```

**장점:**

- Spring이 자동으로 X-Forwarded-* 헤더 처리
- `request.getRemoteAddr()`가 이미 정규화된 클라이언트 IP 반환
- RateLimitFilter에서 추가 검증 불필요

**참고:** `server.forward-headers-strategy=NATIVE` 사용 시 Spring이 내부적으로 Forwarded 헤더를 신뢰할지 결정하므로, 별도의 trusted-proxies 검증 로직이 필요 없을 수 있습니다.

---

## 🧪 테스트

### 1. 로컬 환경 테스트

```bash
# 설정: trust-forwarded-headers=false

# 1. 정상 요청
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# 예상: 127.0.0.1로 Rate Limiting 적용

# 2. X-Forwarded-For 위조 시도
curl -X POST http://localhost:8080/api/auth/login \
  -H "X-Forwarded-For: 1.2.3.4" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# 예상: X-Forwarded-For 무시, 여전히 127.0.0.1로 적용 (스푸핑 차단)
```

### 2. 운영 환경 테스트 (Nginx)

```bash
# 설정: 
# trust-forwarded-headers=true
# trusted-proxies=10.0.1.100

# 1. Nginx를 통한 정상 요청
curl -X POST https://api.example.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# 예상: Nginx가 X-Forwarded-For 설정 → 클라이언트 실제 IP로 Rate Limiting

# 2. 클라이언트가 X-Forwarded-For 위조 시도
curl -X POST https://api.example.com/api/auth/login \
  -H "X-Forwarded-For: 1.2.3.4" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# 예상: 
# - Nginx가 이미 X-Forwarded-For 재설정 → 위조된 헤더 덮어씌워짐
# - 또는 Spring Boot가 신뢰할 수 있는 프록시(10.0.1.100) 검증 → 정상 처리
```

### 3. 직접 접근 공격 테스트

```bash
# 공격자가 Nginx를 우회하고 Spring Boot에 직접 접근 시도
# (방화벽이 없는 경우 가정)

curl -X POST http://10.0.2.100:8080/api/auth/login \
  -H "X-Forwarded-For: 1.2.3.4" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# 예상:
# - request.getRemoteAddr() → 공격자의 실제 IP (예: 203.0.113.50)
# - Trusted Proxies(10.0.1.100)와 일치하지 않음
# - X-Forwarded-For 무시 → 공격자 IP로 Rate Limiting (스푸핑 차단)
```

---

## 📊 보안 수준 비교

| 설정 | IP 스푸핑 방지 | 프록시 환경 지원 | 권장 환경 |
|------|---------------|----------------|----------|
| `trust-forwarded-headers=false` | ✅ 완벽 | ❌ 불가능 | 로컬, 프록시 없음 |
| `trust-forwarded-headers=true` + `trusted-proxies` | ✅ 완벽 | ✅ 가능 | 운영 (Nginx, ALB) |
| `server.forward-headers-strategy=NATIVE` | ✅ 완벽 | ✅ 가능 | 운영 (Spring 공식 방법) |
| 기존 (무조건 신뢰) | ❌ 취약 | ✅ 가능 | ⚠️ 사용 금지 |

---

## 🛡️ 추가 보안 권장사항

### 1. 방화벽 설정

Spring Boot를 직접 외부에 노출하지 말고, 반드시 프록시(Nginx, ALB 등)를 통해 접근하도록 방화벽 설정:

```bash
# EC2 보안 그룹 예시
iptables -A INPUT -p tcp --dport 8080 -s 10.0.1.100 -j ACCEPT  # Nginx만 허용
iptables -A INPUT -p tcp --dport 8080 -j DROP  # 외부 직접 접근 차단
```

### 2. Nginx 설정 보안

Nginx에서 클라이언트가 보낸 X-Forwarded-For 헤더를 덮어쓰도록 설정:

```nginx
# 나쁜 예: 클라이언트 헤더 그대로 전달
proxy_set_header X-Forwarded-For $http_x_forwarded_for;

# 좋은 예: Nginx가 재설정 (클라이언트 위조 헤더 무시)
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

### 3. Spring Security 설정

ForwardedHeaderFilter를 사용하는 경우 Spring Security 필터 체인에 추가:

```java
@Bean
public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
    FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new ForwardedHeaderFilter());
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
}
```

---

## 📚 참고 자료

- [Spring Boot - Forward Headers](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.use-behind-a-proxy-server)
- [RFC 7239 - Forwarded HTTP Extension](https://datatracker.ietf.org/doc/html/rfc7239)
- [OWASP - IP Spoofing](https://owasp.org/www-community/attacks/Spoofing_Attack)

---

**작성:** 2025-12-01  
**버전:** 1.0  
**다음 리뷰:** 2025-12-08
