package com.softwarecampus.backend.dto.board;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardListResponseDTO {

    private Long id;

    private String category;

    private String title;

    private boolean secret;

    private String userNickName;

    private boolean like;

    private String createdAt;

}
