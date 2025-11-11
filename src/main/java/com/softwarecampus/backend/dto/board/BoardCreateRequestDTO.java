package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.BoardAttach;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardCreateRequestDTO {

    private Long id;

    private Long accountId;

    @Pattern(regexp = "NOTICE|QUESTION|COURSE_STORY|CODING_STORY", message = "게시판 카테고리는 NOTICE,QUESTION,COURSE_STORY,CODING_STORY 중 하나여야 합니다")
    private String category;

    @NotBlank
    private String title;

    @NotBlank
    private String text;

    @NotNull
    private boolean isSecret;

    @Builder.Default
    private List<BoardAttach> boardAttachs = new ArrayList<>();

}
