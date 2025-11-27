package com.softwarecampus.backend.dto.academy;

import com.softwarecampus.backend.domain.academy.Academy;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AcademyNameResponse {
    private Long id;
    private String name;

    public static AcademyNameResponse from(Academy academy) {
        return AcademyNameResponse.builder()
                .id(academy.getId())
                .name(academy.getName())
                .build();
    }
}
