package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardCreateRequestDTO {


    private BoardCategory category;

    @NotBlank
    private String title;

    @NotBlank
    private String text;

    private boolean secret;

    public Board toEntity() {
        return Board.builder().category(this.category).title(this.title).text(this.text).secret(this.secret).build();
    }


}
