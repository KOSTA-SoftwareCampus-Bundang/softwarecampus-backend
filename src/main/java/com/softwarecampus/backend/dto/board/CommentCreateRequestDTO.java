package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.Comment;
import com.softwarecampus.backend.domain.user.Account;
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


    @NotNull
    private Long boardId;
    private Long topCommentId;

    @NotBlank
    private String text;

    public Comment toEntity(Board board, Account account) {
        return Comment.builder().board(board).account(account).text(text).build();
    }
}
