package com.softwarecampus.backend.exception.academy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AcademyErrorCode {

    ACADEMY_NOT_FOUND(HttpStatus.NOT_FOUND, 2004, "요청한 훈련기관을 찾을 수 없습니다."),

    QA_NOT_FOUND(HttpStatus.NOT_FOUND, 2005, "요청한 Q&A 게시글을 찾을 수 없습니다."),
    QA_MISMATCH_ACADEMY(HttpStatus.BAD_REQUEST, 2006, "질문 ID와 훈련기관 ID가 일치하지 않습니다."),
    QA_MISSING_ACADEMY_RELATION(HttpStatus.INTERNAL_SERVER_ERROR, 2007, "Q&A 게시글과 훈련기관의 연결 정보가 일치하지 않습니다."),

    ANSWER_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, 2008, "답변 내용을 입력해야 합니다."),
    ANSWER_NOT_EXIST(HttpStatus.BAD_REQUEST, 2009, "삭제할 답변이 존재하지 않습니다."),

    ATTACHMENT_NOT_BELONG_TO_QA(HttpStatus.BAD_REQUEST, 2010, "첨부파일이 Q&A에 속하지 않습니다.."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 2011, "사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
