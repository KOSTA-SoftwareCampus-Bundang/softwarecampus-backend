package com.softwarecampus.backend.domain.board;

public enum BoardCategory {
    //공지사항, 문의사항, 진로이야기, 코딩이야기
    NOTICE,QUESTION,COURSE_STORY,CODING_STORY//ENUM타입 사용시 비 ASCII 문자는 좋지 않다고 한다

    //string input과 enum간의 변환 위한 메서드
    public static BoardCategory from(String value) {
        for (BoardCategory c : values()) {
            if (c.name().equalsIgnoreCase(value)) return c;
        }
        throw new IllegalArgumentException("Unknown category: " + value);
    }

}
