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
        // .env 파일에서 ADMIN 설정값 읽기
        String adminEmail = env.getProperty("ADMIN_EMAIL", "admin@softwarecampus.com");
        String adminPassword = env.getProperty("ADMIN_PASSWORD", "Admin!2024@SoftCampus#Init");
        String adminName = env.getProperty("ADMIN_NAME", "시스템 관리자");
        String adminPhone = env.getProperty("ADMIN_PHONE", "010-0000-0000");

        // ADMIN 계정이 이미 존재하는지 확인
        if (accountRepository.existsByEmail(adminEmail)) {
            log.info("초기 ADMIN 계정이 이미 존재합니다: {}", adminEmail);
            return;
        }

        // ADMIN 계정 생성
        Account admin = Account.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .userName(adminName)
                .phoneNumber(adminPhone)
                .accountType(AccountType.ADMIN)
                .accountApproved(ApprovalStatus.APPROVED)
                .build();

        accountRepository.save(admin);
        
        log.warn("====================================================");
        log.warn("초기 ADMIN 계정이 생성되었습니다!");
        log.warn("이메일: {}", adminEmail);
        log.warn("⚠️  최초 로그인 후 반드시 비밀번호를 변경하세요!");
        log.warn("⚠️  .env 파일에서 초기 비밀번호 설정 가능");
        log.warn("====================================================");
    }
}
