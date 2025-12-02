package com.softwarecampus.backend.service.user.profile;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.exception.user.AccountNotFoundException;
import com.softwarecampus.backend.repository.user.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProfileServiceImpl 단위 테스트
 * 
 * 테스트 대상:
 * - getAccountById(Long id): ID로 계정 조회
 * - getAccountByEmail(String email): 이메일로 계정 조회
 * 
 * Mock 대상:
 * - AccountRepository: DB 접근 모킹
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService 단위 테스트")
class ProfileServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .email("user@example.com")
                .userName("홍길동")
                .phoneNumber("010-1234-5678")
                .accountType(AccountType.USER)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("ID로 계정 조회 - 성공")
    void getAccountById_성공() {
        // Given
        when(accountRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testAccount));

        // When
        AccountResponse response = profileService.getAccountById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.userName()).isEqualTo("홍길동");

        verify(accountRepository).findByIdAndIsDeletedFalse(1L);
    }

    @Test
    @DisplayName("ID로 계정 조회 - 계정 없음")
    void getAccountById_계정없음() {
        // Given
        when(accountRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> profileService.getAccountById(999L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("계정을 찾을 수 없습니다.");

        verify(accountRepository).findByIdAndIsDeletedFalse(999L);
    }

    @Test
    @DisplayName("이메일로 계정 조회 - 성공")
    void getAccountByEmail_성공() {
        // Given
        when(accountRepository.findByEmailAndIsDeletedFalse("user@example.com"))
                .thenReturn(Optional.of(testAccount));

        // When
        AccountResponse response = profileService.getAccountByEmail("user@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.userName()).isEqualTo("홍길동");

        verify(accountRepository).findByEmailAndIsDeletedFalse("user@example.com");
    }

    @Test
    @DisplayName("이메일로 계정 조회 - 계정 없음")
    void getAccountByEmail_계정없음() {
        // Given
        when(accountRepository.findByEmailAndIsDeletedFalse("notfound@example.com"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> profileService.getAccountByEmail("notfound@example.com"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("계정을 찾을 수 없습니다.");

        verify(accountRepository).findByEmailAndIsDeletedFalse("notfound@example.com");
    }

    @Test
    @DisplayName("이메일 형식 검증 - null 입력")
    void getAccountByEmail_이메일null() {
        // When & Then
        assertThatThrownBy(() -> profileService.getAccountByEmail(null))
                .isInstanceOf(com.softwarecampus.backend.exception.user.InvalidInputException.class)
                .hasMessage("이메일을 입력해주세요.");

        // Repository 호출되지 않아야 함
        verify(accountRepository, never()).findByEmailAndIsDeletedFalse(any());
    }

    @Test
    @DisplayName("이메일 형식 검증 - 빈 문자열")
    void getAccountByEmail_이메일빈문자열() {
        // When & Then
        assertThatThrownBy(() -> profileService.getAccountByEmail("   "))
                .isInstanceOf(com.softwarecampus.backend.exception.user.InvalidInputException.class)
                .hasMessage("이메일을 입력해주세요.");

        verify(accountRepository, never()).findByEmailAndIsDeletedFalse(any());
    }

    @Test
    @DisplayName("이메일 형식 검증 - 잘못된 형식")
    void getAccountByEmail_이메일형식오류() {
        // When & Then
        assertThatThrownBy(() -> profileService.getAccountByEmail("invalid-email"))
                .isInstanceOf(com.softwarecampus.backend.exception.user.InvalidInputException.class)
                .hasMessage("올바른 이메일 형식이 아닙니다.");

        verify(accountRepository, never()).findByEmailAndIsDeletedFalse(any());
    }
}