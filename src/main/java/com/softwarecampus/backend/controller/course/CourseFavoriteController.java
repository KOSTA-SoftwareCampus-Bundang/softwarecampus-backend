package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.dto.course.CourseFavoriteResponseDTO;
import com.softwarecampus.backend.service.course.CourseFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{type}/course/favorite")
public class CourseFavoriteController {

    private final CourseFavoriteService favoriteService;

    /** 찜하기 토글 */
    @PostMapping("/{courseId}")
    public ResponseEntity<CourseFavoriteResponseDTO> toggleFavorite(@RequestParam Long accountId,
                                                                    @PathVariable("type") String type,
                                                                    @PathVariable Long courseId) {
        return ResponseEntity.ok(favoriteService.toggleFavorite(accountId, courseId));
    }

    /** 찜 목록 조회 (선택사항) */
    @GetMapping
    public ResponseEntity<List<CourseFavoriteResponseDTO>> getFavorites(@RequestParam Long accountId,
                                                                        @PathVariable("type") String type) {
        return ResponseEntity.ok(favoriteService.getFavorites(accountId));
    }

    /** 찜 여부 확인 (선택사항) */
    @GetMapping("/{courseId}/exists")
    public ResponseEntity<Boolean> isFavorite(@RequestParam Long accountId,
                                              @PathVariable("type") String type,
                                              @PathVariable Long courseId) {
        return ResponseEntity.ok(favoriteService.isFavorite(accountId, courseId));
    }
}
