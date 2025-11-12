package com.softwarecampus.backend.dto.board;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardResponseDTO {

    private Long id;

    private String category;

    private String title;

    private String text;

    private boolean isSecret;

    private int hits;

    private int likes;

    private String createdAt;

    private String userNickName;

    private boolean like;

    @Builder.Default
    private List<BoardAttachResponseDTO> boardAttachs = new ArrayList<>();

    @Builder.Default
    private List<CommentResponseDTO> boardComments = new ArrayList<>();


}
