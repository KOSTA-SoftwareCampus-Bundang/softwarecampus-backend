package com.softwarecampus.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 마이페이지 - 내가 쓴 댓글 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyCommentResponseDTO {

    private Long id;
    private String text;
    private Long boardId;
    private String boardTitle;
    private LocalDateTime createdAt;
}
