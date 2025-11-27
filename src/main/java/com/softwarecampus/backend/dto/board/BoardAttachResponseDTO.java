package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.BoardAttach;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public static BoardAttachResponseDTO from(BoardAttach boardAttach) {
        return BoardAttachResponseDTO.builder().id(boardAttach.getId()).boardId(boardAttach.getBoard().getId()).
                originalFilename(boardAttach.getOriginalFilename()).realFilename(boardAttach.getRealFilename()).
                createdAt(boardAttach.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).build();
    }

}
