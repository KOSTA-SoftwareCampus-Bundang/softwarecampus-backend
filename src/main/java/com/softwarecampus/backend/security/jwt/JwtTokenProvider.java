package com.softwarecampus.backend.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 담당하는 Provider
 * 
 * @since 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    private final JwtProperties jwtProperties;
    
    /**
     * JWT 서명에 사용할 SecretKey 생성
     * HMAC-SHA 알고리즘 사용 시 최소 256bit 이상의 키 필요
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * JWT 토큰 생성 (이메일만)
     * Refresh Token으로 Access Token 갱신 시 사용
     * 
     * @param email 사용자 이메일
     * @return 생성된 JWT 토큰
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());
        
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * JWT 토큰 생성 (이메일 + 권한)
     * 
     * @param email 사용자 이메일
     * @param role 사용자 권한 (STUDENT, INSTRUCTOR, ADMIN)
     * @return 생성된 JWT 토큰
     */
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());
        
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * JWT 토큰 만료 시간 조회 (밀리초)
     * 
     * @return 토큰 유효 시간 (밀리초)
     */
    public long getExpiration() {
        return jwtProperties.getExpiration();
    }
    
    /**
     * JWT 토큰에서 이메일 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 이메일
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
    
    /**
     * JWT 토큰에서 권한 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 권한
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }
    
    /**
     * JWT 토큰 유효성 검증
     * 
     * @param token JWT 토큰
     * @return 유효하면 true, 아니면 false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * JWT 토큰 파싱 및 검증
     * 
     * @param token JWT 토큰
     * @return Claims 객체
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
