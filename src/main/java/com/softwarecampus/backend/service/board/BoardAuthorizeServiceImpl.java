package com.softwarecampus.backend.service.board;


import com.softwarecampus.backend.domain.board.Board;
import com.softwarecampus.backend.domain.board.Comment;
import com.softwarecampus.backend.exception.board.BoardErrorCode;
import com.softwarecampus.backend.exception.board.BoardException;
import com.softwarecampus.backend.repository.board.BoardRepository;
import com.softwarecampus.backend.repository.board.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("boardAuthorizeService")
@RequiredArgsConstructor
public class BoardAuthorizeServiceImpl implements BoardAuthorizeService{

    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    @Override
    public boolean canManipulateBoard(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId).orElseThrow(()->new BoardException(BoardErrorCode.BOARD_NOT_FOUND));
        return board.getAccount().getId().equals(userId);
    }

    @Override
    public boolean canManipulateComment(Long commentId, Long userId) {
        Comment comment =commentRepository.findById(commentId).orElseThrow(()->new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        return comment.getAccount().getId().equals(userId);
    }


}
