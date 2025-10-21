package com.softwarecampus.backend.controller.sample;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 기본 홈 컨트롤러
 * API 서버 상태 확인용
 */
@RestController
public class HomeController {

    /**
     * 루트 경로 - API 서버 상태 확인
     */
    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Software Campus API Server");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 헬스체크 엔드포인트
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return response;
    }
}

