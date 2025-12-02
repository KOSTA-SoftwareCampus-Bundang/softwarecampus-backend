package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardCreateRequestDTO {

    @NotNull
    private BoardCategory category;

    @NotBlank
    private String title;

    @NotBlank
    private String text;

    private boolean secret;

    /**
     * 이미 S3에 업로드된 파일의 URL 목록 (에디터에서 업로드한 이미지)
     * 중복 업로드를 방지하기 위해 사용
     */
    private List<String> uploadedFileUrls;

    public Board toEntity() {
        return Board.builder().category(this.category).title(this.title).text(this.text).secret(this.secret).build();
    }

}
