package com.softwarecampus.backend.dto.board;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardAttachResponseDTO {


    private Long id;

    private Long boardId;

    private String originalFilename;

    private String realFilename;

    private String createdAt;


}
