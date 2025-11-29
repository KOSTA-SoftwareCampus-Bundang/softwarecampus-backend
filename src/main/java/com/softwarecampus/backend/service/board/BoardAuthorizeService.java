package com.softwarecampus.backend.service.board;

public interface BoardAuthorizeService {

    boolean canManipulateBoard(Long boardId,Long userId);

    boolean canManipulateComment(Long commentId,Long userId);

}
