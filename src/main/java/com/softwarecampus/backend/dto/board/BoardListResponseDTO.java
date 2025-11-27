package com.softwarecampus.backend.dto.board;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.softwarecampus.backend.domain.board.BoardCategory;
import lombok.*;

import java.time.LocalDateTime;
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

    private Boolean secret;

    private String userNickName;

    private Long accountId;

    private Long commentsCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

}
