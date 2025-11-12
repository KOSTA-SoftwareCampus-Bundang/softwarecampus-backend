package com.softwarecampus.backend.dto.academy.qna;

import com.softwarecampus.backend.domain.academy.qna.AcademyQA;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 훈련기관 질문 상세 조회
 */

@Getter
@Builder
public class QAResponse {
    private Long id;
    private String title;
    private String questionText;
    private String answerText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long academyId;

    public static QAResponse from(AcademyQA qa) {

        if (qa.getAcademy() == null) {
            throw new IllegalStateException("Academy relationship is required for QA");
        }

        return QAResponse.builder()
                .id(qa.getId())
                .title(qa.getTitle())
                .questionText(qa.getQuestionText())
                .answerText(qa.getAnswerText())
                .createdAt(qa.getCreatedAt())
                .updatedAt(qa.getUpdatedAt())
                .academyId(qa.getAcademy().getId())
                .build();
    }
}
