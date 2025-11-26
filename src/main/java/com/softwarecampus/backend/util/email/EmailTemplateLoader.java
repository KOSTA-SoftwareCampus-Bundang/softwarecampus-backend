package com.softwarecampus.backend.util.email;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 이메일 HTML 템플릿 로더
 * - resources/templates/email/ 디렉토리에서 HTML 파일 로드
 * - 템플릿 변수 치환 기능 제공
 */
@Component
public class EmailTemplateLoader {
    
    private static final String TEMPLATE_BASE_PATH = "templates/email/";
    
    /**
     * HTML 템플릿 파일 로드
     * 
     * @param templateName 템플릿 파일명 (예: "signup-verification.html")
     * @return HTML 문자열
     * @throws IOException 파일 읽기 실패 시
     */
    public String loadTemplate(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_BASE_PATH + templateName);
        byte[] data = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(data, StandardCharsets.UTF_8);
    }
    
    /**
     * 템플릿의 변수 치환
     * 
     * @param template HTML 템플릿 문자열
     * @param variables 치환할 변수 맵 (키: 변수명, 값: 치환할 값)
     * @return 변수가 치환된 HTML 문자열
     */
    public String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 템플릿 로드 및 변수 치환을 한 번에 수행
     * 
     * @param templateName 템플릿 파일명
     * @param variables 치환할 변수 맵
     * @return 변수가 치환된 HTML 문자열
     * @throws IOException 파일 읽기 실패 시
     */
    public String loadAndReplace(String templateName, Map<String, String> variables) throws IOException {
        String template = loadTemplate(templateName);
        return replaceVariables(template, variables);
    }
}
