package com.softwarecampus.backend.integration;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountCacheDto;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 13: UserDetailsService 캐싱 통합 테스트
 * 
 * 목표:
 * - 실제 MySQL + Redis를 사용한 통합 테스트
 * - @Cacheable 동작 검증 (Redis 캐시)
 * - @CacheEvict 동작 검증
 * - 캐시 히트/미스 시나리오 테스트
 * 
 * 캐싱 전략:
 * - AccountCacheDto를 Redis에 캐싱
 * - UserDetails는 매번 새로 생성
 * - DB 조회만 캐싱하여 성능 향상
 * 
 * 테스트 방식:
 * - Mock 사용하지 않음 (실제 MySQL + Redis 사용)
 * - CacheManager를 통한 캐시 존재 여부 확인
 * - 실제 캐시 데이터 검증
 * 
 * @since 2025-11-19
 */
@SpringBootTest
@DisplayName("UserDetailsService 캐싱 통합 테스트")
class UserDetailsCacheIntegrationTest {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired(required = false)
    private CacheManager cacheManager;
    
    private Account testAccount;
    
    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        accountRepository.deleteAll();
        
        // 캐시 초기화
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
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
    }
    
    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        accountRepository.deleteAll();
        
        // 캐시 정리
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }
    
    @Test
    @DisplayName("첫 호출 시 캐시에 데이터 저장")
    void firstCall_CreatesCache() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis 미사용 환경 - 테스트 스킵");
            return;
        }
        
        // given
        Cache cache = cacheManager.getCache("userDetails");
        assertThat(cache).isNotNull();
        assertThat(cache.get("cache@example.com")).isNull(); // 캐시 비어있음
        
        // when - 첫 호출
        AccountCacheDto result = userDetailsService.getAccountByEmail("cache@example.com");
        
        // then - 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("cache@example.com");
        
        // 캐시에 저장되었는지 확인
        Cache.ValueWrapper cachedValue = cache.get("cache@example.com");
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isInstanceOf(AccountCacheDto.class);
        
        AccountCacheDto cached = (AccountCacheDto) cachedValue.get();
        assertThat(cached.getEmail()).isEqualTo("cache@example.com");
    }
    
    @Test
    @DisplayName("동일 이메일로 재호출 시 캐시에서 반환")
    void secondCall_UsesCache() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis 미사용 환경 - 테스트 스킵");
            return;
        }
        
        // given - 첫 호출로 캐시 생성
        AccountCacheDto first = userDetailsService.getAccountByEmail("cache@example.com");
        
        // when - 두 번째 호출
        AccountCacheDto second = userDetailsService.getAccountByEmail("cache@example.com");
        
        // then - 같은 데이터 (캐시에서 가져옴)
        assertThat(second).isNotNull();
        assertThat(second.getEmail()).isEqualTo(first.getEmail());
        assertThat(second.getId()).isEqualTo(first.getId());
    }
    
    @Test
    @DisplayName("evictUserDetailsCache 호출 시 캐시 삭제")
    void evictCache_RemovesFromCache() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis 미사용 환경 - 테스트 스킵");
            return;
        }
        
        // given - 캐시 생성
        userDetailsService.getAccountByEmail("cache@example.com");
        Cache cache = cacheManager.getCache("userDetails");
        assertThat(cache.get("cache@example.com")).isNotNull();
        
        // when - 캐시 삭제
        userDetailsService.evictUserDetailsCache("cache@example.com");
        
        // then - 캐시에서 제거됨
        assertThat(cache.get("cache@example.com")).isNull();
    }
    
    @Test
    @DisplayName("캐시 삭제 후 재호출 시 다시 캐시 생성")
    void afterEviction_RecreatesCache() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis 미사용 환경 - 테스트 스킵");
            return;
        }
        
        // given - 캐시 생성 후 삭제
        userDetailsService.getAccountByEmail("cache@example.com");
        userDetailsService.evictUserDetailsCache("cache@example.com");
        
        Cache cache = cacheManager.getCache("userDetails");
        assertThat(cache.get("cache@example.com")).isNull();
        
        // when - 재호출
        AccountCacheDto result = userDetailsService.getAccountByEmail("cache@example.com");
        
        // then - 캐시 다시 생성됨
        assertThat(result).isNotNull();
        assertThat(cache.get("cache@example.com")).isNotNull();
    }
    
    @Test
    @DisplayName("다른 이메일은 별도 캐시 키 사용")
    void differentEmail_UsesSeparateCacheKey() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis 미사용 환경 - 테스트 스킵");
            return;
        }
        
        // given - 두 번째 계정 생성
        Account account2 = Account.builder()
                .email("cache2@example.com")
                .password("$2a$10$hashedPassword")
                .userName("캐시테스트2")
                .phoneNumber("01087654321")
                .accountType(AccountType.ACADEMY)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();
        accountRepository.save(account2);
        
        // when - 두 이메일로 호출
        userDetailsService.getAccountByEmail("cache@example.com");
        userDetailsService.getAccountByEmail("cache2@example.com");
        
        // then - 각각 별도 캐시 키로 저장됨
        Cache cache = cacheManager.getCache("userDetails");
        assertThat(cache.get("cache@example.com")).isNotNull();
        assertThat(cache.get("cache2@example.com")).isNotNull();
        
        // 캐시 내용 검증
        AccountCacheDto cached1 = (AccountCacheDto) cache.get("cache@example.com").get();
        AccountCacheDto cached2 = (AccountCacheDto) cache.get("cache2@example.com").get();
        
        assertThat(cached1.getEmail()).isEqualTo("cache@example.com");
        assertThat(cached2.getEmail()).isEqualTo("cache2@example.com");
        assertThat(cached1.getAccountType()).isEqualTo(AccountType.USER);
        assertThat(cached2.getAccountType()).isEqualTo(AccountType.ACADEMY);
    }
    
    @Test
    @DisplayName("loadUserByUsername은 매번 새로운 UserDetails 생성")
    void loadUserByUsername_CreatesNewUserDetails() {
        // when - 두 번 호출
        UserDetails user1 = userDetailsService.loadUserByUsername("cache@example.com");
        UserDetails user2 = userDetailsService.loadUserByUsername("cache@example.com");
        
        // then - UserDetails는 매번 새로 생성 (다른 객체)
        assertThat(user1).isNotSameAs(user2);
        
        // 하지만 내용은 동일
        assertThat(user1.getUsername()).isEqualTo(user2.getUsername());
        assertThat(user1.getPassword()).isEqualTo(user2.getPassword());
        assertThat(user1.getAuthorities()).hasSize(user2.getAuthorities().size());
    }
    
    @Test
    @DisplayName("캐시된 데이터 구조 검증")
    void cacheData_StructureValidation() {
        if (cacheManager == null) {
            System.out.println("⚠️ Redis 미사용 환경 - 테스트 스킵");
            return;
        }
        
        // when
        userDetailsService.getAccountByEmail("cache@example.com");
        
        // then
        Cache cache = cacheManager.getCache("userDetails");
        assertThat(cache).isNotNull();
        
        Cache.ValueWrapper wrapper = cache.get("cache@example.com");
        assertThat(wrapper).isNotNull();
        
        Object cachedObject = wrapper.get();
        assertThat(cachedObject).isInstanceOf(AccountCacheDto.class);
        
        AccountCacheDto dto = (AccountCacheDto) cachedObject;
        assertThat(dto.getId()).isEqualTo(testAccount.getId());
        assertThat(dto.getEmail()).isEqualTo("cache@example.com");
        assertThat(dto.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(dto.getAccountType()).isEqualTo(AccountType.USER);
    }
    
    @Test
    @DisplayName("Redis 미사용 환경에서도 정상 동작")
    void worksWithoutRedis() {
        // when - Redis 없어도 정상 동작
        AccountCacheDto result = userDetailsService.getAccountByEmail("cache@example.com");
        UserDetails userDetails = userDetailsService.loadUserByUsername("cache@example.com");
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("cache@example.com");
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("cache@example.com");
    }
}
