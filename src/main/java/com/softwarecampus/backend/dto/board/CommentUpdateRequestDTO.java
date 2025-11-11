package com.softwarecampus.backend.dto.board;


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

    private Long accountId;

    @NotBlank
    private String text;

}
