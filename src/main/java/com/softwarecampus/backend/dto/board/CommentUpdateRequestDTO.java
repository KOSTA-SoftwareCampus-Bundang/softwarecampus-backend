package com.softwarecampus.backend.dto.board;


import com.softwarecampus.backend.domain.board.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentUpdateRequestDTO {

    @NotNull
    private Long id;

    @NotBlank
    private String text;

    public void updateEntity(Comment comment) {
        comment.setText(this.text);
    }

}
