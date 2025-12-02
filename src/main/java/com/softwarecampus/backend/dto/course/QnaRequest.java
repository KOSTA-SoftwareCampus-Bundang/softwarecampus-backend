package com.softwarecampus.backend.dto.course;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Course Q&A 질문 생성/수정 요청 DTO
 */
@Getter
@Setter
public class QnaRequest {
    /** 질문 제목 */
    private String title;
    /** 질문 내용 */
    private String questionText;
    
    /** 새로 첨부할 파일 목록 (파일 업로드 후 받은 정보) */
    private List<QnaFileDetail> fileDetails;
    /** 삭제할 첨부파일 ID 목록 (수정 시 사용) */
    private List<Long> deletedFileIds;
}