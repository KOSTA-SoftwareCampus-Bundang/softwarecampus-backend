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

    private Long accountId;
    private String writerName;

    private Long answeredById;
    private String answeredByName;
    private boolean isAnswered;

    public static QAResponse from(AcademyQA qa) {

        if (qa.getAcademy() == null) {
            throw new IllegalStateException("Academy relationship is required for QA");
        }

        var account = qa.getAccount();
        var answeredBy = qa.getAnsweredBy();

        return QAResponse.builder()
                .id(qa.getId())
                .title(qa.getTitle())
                .questionText(qa.getQuestionText())
                .answerText(qa.getAnswerText())
                .createdAt(qa.getCreatedAt())
                .updatedAt(qa.getUpdatedAt())
                .academyId(qa.getAcademy().getId())
                .accountId(account != null ? account.getId() : null)
                .writerName(account != null ? account.getUserName() : "익명")
                .answeredById(answeredBy != null ? answeredBy.getId() : null)
                .answeredByName(answeredBy != null ? answeredBy.getUserName() : null)
                .isAnswered(qa.getAnswerText() != null && !qa.getAnswerText().isEmpty())
                .build();
    }
}
