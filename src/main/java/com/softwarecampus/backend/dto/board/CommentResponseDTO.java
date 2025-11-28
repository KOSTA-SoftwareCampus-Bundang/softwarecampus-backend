package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.Comment;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentResponseDTO {

    private Long id;
    private Long accountId;
    private String userNickName;
    private String text;

    private String createdAt;

    @Builder.Default
    private List<CommentResponseDTO> subComments = new ArrayList<>();

    public static CommentResponseDTO from(Comment comment, Long userId) {
        CommentResponseDTO commentResponseDTO = CommentResponseDTO.builder().id(comment.getId()).accountId(comment.getAccount().getId()).userNickName(comment.getAccount().getUserName()).text(comment.getText()).
                createdAt(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).build();
        if (comment.isSecret() && (userId == null || !userId.equals(comment.getAccount().getId()))){
            commentResponseDTO.setUserNickName("익명");
            commentResponseDTO.setAccountId(0L);
            commentResponseDTO.setText("비밀글입니다");
        }
        if (!comment.getSubComments().isEmpty()) {
            commentResponseDTO.setSubComments(comment.getSubComments().stream().filter(comment1 -> comment1.isActive()).map((c) -> CommentResponseDTO.from(c, userId)).toList());
        }
        return commentResponseDTO;
    }
}
