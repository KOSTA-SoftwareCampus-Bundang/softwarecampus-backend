package com.softwarecampus.backend.controller.course;

import com.softwarecampus.backend.dto.course.CourseFavoriteResponseDTO;
import com.softwarecampus.backend.security.CustomUserDetails;
import com.softwarecampus.backend.service.course.CourseFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseFavoriteController {

    private final CourseFavoriteService favoriteService;

    /** 찜하기 추가 (인증 필요) */
    @PostMapping("/{courseId}/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> addFavorite(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        favoriteService.addFavorite(userDetails.getId(), courseId);
        return ResponseEntity.noContent().build();
    }

    /** 찜하기 삭제 (인증 필요) */
    @DeleteMapping("/{courseId}/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        favoriteService.removeFavorite(userDetails.getId(), courseId);
        return ResponseEntity.noContent().build();
    }

    /** 찜 여부 확인 (인증 필요) */
    @GetMapping("/{courseId}/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CourseFavoriteResponseDTO> checkFavorite(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean isFavorite = favoriteService.isFavorite(userDetails.getId(), courseId);
        return ResponseEntity.ok(new CourseFavoriteResponseDTO(courseId, isFavorite));
    }

    /** 현재 사용자의 찜 목록 조회 (인증 필요) */
    @GetMapping("/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CourseFavoriteResponseDTO>> getFavorites(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(favoriteService.getFavorites(userDetails.getId()));
    }
}
