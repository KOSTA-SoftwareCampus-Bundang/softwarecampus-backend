package com.softwarecampus.backend.controller.sample;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 관리자 전용 컨트롤러
 * ADMIN 권한이 있는 사용자만 접근 가능
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /**
     * 관리자 대시보드
     * ADMIN 권한 필요
     */
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "관리자 대시보드에 접근했습니다.");
        response.put("role", "ADMIN");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 시스템 정보 조회
     * ADMIN 권한 필요
     */
    @GetMapping("/system")
    public Map<String, Object> systemInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("javaVersion", System.getProperty("java.version"));
        response.put("osName", System.getProperty("os.name"));
        response.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        response.put("freeMemory", Runtime.getRuntime().freeMemory());
        response.put("totalMemory", Runtime.getRuntime().totalMemory());
        return response;
    }
}

