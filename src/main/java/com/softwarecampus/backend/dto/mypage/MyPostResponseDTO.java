package com.softwarecampus.backend.dto.mypage;

import com.softwarecampus.backend.domain.board.BoardCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 마이페이지 - 내가 쓴 글 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPostResponseDTO {

    private Long id;
    private String title;
    private BoardCategory category;
    private Long hits;
    private Long commentsCount;
    private Long likeCount;
    private LocalDateTime createdAt;
}
