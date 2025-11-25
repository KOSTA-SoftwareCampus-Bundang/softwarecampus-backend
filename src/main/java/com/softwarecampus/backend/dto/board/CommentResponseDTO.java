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
    private String userNickName;
    private String text;

    private String createdAt;

    @Builder.Default
    private List<CommentResponseDTO> comments = new ArrayList<>();

    public static CommentResponseDTO from(Comment comment) {
        CommentResponseDTO commentResponseDTO =  CommentResponseDTO.builder().id(comment.getId()).userNickName(comment.getAccount().getUserName()).text(comment.getText()).
                createdAt(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).build();
        if(!comment.getSubComments().isEmpty()){
            commentResponseDTO.setComments(comment.getSubComments().stream().map(CommentResponseDTO::from).toList());
        }
        return commentResponseDTO;
    }
}
