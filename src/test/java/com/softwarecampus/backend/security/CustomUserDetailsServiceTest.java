package com.softwarecampus.backend.security;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CustomUserDetailsService 테스트
 * - Spring Security UserDetailsService 구현체 검증
 * - Redis 캐싱 동작은 통합 테스트에서 검증 (여기서는 로직만 확인)
 * 
 * Phase 13: JWT 보안 시스템 테스트
 * 
 * @since 2025-01-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 테스트")
class CustomUserDetailsServiceTest {

        @Mock
        private AccountRepository accountRepository;

        @InjectMocks
        private CustomUserDetailsService customUserDetailsService;

        private Account testUserAccount;
        private Account testAcademyAccount;
        private Account testAdminAccount;

        @BeforeEach
        void setUp() {
                // 일반 사용자 계정
                testUserAccount = Account.builder()
                                .id(1L)
                                .email("user@test.com")
                                .password("$2a$10$hashedPassword")
                                .userName("테스트유저")
                                .phoneNumber("01012345678")
                                .accountType(AccountType.USER)
                                .accountApproved(ApprovalStatus.APPROVED)
                                .build();

                // 기관 계정
                testAcademyAccount = Account.builder()
                                .id(2L)
                                .email("academy@test.com")
                                .password("$2a$10$hashedPassword2")
                                .userName("테스트학원")
                                .phoneNumber("01087654321")
                                .accountType(AccountType.ACADEMY)
                                .accountApproved(ApprovalStatus.APPROVED) // PENDING → APPROVED로 변경
                                .academyId(100L)
                                .build();

                // 관리자 계정
                testAdminAccount = Account.builder()
                                .id(3L)
                                .email("admin@test.com")
                                .password("$2a$10$hashedPassword3")
                                .userName("관리자")
                                .phoneNumber("01011112222")
                                .accountType(AccountType.ADMIN)
                                .accountApproved(ApprovalStatus.APPROVED)
                                .build();
        }

        @Test
        @DisplayName("USER 타입 계정으로 UserDetails 로드 성공")
        void loadUserByUsername_UserType_Success() {
                // given
                when(accountRepository.findByEmailAndIsDeletedFalse("user@test.com"))
                                .thenReturn(Optional.of(testUserAccount));

                // when
                UserDetails userDetails = customUserDetailsService.loadUserByUsername("user@test.com");

                // then
                assertThat(userDetails).isNotNull();
                assertThat(userDetails.getUsername()).isEqualTo("user@test.com");
                assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword");
                assertThat(userDetails.getAuthorities()).hasSize(1);
                assertThat(userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()).containsExactly("ROLE_USER");

                verify(accountRepository, times(1)).findByEmailAndIsDeletedFalse("user@test.com");
        }

        @Test
        @DisplayName("ACADEMY 타입 계정으로 UserDetails 로드 성공")
        void loadUserByUsername_AcademyType_Success() {
                // given
                when(accountRepository.findByEmailAndIsDeletedFalse("academy@test.com"))
                                .thenReturn(Optional.of(testAcademyAccount));

                // when
                UserDetails userDetails = customUserDetailsService.loadUserByUsername("academy@test.com");

                // then
                assertThat(userDetails).isNotNull();
                assertThat(userDetails.getUsername()).isEqualTo("academy@test.com");
                assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword2");
                assertThat(userDetails.getAuthorities()).hasSize(1);
                assertThat(userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()).containsExactly("ROLE_ACADEMY");

                verify(accountRepository, times(1)).findByEmailAndIsDeletedFalse("academy@test.com");
        }

        @Test
        @DisplayName("ADMIN 타입 계정으로 UserDetails 로드 성공")
        void loadUserByUsername_AdminType_Success() {
                // given
                when(accountRepository.findByEmailAndIsDeletedFalse("admin@test.com"))
                                .thenReturn(Optional.of(testAdminAccount));

                // when
                UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin@test.com");

                // then
                assertThat(userDetails).isNotNull();
                assertThat(userDetails.getUsername()).isEqualTo("admin@test.com");
                assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword3");
                assertThat(userDetails.getAuthorities()).hasSize(1);
                assertThat(userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()).containsExactly("ROLE_ADMIN");

                verify(accountRepository, times(1)).findByEmailAndIsDeletedFalse("admin@test.com");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로드 시 UsernameNotFoundException 발생")
        void loadUserByUsername_NotFound_ThrowsException() {
                // given
                when(accountRepository.findByEmailAndIsDeletedFalse("nonexistent@test.com"))
                                .thenReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent@test.com"))
                                .isInstanceOf(UsernameNotFoundException.class)
                                .hasMessageContaining("사용자를 찾을 수 없습니다")
                                .hasMessageContaining("nonexistent@test.com");

                verify(accountRepository, times(1)).findByEmailAndIsDeletedFalse("nonexistent@test.com");
        }

        @Test
        @DisplayName("권한 목록에 ROLE_ 접두사가 정확히 추가됨")
        void getAuthorities_AddsRolePrefix() {
                // given
                when(accountRepository.findByEmailAndIsDeletedFalse("user@test.com"))
                                .thenReturn(Optional.of(testUserAccount));

                // when
                UserDetails userDetails = customUserDetailsService.loadUserByUsername("user@test.com");

                // then
                assertThat(userDetails.getAuthorities()).hasSize(1);
                GrantedAuthority authority = userDetails.getAuthorities().iterator().next();
                assertThat(authority.getAuthority()).startsWith("ROLE_");
                assertThat(authority.getAuthority()).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("동일 이메일로 여러 번 호출 시 Repository 호출 확인 (캐싱은 통합 테스트)")
        void loadUserByUsername_MultipleCalls_RepositoryCalled() {
                // given
                when(accountRepository.findByEmailAndIsDeletedFalse("user@test.com"))
                                .thenReturn(Optional.of(testUserAccount));

                // when
                customUserDetailsService.loadUserByUsername("user@test.com");
                customUserDetailsService.loadUserByUsername("user@test.com");
                customUserDetailsService.loadUserByUsername("user@test.com");

                // then - 캐싱 없는 단위 테스트에서는 3번 호출됨
                // 실제 캐싱은 @Cacheable이 동작하는 통합 테스트에서 검증
                verify(accountRepository, times(3)).findByEmailAndIsDeletedFalse("user@test.com");
        }

        @Test
        @DisplayName("evictUserDetailsCache 호출 시 예외 없이 종료")
        void evictUserDetailsCache_NoException() {
                // given
                String email = "user@test.com";

                // when & then - 예외 발생하지 않음을 확인
                customUserDetailsService.evictUserDetailsCache(email);

                // 캐시 제거는 Spring의 @CacheEvict가 처리하므로
                // 단위 테스트에서는 메서드 호출만 확인
                verifyNoInteractions(accountRepository);
        }
}
