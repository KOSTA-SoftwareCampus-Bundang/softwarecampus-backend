package com.softwarecampus.backend.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

/**
 * 리뷰 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCreateRequest {

    @NotBlank
    private String title;

    @NotEmpty
    @Valid
    private List<ReviewSectionRequest> sections;
}
