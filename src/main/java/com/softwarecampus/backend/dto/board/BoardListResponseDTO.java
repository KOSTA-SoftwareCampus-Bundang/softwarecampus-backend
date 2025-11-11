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

    private boolean isSecret;

    private String userNickName;

    private boolean isLike;

    private String createdAt;

}
