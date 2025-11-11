package com.softwarecampus.backend.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentCreateRequestDTO {

    private Long id;
    @NotNull
    private Long boardId;
    private Long topCommentId;
    private Long accountId;

    @NotBlank
    private String text;
}
