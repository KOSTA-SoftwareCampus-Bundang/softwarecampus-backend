package com.softwarecampus.backend.dto.course;

import java.time.LocalDateTime;

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
                LocalDateTime updatedAt) {
}