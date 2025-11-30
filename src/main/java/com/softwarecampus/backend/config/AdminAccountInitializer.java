package com.softwarecampus.backend.config;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.common.ApprovalStatus;
import com.softwarecampus.backend.domain.user.Account;
import com.softwarecampus.backend.repository.user.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * 애플리케이션 시작 시 초기 ADMIN 계정 생성
 * - 최초 실행 시에만 ADMIN 계정 자동 생성
 * - 이미 존재하면 스킵
 * - .env 파일에서 환경변수로 설정값 주입 (보안)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // .env 파일에서 ADMIN 설정값 읽기 (필수값)
        String adminEmail = env.getProperty("ADMIN_EMAIL");
        String adminPassword = env.getProperty("ADMIN_PASSWORD");
        String adminName = env.getProperty("ADMIN_NAME");
        String adminPhone = env.getProperty("ADMIN_PHONE");
        
        // 필수 환경변수 검증 - 없으면 애플리케이션 시작 실패
        if (Objects.isNull(adminEmail) || adminEmail.isBlank()) {
            throw new IllegalStateException("ADMIN_EMAIL 환경 변수를 반드시 설정해야 합니다.");
        }
        if (Objects.isNull(adminPassword) || adminPassword.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD 환경 변수를 반드시 설정해야 합니다.");
        }

        // ADMIN 계정이 이미 존재하는지 확인
        if (accountRepository.existsByEmailAndIsDeletedFalse(adminEmail)) {
            log.info("초기 ADMIN 계정이 이미 존재합니다: {}", adminEmail);
            return;
        }

        // Soft Delete된 ADMIN 계정 복구 체크
        Optional<Account> deletedAdmin = accountRepository.findByEmailAndIsDeletedTrue(adminEmail);
        if (deletedAdmin.isPresent()) {
            // 환경변수 확인: ADMIN_ALLOW_RESTORE가 true인 경우에만 복구
            String allowRestore = env.getProperty("ADMIN_ALLOW_RESTORE");
            boolean shouldRestore = "true".equalsIgnoreCase(allowRestore);
            
            if (!shouldRestore) {
                log.warn("====================================================");
                log.warn("삭제된 ADMIN 계정이 발견되었으나 자동 복구가 비활성화되어 있습니다.");
                log.warn("이메일: {}", adminEmail);
                log.warn("복구를 원하시면 ADMIN_ALLOW_RESTORE=true 환경변수를 설정하세요.");
                log.warn("====================================================");
                return;
            }
            
            Account admin = deletedAdmin.get();
            
            // 복구: isDeleted=false로 설정
            admin.restore();
            
            // 필수: 비밀번호 업데이트
            admin.setPassword(passwordEncoder.encode(adminPassword));
            
            // 선택적: 이름/전화번호 업데이트 (값이 있는 경우에만)
            if (StringUtils.hasText(adminName)) {
                admin.setUserName(adminName);
            }
            if (StringUtils.hasText(adminPhone)) {
                admin.setPhoneNumber(adminPhone);
            }
            
            accountRepository.save(admin);
            
            log.warn("====================================================");
            log.warn("삭제된 ADMIN 계정이 복구되었습니다!");
            log.warn("이메일: {}", adminEmail);
            log.warn("⚠️  최초 로그인 후 반드시 비밀번호를 변경하세요!");
            log.warn("====================================================");
            return;
        }

        // ADMIN 계정 생성
        Account admin = Account.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .userName(adminName != null ? adminName : "시스템 관리자")
                .phoneNumber(adminPhone != null ? adminPhone : "010-0000-0000")
                .accountType(AccountType.ADMIN)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();

        accountRepository.save(admin);
        
        log.warn("====================================================");
        log.warn("초기 ADMIN 계정이 생성되었습니다!");
        log.warn("이메일: {}", adminEmail);
        log.warn("⚠️  최초 로그인 후 반드시 비밀번호를 변경하세요!");
        log.warn("====================================================");
    }
}
