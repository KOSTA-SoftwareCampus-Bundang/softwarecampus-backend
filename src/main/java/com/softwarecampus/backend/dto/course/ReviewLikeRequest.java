package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.ReviewLike;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewLikeRequest {
    private ReviewLike.LikeType type;
}