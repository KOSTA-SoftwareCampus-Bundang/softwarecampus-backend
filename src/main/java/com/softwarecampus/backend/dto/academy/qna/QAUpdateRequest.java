package com.softwarecampus.backend.dto.academy.qna;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 훈련기관 질문 수정
 */

@Getter
@Setter
@NoArgsConstructor
public class QAUpdateRequest {
    private String title;
    private String questionText;
    private String answerText;
}
