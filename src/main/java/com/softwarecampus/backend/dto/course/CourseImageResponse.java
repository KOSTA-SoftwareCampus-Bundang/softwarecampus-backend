package com.softwarecampus.backend.dto.course;

import com.softwarecampus.backend.domain.course.CourseImage;
import com.softwarecampus.backend.domain.course.CourseImageType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseImageResponse {
    private Long imageId;
    private String imageUrl;
    private CourseImageType imageType;
    
    /** @deprecated imageType 사용 권장 */
    @Deprecated
    private boolean isThumbnail;

    public static CourseImageResponse from(CourseImage image) {
        return CourseImageResponse.builder()
                .imageId(image.getId())
                .imageUrl(image.getImageUrl())
                .imageType(image.getImageType())
                .isThumbnail(image.isThumbnail())
                .build();
    }
}
