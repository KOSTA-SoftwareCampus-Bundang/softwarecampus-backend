package com.softwarecampus.backend.service.AdminAccount;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.dto.user.AccountResponse;
import com.softwarecampus.backend.dto.user.AccountUpdateRequest;
import com.softwarecampus.backend.repository.user.AccountRepository;
import com.softwarecampus.backend.service.academy.AccountAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
class AccountAdminServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountAdminServiceImpl accountAdminService;

    private Account activeUser;
    private Account deletedUser;
    private Account academyUser;

    private void setAccountId(Account account, Long id) {
        try {
            Field idField = Account.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }

    // 🟢 헬퍼 메서드 2: isDeleted 필드 설정 (핵심 수정 부분)
    private void setIsDeleted(Account account, Boolean deleted) {
        try {
            // Account가 상속받은 BaseSoftDeleteSupportEntity에서 isDeleted 필드를 찾습니다.
            Field isDeletedField = account.getClass().getSuperclass().getDeclaredField("isDeleted");
            isDeletedField.setAccessible(true); // private/protected 접근 가능하도록 설정
            isDeletedField.set(account, deleted); // 값 주입
        } catch (Exception e) {
            throw new RuntimeException("Failed to set isDeleted via reflection", e);
        }
    }

    @BeforeEach
    void setUp() {
        // 1. 활성 일반 사용자 (Builder 사용 시 isDeleted 필드가 누락됨)
        activeUser = Account.builder()
                .email("active@test.com").userName("활성사용자").accountType(AccountType.USER)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();
        setAccountId(activeUser, 1L); // ID 설정
        setIsDeleted(activeUser, false); // 🟢 리플렉션으로 isDeleted 설정

        // 2. 삭제된 사용자
        deletedUser = Account.builder()
                .email("deleted@test.com").userName("삭제된사용자").accountType(AccountType.USER)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();
        setAccountId(deletedUser, 2L); // ID 설정
        setIsDeleted(deletedUser, true); // 🟢 리플렉션으로 isDeleted 설정

        // 3. 승인 대기중인 기관 사용자
        academyUser = Account.builder()
                .email("academy@test.com").userName("기관사용자").accountType(AccountType.ACADEMY)
                .accountApproved(ApprovalStatus.PENDING)
                .build();
        setAccountId(academyUser, 3L); // ID 설정
        setIsDeleted(academyUser, false); // 🟢 리플렉션으로 isDeleted 설정
    }

    // AccountResponse DTO의 필드 순서와 매칭되는지 확인하는 헬퍼 메서드
    private void assertAccountResponse(Account account, AccountResponse response) {
        assertThat(response.id()).isEqualTo(account.getId());
        assertThat(response.email()).isEqualTo(account.getEmail());
        assertThat(response.userName()).isEqualTo(account.getUserName());
        assertThat(response.accountType()).isEqualTo(account.getAccountType());
        // DTO 필드명은 approvalStatus지만 엔티티는 accountApproved이므로, 값을 확인
        assertThat(response.approvalStatus()).isEqualTo(account.getAccountApproved());
    }

    /**
     *  목록 조회 및 검색 테스트
     */
    @Test
    @DisplayName("활성 회원 목록 조회 시 삭제된 계정은 제외")
    void getAllActiveAccounts() {
        List<Account> activeAccounts = Arrays.asList(activeUser, academyUser);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> mockPage = new PageImpl<>(activeAccounts, pageable, activeAccounts.size());

        when(accountRepository.findByIsDeletedFalse(any(Pageable.class))).thenReturn(mockPage);

        // When
        Page<AccountResponse> resultPage = accountAdminService.getAllActiveAccounts(pageable);

        // Then
        // 1. 총 2개의 활성 계정만 남아 있어야 한다 (deletedUser 제외)
        assertThat(resultPage.getContent()).hasSize(2);

        // 2. 삭제된 사용자가 목록에 없는지 확인
        List<String> userNames = resultPage.getContent().stream()
                .map(AccountResponse::userName)
                .toList();
        assertThat(userNames).doesNotContain("삭제된사용자");
        assertThat(userNames).containsExactlyInAnyOrder("활성사용자", "기관사용자");
    }

    @Test
    @DisplayName("회원 목록 검색 시 키워드와 Soft Delete 상태를 필터링")
    void searchAccounts_FilterByKeyword() {
        // '사용자'라는 키워드에는 세 명 모두 포함됨.
        List<Account> allAccounts = Arrays.asList(activeUser, academyUser);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> mockPage = new PageImpl<>(allAccounts, pageable, allAccounts.size());

        String keyword = "사용자";
        String searchKeyword = keyword.toLowerCase();

        when(accountRepository.searchActiveAccounts(eq(searchKeyword), any(Pageable.class))).thenReturn(mockPage);

        // When
        Page<AccountResponse> resultPage = accountAdminService.searchAccounts(keyword, pageable);

        // Then
        // 1. 키워드에 해당하면서 활성 상태인 2명만 반환
        assertThat(resultPage.getContent()).hasSize(2);

        // 2. 삭제된 사용자(deletedUser)가 포함되지 않았는지 확인
        assertThat(resultPage.getContent().stream().map(AccountResponse::id).toList())
                .contains(activeUser.getId(), academyUser.getId())
                .doesNotContain(deletedUser.getId());
    }

    /**
     *  상세 조회 테스트
     */
    @Test
    @DisplayName("활성 계정 상세 조회 성공")
    void getAccountDetail_success() {
        when(accountRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        // When
        AccountResponse response = accountAdminService.getAccountDetail(activeUser.getId());

        // Then
        assertAccountResponse(activeUser, response);
    }

    @Test
    @DisplayName("존재하지 않는 계정 상세 조회 시 NoSuchElementException 발생")
    void getAccountDetail_fail_notFound() {
        when(accountRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class,
                () -> accountAdminService.getAccountDetail(999L),
                "Account Not Found 예외가 발생해야 한다.");
    }

    @Test
    @DisplayName("삭제된 계정 상세 조회 시 NoSuchElementException 발생")
    void getAccountDetail_fail_deletedUser() {
        when(accountRepository.findById(deletedUser.getId())).thenReturn(Optional.of(deletedUser));

        // When & Then
        assertThrows(NoSuchElementException.class,
                () -> accountAdminService.getAccountDetail(deletedUser.getId()),
                "삭제된 계정은 찾을 수 없음 예외가 발생해야 한다.");
    }

    /**
     *  회원 정보 수정 테스트
     */
    @Test
    @DisplayName("회원 정보 수정 시 DTO의 내용대로 업데이트 되어야 한다.")
    void updateAccount_success() {
        AccountUpdateRequest updateRequest = new AccountUpdateRequest();
        updateRequest.setUserName("업데이트된이름");
        updateRequest.setAffiliation("새 소속");
        updateRequest.setAccountApproved(ApprovalStatus.APPROVED); // 승인 상태 변경

        when(accountRepository.findById(academyUser.getId())).thenReturn(Optional.of(academyUser));

        // When
        AccountResponse response = accountAdminService.updateAccount(academyUser.getId(), updateRequest);

        // Then
        // 1. 반환된 DTO가 수정된 내용 포함 확인
        assertThat(response.userName()).isEqualTo("업데이트된이름");
        assertThat(response.affiliation()).isEqualTo("새 소속");
        assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);

        // 2. 원본 엔티티 객체(Mock으로 반환된 academyUser)가 실제로 변경되었는지 확인 (Dirty Checking 검증)
        assertThat(academyUser.getUserName()).isEqualTo("업데이트된이름");
        assertThat(academyUser.getAffiliation()).isEqualTo("새 소속");
        assertThat(academyUser.getAccountApproved()).isEqualTo(ApprovalStatus.APPROVED);
    }

    /**
     *  삭제 테스트
     */
    @Test
    @DisplayName("회원 삭제 시 Repository의 delete 메서드가 호출되어 Soft Delete가 실행되어야 함")
    void deleteAccount_success() {
        when(accountRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        // When
        accountAdminService.deleteAccount(activeUser.getId());

        // Then
        // 1. Repository의 delete 메서드가 정확히 호출되었는지 확인
        //    (Spring Data JPA가 delete() 호출 시 Soft Delete 로직을 수행한다고 가정)
        verify(accountRepository, times(1)).delete(activeUser);

        // 2. findById는 한 번 호출되었는지 확인
        verify(accountRepository, times(1)).findById(activeUser.getId());
    }

    @Test
    @DisplayName("존재하지 않는 회원 삭제 시 NoSuchElementException 발생")
    void deleteAccount_fail_notFound() {
        when(accountRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class,
                () -> accountAdminService.deleteAccount(999L),
                "계정을 찾을 수 없음 예외가 발생해야 한다.");

        // delete 메서드는 호출되지 않았는지 확인
        verify(accountRepository, never()).delete(any());
    }
}
