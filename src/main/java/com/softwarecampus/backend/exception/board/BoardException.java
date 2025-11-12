package com.softwarecampus.backend.exception.board;

import lombok.Getter;

@Getter
public class BoardException extends RuntimeException{

    private final BoardErrorCode errorCode;
    public BoardException(BoardErrorCode errorCode) {
        this.errorCode = errorCode;
    }

}
