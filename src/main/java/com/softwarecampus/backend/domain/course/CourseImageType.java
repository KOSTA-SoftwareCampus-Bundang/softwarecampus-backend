package com.softwarecampus.backend.domain.course;

/**
 * 과정 이미지 타입
 */
public enum CourseImageType {
    /** 썸네일 이미지 - 과정 목록에 표시 */
    THUMBNAIL,
    
    /** 헤더 이미지 - 과정 상세 페이지 배경 */
    HEADER,
    
    /** 콘텐츠 이미지 - 과정 설명 본문 내 이미지 */
    CONTENT
}
