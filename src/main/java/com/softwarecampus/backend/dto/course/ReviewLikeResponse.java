package com.softwarecampus.backend.dto.course;

public record ReviewLikeResponse(
        String type,
        long likeCount,
        long dislikeCount
) {}