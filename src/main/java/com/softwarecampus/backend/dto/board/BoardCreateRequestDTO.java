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

    @Pattern(regexp = "NOTICE|QUESTION|COURSE_STORY|CODING_STORY", message = "게시판 카테고리는 NOTICE,QUESTION,COURSE_STORY,CODING_STORY 중 하나여야 합니다")
    private String category;

    @NotBlank
    private String title;

    @NotBlank
    private String text;

    private boolean secret;

    public Board toEntity() {
        return Board.builder().category(BoardCategory.from(this.category)).title(this.title).text(this.text).secret(this.secret).build();
    }


}
