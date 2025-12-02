package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardCategory;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardUpdateRequestDTO {

    @NotNull
    private Long id;

    private String title;

    private String text;

    private boolean secret;

    private BoardCategory category;

    // 삭제할 첨부파일 ID 목록
    private List<Long> deleteAttachIds;

    public void updateEntity(Board board) {
        if (this.title != null) {
            board.setTitle(this.getTitle());
        }
        if (this.text != null) {
            board.setText(this.getText());
        }
        if (this.category != null) {
            board.setCategory(this.getCategory());
        }
        board.setSecret(this.isSecret());
    }

}
