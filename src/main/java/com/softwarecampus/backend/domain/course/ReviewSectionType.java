package com.softwarecampus.backend.domain.course;

import lombok.Getter;

@Getter
public enum ReviewSectionType {

    CONTENT(1, "교육내용", true),
    COURSEWARE(2, "교재/교안", true),
    INSTRUCTOR(3, "강사", true),
    EQUIPMENT(4, "시설/장비", true),
    OTHER(5, "기타의견", false);

    private final int code;
    private final String label;
    private final boolean hasRating; // 평점 존재 여부

    ReviewSectionType(int code, String label, boolean hasRating) {
        this.code = code;
        this.label = label;
        this.hasRating = hasRating;
    }

    public static ReviewSectionType fromCode(int code) {
        for (ReviewSectionType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Invalid section code: " + code);
    }
}
