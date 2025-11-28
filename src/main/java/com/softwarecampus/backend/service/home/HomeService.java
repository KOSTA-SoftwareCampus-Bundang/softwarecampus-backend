package com.softwarecampus.backend.service.home;

import com.softwarecampus.backend.dto.home.HomeResponseDTO;

/**
 * 메인페이지 전용 서비스 인터페이스
 * 기존 CourseService와 독립적으로 동작
 */
public interface HomeService {

    /**
     * 메인페이지 데이터 조회
     * - 재직자 베스트 과정
     * - 취업예정자 베스트 과정
     * - 마감 임박 과정
     * 
     * @return 메인페이지 전체 데이터
     */
    HomeResponseDTO getHomePageData();

    /**
     * 커뮤니티 하이라이트 조회
     * - 최신 게시글 n개
     */
    java.util.List<com.softwarecampus.backend.dto.home.HomeCommunityDTO> getCommunityHighlights(int limit);
}
