package com.softwarecampus.backend.dto.academy.qna;

import lombok.*;

import java.util.List;

/**
 * 훈련기관 질문 등록
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QACreateRequest {
    private String title;
    private String questionText;
    private Long academyId;

    private List<QAFileDetail> fileDetails;
}
