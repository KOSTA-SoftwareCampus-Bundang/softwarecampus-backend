package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.BoardCategory;
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

    private BoardCategory category;

    private String title;

    private boolean secret;

    private String userNickName;

    private int commentsCount;

    private String createdAt;

}
