package com.softwarecampus.backend.service.user.email;

import com.softwarecampus.backend.common.constants.EmailConstants;
import com.softwarecampus.backend.exception.email.EmailSendException;
import com.softwarecampus.backend.domain.common.VerificationType;
import com.softwarecampus.backend.util.email.EmailTemplateLoader;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 이메일 발송 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendServiceImpl implements EmailSendService {
    
    private final JavaMailSender mailSender;
    private final EmailTemplateLoader templateLoader;
    
    @Override
    public void sendVerificationCode(String to, String code, VerificationType type) {
        try {
            MimeMessage message = createMessage(to, code, type);
            mailSender.send(message);
            log.info("이메일 발송 성공 - type: {}", type);
        } catch (MessagingException | IOException e) {
            log.error("이메일 발송 실패 - type: {}, error: {}", type, e.getMessage());
            throw new EmailSendException("이메일 발송에 실패했습니다", e);
        }
    }
    
    /**
     * MIME 메시지 생성
     */
    private MimeMessage createMessage(String to, String code, VerificationType type) throws MessagingException, IOException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(EmailConstants.SENDER_EMAIL, EmailConstants.SENDER_NAME);
        helper.setTo(to);
        helper.setSubject(getSubject(type));
        helper.setText(getHtmlContent(code, type), true); // HTML 모드
        
        return message;
    }
    
    /**
     * 이메일 제목 가져오기
     */
    private String getSubject(VerificationType type) {
        return switch (type) {
            case SIGNUP -> EmailConstants.SUBJECT_SIGNUP;
            case PASSWORD_RESET -> EmailConstants.SUBJECT_PASSWORD_RESET;
        };
    }
    
    /**
     * HTML 본문 생성
     */
    private String getHtmlContent(String code, VerificationType type) throws IOException {
        String templateName = switch (type) {
            case SIGNUP -> "signup-verification.html";
            case PASSWORD_RESET -> "password-reset-verification.html";
        };
        
        Map<String, String> variables = new HashMap<>();
        variables.put("code", code);
        
        return templateLoader.loadAndReplace(templateName, variables);
    }
    
    /**
     * 기관 승인 완료 이메일 발송
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @Override
    public void sendAcademyApprovalEmail(String toEmail, String academyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(EmailConstants.SENDER_EMAIL, EmailConstants.SENDER_NAME);
            helper.setTo(toEmail);
            helper.setSubject("[코스타] 기관 등록이 승인되었습니다");
            
            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2>기관 등록 승인 완료</h2>
                    <p>안녕하세요,</p>
                    <p><strong>%s</strong> 기관 등록이 승인되었습니다.</p>
                    <p>이제 회원가입을 진행하실 수 있습니다.</p>
                    <p>감사합니다.</p>
                </body>
                </html>
                """, academyName);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("기관 승인 이메일 발송 성공 - academyName: {}", academyName);
        } catch (MessagingException e) {
            log.error("기관 승인 이메일 발송 실패 - academyName: {}, error: {}", academyName, e.getMessage());
            throw new EmailSendException("기관 승인 이메일 발송에 실패했습니다", e);
        }
    }
    
    /**
     * 기관 거절 이메일 발송
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @Override
    public void sendAcademyRejectionEmail(String toEmail, String academyName, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(EmailConstants.SENDER_EMAIL, EmailConstants.SENDER_NAME);
            helper.setTo(toEmail);
            helper.setSubject("[코스타] 기관 등록이 거절되었습니다");
            
            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2>기관 등록 거절</h2>
                    <p>안녕하세요,</p>
                    <p><strong>%s</strong> 기관 등록이 거절되었습니다.</p>
                    <h3>거절 사유:</h3>
                    <p style="background-color: #f5f5f5; padding: 10px; border-left: 3px solid #d32f2f;">%s</p>
                    <p>수정 후 다시 신청해주시기 바랍니다.</p>
                    <p>감사합니다.</p>
                </body>
                </html>
                """, academyName, reason);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("기관 거절 이메일 발송 성공 - academyName: {}", academyName);
        } catch (MessagingException e) {
            log.error("기관 거절 이메일 발송 실패 - academyName: {}, error: {}", academyName, e.getMessage());
            throw new EmailSendException("기관 거절 이메일 발송에 실패했습니다", e);
        }
    }
    
    /**
     * 회원 승인 완료 이메일 발송
     * 작성자: GitHub Copilot
     * 작성일: 2025-11-28
     */
    @Override
    public void sendAccountApprovalEmail(String toEmail, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(EmailConstants.SENDER_EMAIL, EmailConstants.SENDER_NAME);
            helper.setTo(toEmail);
            helper.setSubject("[코스타] 회원가입이 승인되었습니다");
            
            String htmlContent = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2>회원가입 승인 완료</h2>
                    <p>안녕하세요, <strong>%s</strong>님</p>
                    <p>코스타 소프트웨어 아카데미 회원가입이 승인되었습니다.</p>
                    <p>이제 로그인하여 서비스를 이용하실 수 있습니다.</p>
                    <p>감사합니다.</p>
                </body>
                </html>
                """, userName);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("회원 승인 이메일 발송 성공 - userName: {}", userName);
        } catch (MessagingException e) {
            log.error("회원 승인 이메일 발송 실패 - userName: {}, error: {}", userName, e.getMessage());
            throw new EmailSendException("회원 승인 이메일 발송에 실패했습니다", e);
        }
    }
}
