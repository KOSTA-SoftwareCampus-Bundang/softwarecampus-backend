package com.softwarecampus.backend.dto.course;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QnaRequest {
    private String title;
    private String questionText;
}