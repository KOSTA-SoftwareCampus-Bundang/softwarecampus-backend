package com.softwarecampus.backend.controller.sample;

import com.softwarecampus.backend.dto.home.HomeCommunityDTO;
import com.softwarecampus.backend.dto.home.HomeResponseDTO;
import com.softwarecampus.backend.service.home.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 홈 화면 전용 컨트롤러
 * - 메인페이지에서 필요한 데이터만 최적화하여 제공
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private static final int COMMUNITY_HIGHLIGHTS_LIMIT = 6;

    private final HomeService homeService;

    /**
     * 메인페이지 데이터 조회
     * - 재직자 베스트 과정 (4개)
     * - 취업예정자 베스트 과정 (4개)
     * - 마감 임박 과정 (4개)
     */
    @GetMapping("/courses")
    public ResponseEntity<HomeResponseDTO> getHomeCourses() {
        HomeResponseDTO data = homeService.getHomePageData();
        return ResponseEntity.ok(data);
    }

    /**
     * 커뮤니티 하이라이트 조회
     */
import com.softwarecampus.backend.dto.home.HomeCommunityDTO;

// ... (existing imports)

    private static final int COMMUNITY_HIGHLIGHTS_LIMIT = 6;

    // ... (existing code)

    /**
     * 커뮤니티 하이라이트 조회
     */
    @GetMapping("/community")
    public ResponseEntity<List<HomeCommunityDTO>> getCommunityHighlights() {
        List<HomeCommunityDTO> data = homeService.getCommunityHighlights(COMMUNITY_HIGHLIGHTS_LIMIT);
        return ResponseEntity.ok(data);
    }

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
