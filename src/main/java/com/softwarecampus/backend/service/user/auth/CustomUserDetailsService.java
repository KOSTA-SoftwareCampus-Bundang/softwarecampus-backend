package com.softwarecampus.backend.service.user.auth;

import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Spring Security 인증을 위한 UserDetailsService 구현체
 * JWT 필터에서 사용자 정보를 로드할 때 사용됨
 * 
 * @since 2025-11-19
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final AccountRepository accountRepository;
    
    /**
     * 이메일로 사용자 정보 로드
     * Spring Security의 인증 과정에서 호출됨
     * 
     * @param email 사용자 이메일
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
        
        return User.builder()
                .username(account.getEmail())
                .password(account.getPassword())
                .authorities(getAuthorities(account))
                .build();
    }
    
    /**
     * 계정의 권한 목록 생성
     * AccountType을 Spring Security의 GrantedAuthority로 변환
     * 
     * @param account 사용자 계정
     * @return 권한 목록
     */
    private List<GrantedAuthority> getAuthorities(Account account) {
        // ROLE_ 접두사는 Spring Security의 권한 체계 규칙
        String role = "ROLE_" + account.getAccountType().name();
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
}
