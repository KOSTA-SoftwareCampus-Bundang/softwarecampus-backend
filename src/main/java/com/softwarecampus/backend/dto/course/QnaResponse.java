package com.softwarecampus.backend.dto.course;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Course Q&A 응답 DTO
 */
public record QnaResponse(
                Long id,
                String title,
                String questionText,
                String answerText,
                Long accountId,
                String writerName,
                Long answeredById,
                String answeredByName,
                boolean isAnswered,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,
                /** 첨부파일 목록 */
                List<QnaFileDetail> files) {
    
    /**
     * 파일 목록 없이 생성하는 팩토리 메서드 (하위 호환성)
     */
    public static QnaResponse withoutFiles(
            Long id, String title, String questionText, String answerText,
            Long accountId, String writerName, Long answeredById, String answeredByName,
            boolean isAnswered, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new QnaResponse(id, title, questionText, answerText,
                accountId, writerName, answeredById, answeredByName,
                isAnswered, createdAt, updatedAt, List.of());
    }
}