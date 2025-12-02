package com.softwarecampus.backend.service.board;

import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.Comment;
import com.softwarecampus.backend.exception.board.BoardErrorCode;
import com.softwarecampus.backend.exception.board.BoardException;
import com.softwarecampus.backend.repository.board.BoardRepository;
import com.softwarecampus.backend.repository.board.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("boardAuthorizeService")
@RequiredArgsConstructor
public class BoardAuthorizeServiceImpl implements BoardAuthorizeService {

    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    @Override
    public boolean canManipulateBoard(Long boardId, Long userId) {
        log.debug("canManipulateBoard - userId={}, boardId={}", userId, boardId);
        // soft-delete-aware 메서드 사용: 삭제된 게시글과 삭제된 계정 제외
        Board board = boardRepository.findByIdWithDetails(boardId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        return board.getAccount().getId().equals(userId);
    }

    @Override
    public boolean canManipulateComment(Long commentId, Long userId) {
        log.debug("canManipulateComment - userId={}, commentId={}", userId, commentId);
        // soft-delete-aware 메서드 사용: 삭제된 댓글과 삭제된 계정 제외
        Comment comment = commentRepository.findByIdWithAccount(commentId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        return comment.getAccount().getId().equals(userId);
    }

}
