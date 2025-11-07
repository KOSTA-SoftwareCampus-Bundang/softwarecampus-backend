package com.softwarecampus.backend.service.course;

import com.softwarecampus.backend.dto.course.CourseFavoriteResponseDTO;
import java.util.List;

public interface CourseFavoriteService {

    /** 찜하기 토글 (등록 or 해제) */
    CourseFavoriteResponseDTO toggleFavorite(Long accountId, Long courseId);

    /** 찜 목록 조회 */
    List<CourseFavoriteResponseDTO> getFavorites(Long accountId);

    /** 찜 여부 확인 */
    boolean isFavorite(Long accountId, Long courseId);
}
