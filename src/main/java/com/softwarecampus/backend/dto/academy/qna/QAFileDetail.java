package com.softwarecampus.backend.dto.academy.qna;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QAFileDetail {
    private Long id;
    private String originName;
    private String filename;
}
