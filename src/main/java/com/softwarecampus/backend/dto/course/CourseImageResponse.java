package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.CourseImage;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseImageResponse {
    private Long imageId;
    private String imageUrl;
    private boolean isThumbnail;

    public static CourseImageResponse from(CourseImage image) {
        return CourseImageResponse.builder()
                .imageId(image.getId())
                .imageUrl(image.getImageUrl())
                .isThumbnail(image.isThumbnail())
                .build();
    }
}
