package com.softwarecampus.backend.dto.course;

import lombok.Builder;
import lombok.Data;

/**
 * Course Q&A 첨부파일 상세 정보 DTO
 */
@Data
@Builder
public class QnaFileDetail {
    /** 첨부파일 ID */
    private Long id;
    /** 원본 파일명 */
    private String originName;
    /** S3 파일 URL */
    private String fileUrl;
}
