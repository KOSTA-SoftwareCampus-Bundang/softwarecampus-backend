package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.dto.course.CourseFavoriteResponseDTO;
import java.util.List;

public interface CourseFavoriteService {

    /** 찜하기 추가 (idempotent) */
    void addFavorite(Long accountId, Long courseId);

    /** 찜하기 삭제 (idempotent) */
    void removeFavorite(Long accountId, Long courseId);

    /** 찜 목록 조회 */
    List<CourseFavoriteResponseDTO> getFavorites(Long accountId);

    /** 찜 여부 확인 */
    boolean isFavorite(Long accountId, Long courseId);
}
