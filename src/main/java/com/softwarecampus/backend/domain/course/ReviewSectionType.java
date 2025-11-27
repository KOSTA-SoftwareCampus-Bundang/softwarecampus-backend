package com.softwarecampus.backend.domain.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum ReviewSectionType {
    CURRICULUM(1, "커리큘럼"),
    COURSEWARE(2, "교재/강의자료"),
    INSTRUCTOR(3, "강사"),
    EQUIPMENT(4, "실습장비"),
    OTHER(5, "기타의견");

    private final int code;
    private final String label;

    ReviewSectionType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonCreator
    public static ReviewSectionType from(String value) {
        // enum 이름으로 들어오는 경우 처리
        for (ReviewSectionType t : values()) {
            if (t.name().equalsIgnoreCase(value)) return t;
        }
        // label 로 들어오는 경우 처리 (교육내용 → CONTENT)
        for (ReviewSectionType t : values()) {
            if (t.label.equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Invalid ReviewSectionType: " + value);
    }
}

