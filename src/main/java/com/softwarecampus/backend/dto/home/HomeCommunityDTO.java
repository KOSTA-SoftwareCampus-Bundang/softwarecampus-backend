package com.softwarecampus.backend.dto.home;

import com.softwarecampus.backend.domain.board.Board;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 메인페이지 - 커뮤니티 하이라이트 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeCommunityDTO {

    private Long id;
    private String title;
    private String category; // NOTICE, QUESTION, etc.
    private String categoryName; // 공지사항, 문의사항 등
    private String writerName;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;

    public static HomeCommunityDTO fromEntity(Board board) {
        return HomeCommunityDTO.builder()
                .id(board.getId())
                .title(board.getTitle())
                .category(board.getCategory().name())
                .categoryName(getCategoryName(board.getCategory().name()))
                .writerName(board.getAccount() != null ? board.getAccount().getUserName() : "익명")
                .viewCount((int) board.getHits())
                .likeCount(board.getBoardRecommends().size())
                .commentCount(board.getComments().size())
                .createdAt(board.getCreatedAt())
                .build();
    }

    private static String getCategoryName(String category) {
        switch (category) {
            case "NOTICE":
                return "공지사항";
            case "QUESTION":
                return "문의사항";
            case "COURSE_STORY":
                return "진로이야기";
            case "CODING_STORY":
                return "코딩이야기";
            default:
                return category;
        }
    }
}
