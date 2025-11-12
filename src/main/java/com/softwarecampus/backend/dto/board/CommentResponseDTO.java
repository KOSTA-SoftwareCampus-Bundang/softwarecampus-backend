package com.softwarecampus.backend.dto.board;

import lombok.*;

import java.time.LocalDateTime;
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

}
