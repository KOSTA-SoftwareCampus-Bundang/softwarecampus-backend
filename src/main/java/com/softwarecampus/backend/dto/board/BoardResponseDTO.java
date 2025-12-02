package com.softwarecampus.backend.dto.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.BoardCategory;
import com.softwarecampus.backend.domain.board.BoardRecommend;
import lombok.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardResponseDTO {

    private Long id;

    private BoardCategory category;

    private String title;

    private String text;

    private boolean secret;

    private long hits;

    // >필드명만 likes 에서 likeCount로변경
    private long likeCount;

    private String createdAt;

    private Long accountId;

    private String userNickName;

    private boolean like;

    private boolean owner;

    @Builder.Default
    private List<BoardAttachResponseDTO> boardAttachs = new ArrayList<>();

    @Builder.Default
    private List<CommentResponseDTO> boardComments = new ArrayList<>();

    public static BoardResponseDTO from(Board board, Long userId) {
        BoardResponseDTO boardResponseDTO = BoardResponseDTO.builder().id(board.getId()).category(board.getCategory())
                .title(board.getTitle())
                .text(board.getText()).secret(board.isSecret()).hits(board.getHits())
                .likeCount(board.getBoardRecommends().size())
                .createdAt(board.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .userNickName(board.getAccount().getUserName()).accountId(board.getAccount().getId()).build();
        boardResponseDTO.setBoardAttachs(board.getBoardAttaches().stream().map(BoardAttachResponseDTO::from).toList());
        // topComment가 null인 원댓글만 반환 (대댓글은 subComments로 포함됨)
        boardResponseDTO.setBoardComments(board.getComments().stream()
                .filter(comment -> comment.isActive() && comment.getTopComment() == null)
                .map((c) -> CommentResponseDTO.from(c, userId)).toList());

        return boardResponseDTO;
    }

}
