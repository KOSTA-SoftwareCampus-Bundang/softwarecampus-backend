package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.ReviewLike;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewLikeRequest {
    @NotNull(message = "좋아요/싫어요 타입은 필수입니다")
    private ReviewLike.LikeType type;
}