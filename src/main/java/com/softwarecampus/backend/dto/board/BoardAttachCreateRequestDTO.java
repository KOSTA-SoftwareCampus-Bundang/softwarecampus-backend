package com.softwarecampus.backend.dto.board;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardAttachCreateRequestDTO {

    private Long id;

    private Long boardId;

    private String originalFilename;

    private String realFilename;

}
