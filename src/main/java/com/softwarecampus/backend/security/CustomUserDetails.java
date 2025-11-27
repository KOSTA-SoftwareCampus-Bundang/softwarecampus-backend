package com.softwarecampus.backend.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Spring Security의 User를 확장하여 사용자 ID(PK)를 포함하는 클래스
 * - 인가 처리 및 비즈니스 로직에서 DB 조회 없이 ID를 사용하기 위함
 */
@Getter
public class CustomUserDetails extends User {

    private final Long id;

    public CustomUserDetails(Long id, String username, String password,
            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }
}
