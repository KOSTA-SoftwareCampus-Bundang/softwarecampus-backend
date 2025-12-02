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
                LocalDateTime createdAt,      // 질문 작성일
                LocalDateTime updatedAt,      // 질문 수정일
                LocalDateTime answeredAt,     // 답변 작성일 (2025-12-03 추가)
                /** 첨부파일 목록 */
                List<QnaFileDetail> files) {
    
    /**
     * 파일 목록 없이 생성하는 팩토리 메서드 (하위 호환성)
     */
    public static QnaResponse withoutFiles(
            Long id, String title, String questionText, String answerText,
            Long accountId, String writerName, Long answeredById, String answeredByName,
            boolean isAnswered, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime answeredAt) {
        return new QnaResponse(id, title, questionText, answerText,
                accountId, writerName, answeredById, answeredByName,
                isAnswered, createdAt, updatedAt, answeredAt, List.of());
    }
}