package com.softwarecampus.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 - 활동 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyStatsResponseDTO {

    private Long totalPosts; // 작성한 글 수
    private Long totalComments; // 작성한 댓글 수
    private Long totalBookmarks; // 찜한 강좌 수
    private Long totalViews; // 총 조회수 (내 글들의 hits 합계)
}
