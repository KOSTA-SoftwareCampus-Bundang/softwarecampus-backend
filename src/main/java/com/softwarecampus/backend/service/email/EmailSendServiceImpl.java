package com.softwarecampus.backend.service.email;

import com.softwarecampus.backend.common.constants.EmailConstants;
import com.softwarecampus.backend.exception.email.EmailSendException;
import com.softwarecampus.backend.model.enums.VerificationType;
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
            log.info("이메일 발송 성공 - to: {}, type: {}", to, type);
        } catch (MessagingException | IOException e) {
            log.error("이메일 발송 실패 - to: {}, type: {}, error: {}", to, type, e.getMessage());
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
}
