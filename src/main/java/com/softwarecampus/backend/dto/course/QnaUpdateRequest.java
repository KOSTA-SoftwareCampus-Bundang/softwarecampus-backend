package com.softwarecampus.backend.dto.course;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Course Q&A 질문 수정 요청 DTO
 * - 수정 시에는 변경된 필드만 전달 가능
 * - 필수 입력 검증은 서비스 레이어에서 수행
 */
@Getter
@Setter
public class QnaUpdateRequest {
    /** 질문 제목 (변경 시에만 전달) */
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    /** 질문 내용 (변경 시에만 전달) */
    @Size(max = 5000, message = "질문 내용은 5000자를 초과할 수 없습니다")
    private String questionText;

    /** 새로 첨부할 파일 목록 (파일 업로드 후 받은 정보) */
    @Size(max = 5, message = "첨부파일은 최대 5개까지 가능합니다")
    private List<QnaFileDetail> fileDetails;

    /** 삭제할 첨부파일 ID 목록 (수정 시 사용) */
    private List<Long> deletedFileIds;
}
