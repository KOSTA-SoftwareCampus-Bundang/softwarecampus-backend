package com.softwarecampus.backend.dto.academy.qna;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 훈련기관 질문 등록
 */

@Getter
@Setter
@NoArgsConstructor
public class QACreateRequest {
    private String title;
    private String questionText;
    private Long academyId;
}
