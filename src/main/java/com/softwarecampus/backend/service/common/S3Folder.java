package com.softwarecampus.backend.service.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * S3 폴더 타입 정의
 * 허용된 S3 폴더 경로를 타입 안전하게 관리합니다.
 *
 * 사용 예시:
 * 
 * <pre>
 * // 다른 서비스에서 타입 안전하게 사용
 * s3Service.uploadFile(file, S3Folder.BOARD.getPath());
 * s3Service.uploadFile(file, S3Folder.COURSE.getPath());
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum S3Folder {
    BOARD("board"),
    ACADEMY("academy"),
    COURSE("course"),
    PROFILE("profile"),
    TEMP("temp"),
    ROOT(""),
    REVIEW("review");

    private final String path;
}
