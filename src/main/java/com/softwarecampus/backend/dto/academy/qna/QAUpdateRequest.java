package com.softwarecampus.backend.dto.academy.qna;

import lombok.*;

import java.util.List;

/**
 * 훈련기관 질문 수정
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAUpdateRequest {
    private String title;
    private String questionText;
    private String answerText;

    // 새로 추가할 파일 목록
    private List<QAFileDetail> newFileDetails;
    // 삭제할 기준 파일 Id 목록
    private List<Long> deletedFileIds;
}
