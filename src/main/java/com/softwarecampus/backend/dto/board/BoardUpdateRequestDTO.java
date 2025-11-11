package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardAttach;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardUpdateRequestDTO {

    @NotNull
    private Long id;

    private Long accountId;

    private String title;

    private String text;

    private boolean isSecret;

    @Builder.Default
    private List<BoardAttach> boardAttachs = new ArrayList<>();

    public void updateEntity(Board board){
        if(this.title == null){
            board.setTitle(this.getTitle());
        }
        if(this.text == null){
            board.setText(this.getText());
        }
        board.setSecret(this.isSecret());

    }

}
