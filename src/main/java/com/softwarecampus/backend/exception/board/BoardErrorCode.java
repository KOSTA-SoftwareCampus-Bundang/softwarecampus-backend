package com.softwarecampus.backend.exception.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BoardErrorCode {

    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, 1001, "해당 게시글을 찾을 수 없습니다"),
    CANNOT_READ_BOARD(HttpStatus.FORBIDDEN, 1002, "게시글에 접근 권한이 없습니다"),
    CANNOT_MODIFY_BOARD(HttpStatus.FORBIDDEN, 1003, "게시글 수정 권한이 없습니다"),
    CANNOT_DELETE_BOARD(HttpStatus.FORBIDDEN, 1004, "게시글 삭제 권힌이 없습니다"),
    ALREADY_RECOMMEND_BOARD(HttpStatus.BAD_REQUEST, 1005, "게시글을 이미 추천하였습니다"),
    NOT_RECOMMEND_BOARD(HttpStatus.BAD_REQUEST, 1006, "게시글을 추천한 기록이 없습니다"),
    SEARCHTYPE_MISSMATCH(HttpStatus.BAD_REQUEST, 1007, "검색 타입이 잘못됐습니다"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 1101, "해당 댓글을 찾지 못했습니다"),
    CANNOT_MODIFY_COMMENT(HttpStatus.FORBIDDEN, 1102, "댓글 수정 권한이 없습니다"),
    CANNOT_DELETE_COMMENT(HttpStatus.FORBIDDEN, 1103, "댓글 삭제 권한이 없습니다"),
    ALREADY_RECOMMEND_COMMENT(HttpStatus.BAD_REQUEST, 1104, "댓글을 이미 추천하였습니다"),
    NOT_RECOMMEND_COMMENT(HttpStatus.BAD_REQUEST, 1105, "댓글을 추천한 기록이 없습니다"),
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 2001, "게시판 첨부파일 업로드시 에러가 발생하였습니다"),
    FILE_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 2002, "게시판 첨부파일 삭제시 에러가 발생하였습니다"),
    FILE_DOWNLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 2003, "게시판 첨부파일 다운로드시 에러가 발생하였습니다"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, 2004, "게시판 첨부파일이 존재하지 않습니다"),
    FILE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, 2005, "게시판 첨부파일에 접근 권한이 없습니다"),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, 2006, "첨부파일 개수가 제한을 초과하였습니다");

    private final HttpStatus httpStatus;
    private final int errorCode;
    private final String details;
}
