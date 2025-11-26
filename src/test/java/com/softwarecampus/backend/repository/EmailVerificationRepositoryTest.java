package com.softwarecampus.backend.repository;

import com.softwarecampus.backend.domain.EmailVerification;
import com.softwarecampus.backend.domain.common.VerificationType;
import com.softwarecampus.backend.repository.user.EmailVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EmailVerificationRepository 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("이메일 인증 Repository 테스트")
class EmailVerificationRepositoryTest {
    
    @Autowired
    private EmailVerificationRepository repository;
    
    private String testEmail;
    private String testCode;
    
    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testCode = "123456";
        repository.deleteAll();
    }
    
    @Test
    @DisplayName("이메일 인증 레코드를 저장할 수 있다")
    void save_ShouldPersistEmailVerification() {
        // given
        EmailVerification verification = createVerification(testEmail, testCode);
        
        // when
        EmailVerification saved = repository.save(verification);
        
        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo(testEmail);
        assertThat(saved.getCode()).isEqualTo(testCode);
    }
    
    @Test
    @DisplayName("이메일과 타입으로 최근 레코드를 조회할 수 있다")
    void findTopByEmailAndTypeOrderByCreatedAtDesc_ShouldReturnLatest() throws InterruptedException {
        // given
        repository.save(createVerification(testEmail, "111111"));
        Thread.sleep(10); // 시간차 보장
        EmailVerification latest = repository.save(createVerification(testEmail, "222222"));
        
        // when
        Optional<EmailVerification> result = repository
                .findTopByEmailAndTypeOrderByCreatedAtDesc(testEmail, VerificationType.SIGNUP);
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("222222");
    }
    
    @Test
    @DisplayName("인증 완료된 레코드가 존재하는지 확인할 수 있다")
    void existsByEmailAndTypeAndVerifiedTrue_ShouldReturnTrue() {
        // given
        EmailVerification verification = createVerification(testEmail, testCode);
        verification.markAsVerified();
        repository.save(verification);
        
        // when
        boolean exists = repository.existsByEmailAndTypeAndVerifiedTrue(
                testEmail, 
                VerificationType.SIGNUP
        );
        
        // then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("인증 완료되지 않은 경우 false를 반환한다")
    void existsByEmailAndTypeAndVerifiedTrue_WithUnverified_ShouldReturnFalse() {
        // given
        repository.save(createVerification(testEmail, testCode));
        
        // when
        boolean exists = repository.existsByEmailAndTypeAndVerifiedTrue(
                testEmail, 
                VerificationType.SIGNUP
        );
        
        // then
        assertThat(exists).isFalse();
    }
    
    @Test
    @DisplayName("이메일, 타입, 코드로 레코드를 조회할 수 있다")
    void findByEmailAndTypeAndCode_ShouldReturnVerification() {
        // given
        repository.save(createVerification(testEmail, testCode));
        
        // when
        Optional<EmailVerification> result = repository.findByEmailAndTypeAndCode(
                testEmail, 
                VerificationType.SIGNUP, 
                testCode
        );
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(testEmail);
    }
    
    @Test
    @DisplayName("인증 완료된 오래된 데이터를 삭제할 수 있다")
    void deleteOldVerified_ShouldDeleteOldVerifiedRecords() {
        // given
        EmailVerification verified = createVerification(testEmail, testCode);
        verified.markAsVerified();
        repository.save(verified);
        
        // when
        LocalDateTime cutoff = LocalDateTime.now().plusDays(1); // 미래 시점
        int deleted = repository.deleteOldVerified(cutoff);
        repository.flush();
        
        // then
        assertThat(deleted).isEqualTo(1);
    }
    
    @Test
    @DisplayName("특정 이메일과 타입의 모든 레코드를 삭제할 수 있다")
    void deleteByEmailAndType_ShouldDeleteAllRecordsForEmailAndType() {
        // given
        repository.save(createVerification(testEmail, "111111"));
        repository.save(createVerification(testEmail, "222222"));
        repository.save(createVerification("other@example.com", "333333"));
        
        // when
        repository.deleteByEmailAndType(testEmail, VerificationType.SIGNUP);
        repository.flush();
        
        // then
        assertThat(repository.count()).isEqualTo(1); // other@example.com만 남음
    }
    
    private EmailVerification createVerification(String email, String code) {
        return EmailVerification.builder()
                .email(email)
                .code(code)
                .type(VerificationType.SIGNUP)
                .verified(false)
                .attempts(0)
                .blocked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();
    }
}
