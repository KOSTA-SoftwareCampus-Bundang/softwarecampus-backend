package com.softwarecampus.backend.integration;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Phase 13: UserDetailsService 캐싱 통합 테스트
 * 
 * 목표:
 * - @Cacheable 동작 검증 (Redis 캐시)
 * - @CacheEvict 동작 검증
 * - 캐시 히트/미스 시나리오 테스트
 * - 실제 Redis 연동 확인
 * 
 * 테스트 시나리오:
 * 1. 첫 호출 시 DB 조회 (캐시 미스)
 * 2. 동일 이메일로 재호출 시 캐시에서 반환 (캐시 히트)
 * 3. evictUserDetailsCache 호출 시 캐시 삭제
 * 4. 캐시 삭제 후 다시 DB 조회
 * 5. 다른 이메일은 별도 캐시 키 사용
 * 
 * @since 2025-11-19
 */
@SpringBootTest
@Transactional
@DisplayName("UserDetailsService 캐싱 통합 테스트")
class UserDetailsCacheIntegrationTest {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @SpyBean
    private AccountRepository accountRepository;
    
    @Autowired(required = false)
    private CacheManager cacheManager;
    
    private Account testAccount;
    
    @BeforeEach
    void setUp() {
        // 캐시 초기화
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
        
        // 테스트용 계정 생성
        testAccount = Account.builder()
                .email("cache@example.com")
                .password("$2a$10$hashedPassword")
                .userName("캐시테스트")
                .phoneNumber("01012345678")
                .accountType(AccountType.USER)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();
        accountRepository.save(testAccount);
        
        // Mock 호출 기록 초기화
        clearInvocations(accountRepository);
    }
    
    @Test
    @DisplayName("첫 호출 시 DB 조회 (캐시 미스)")
    void loadUserByUsername_FirstCall_QueriesDatabase() {
        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("cache@example.com");
        
        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("cache@example.com");
        
        // DB 조회 확인
        verify(accountRepository, times(1)).findByEmail("cache@example.com");
    }
    
    @Test
    @DisplayName("동일 이메일로 재호출 시 캐시에서 반환 (캐시 히트)")
    void loadUserByUsername_SecondCall_UsesCache() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis not available, skipping cache test");
            return;
        }
        
        // given - 첫 번째 호출로 캐시 생성
        UserDetails firstCall = userDetailsService.loadUserByUsername("cache@example.com");
        clearInvocations(accountRepository); // 호출 기록 초기화
        
        // when - 두 번째 호출 (캐시에서 반환)
        UserDetails secondCall = userDetailsService.loadUserByUsername("cache@example.com");
        
        // then - 같은 객체 반환
        assertThat(secondCall).isNotNull();
        assertThat(secondCall.getUsername()).isEqualTo(firstCall.getUsername());
        
        // DB 조회하지 않음 (캐시 히트)
        verify(accountRepository, never()).findByEmail("cache@example.com");
    }
    
    @Test
    @DisplayName("evictUserDetailsCache 호출 시 캐시 삭제")
    void evictUserDetailsCache_RemovesFromCache() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis not available, skipping cache test");
            return;
        }
        
        // given - 캐시 생성
        userDetailsService.loadUserByUsername("cache@example.com");
        clearInvocations(accountRepository);
        
        // when - 캐시 삭제
        userDetailsService.evictUserDetailsCache("cache@example.com");
        
        // then - 다음 호출 시 DB 조회
        userDetailsService.loadUserByUsername("cache@example.com");
        verify(accountRepository, times(1)).findByEmail("cache@example.com");
    }
    
    @Test
    @DisplayName("캐시 삭제 후 다시 DB 조회")
    void loadUserByUsername_AfterEviction_QueriesDatabase() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis not available, skipping cache test");
            return;
        }
        
        // Step 1: 첫 호출 - DB 조회 (캐시 생성)
        userDetailsService.loadUserByUsername("cache@example.com");
        verify(accountRepository, times(1)).findByEmail("cache@example.com");
        
        // Step 2: 두 번째 호출 - 캐시 사용
        clearInvocations(accountRepository);
        userDetailsService.loadUserByUsername("cache@example.com");
        verify(accountRepository, never()).findByEmail("cache@example.com");
        
        // Step 3: 캐시 삭제
        userDetailsService.evictUserDetailsCache("cache@example.com");
        
        // Step 4: 세 번째 호출 - DB 조회 (캐시 미스)
        clearInvocations(accountRepository);
        userDetailsService.loadUserByUsername("cache@example.com");
        verify(accountRepository, times(1)).findByEmail("cache@example.com");
    }
    
    @Test
    @DisplayName("다른 이메일은 별도 캐시 키 사용")
    void loadUserByUsername_DifferentEmail_SeparateCacheKey() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis not available, skipping cache test");
            return;
        }
        
        // given - 두 번째 계정 생성
        Account secondAccount = Account.builder()
                .email("cache2@example.com")
                .password("$2a$10$hashedPassword2")
                .userName("캐시테스트2")
                .phoneNumber("01087654321")
                .accountType(AccountType.USER)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();
        accountRepository.save(secondAccount);
        
        // when - 첫 번째 이메일로 캐시 생성
        userDetailsService.loadUserByUsername("cache@example.com");
        clearInvocations(accountRepository);
        
        // then - 두 번째 이메일은 캐시 미스 (별도 키)
        userDetailsService.loadUserByUsername("cache2@example.com");
        verify(accountRepository, times(1)).findByEmail("cache2@example.com");
    }
    
    @Test
    @DisplayName("동일 이메일 3번 호출 시 첫 번째만 DB 조회")
    void loadUserByUsername_ThreeCalls_OneQuery() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis not available, skipping cache test");
            return;
        }
        
        // when
        userDetailsService.loadUserByUsername("cache@example.com");
        userDetailsService.loadUserByUsername("cache@example.com");
        userDetailsService.loadUserByUsername("cache@example.com");
        
        // then - DB 조회는 1번만
        verify(accountRepository, times(1)).findByEmail("cache@example.com");
    }
    
    @Test
    @DisplayName("캐시 키는 이메일 기반으로 생성됨")
    void cacheKey_BasedOnEmail() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis not available, skipping cache test");
            return;
        }
        
        // when
        userDetailsService.loadUserByUsername("cache@example.com");
        
        // then - 캐시에 값이 저장되었는지 확인
        var cache = cacheManager.getCache("userDetails");
        assertThat(cache).isNotNull();
        
        var cachedValue = cache.get("cache@example.com");
        assertThat(cachedValue).isNotNull();
    }
    
    @Test
    @DisplayName("전체 플로우: 조회 → 캐시 히트 → 캐시 삭제 → 재조회")
    void fullCacheFlow() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis not available, skipping cache test");
            return;
        }
        
        // Step 1: 첫 조회 (DB)
        UserDetails first = userDetailsService.loadUserByUsername("cache@example.com");
        assertThat(first.getUsername()).isEqualTo("cache@example.com");
        verify(accountRepository, times(1)).findByEmail("cache@example.com");
        
        // Step 2: 재조회 (캐시)
        clearInvocations(accountRepository);
        UserDetails second = userDetailsService.loadUserByUsername("cache@example.com");
        assertThat(second.getUsername()).isEqualTo("cache@example.com");
        verify(accountRepository, never()).findByEmail("cache@example.com");
        
        // Step 3: 캐시 삭제
        userDetailsService.evictUserDetailsCache("cache@example.com");
        
        // Step 4: 재조회 (DB)
        clearInvocations(accountRepository);
        UserDetails third = userDetailsService.loadUserByUsername("cache@example.com");
        assertThat(third.getUsername()).isEqualTo("cache@example.com");
        verify(accountRepository, times(1)).findByEmail("cache@example.com");
    }
}
